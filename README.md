# Payment Processing

Plataforma de processamento de pagamentos construída como um sistema de microsserviços em Spring Boot, com um front-end em Vue 3. O objetivo do projeto é servir de estudo/portfólio para padrões de sistemas distribuídos usados em pagamentos: saga com compensação, transactional outbox, idempotência, locks distribuídos, circuit breaker e observabilidade completa (métricas, logs, traces).

## Arquitetura

```
                         ┌──────────────┐
                         │   frontend   │  (Vue 3 + Vite)
                         └──────┬───────┘
                                │ HTTP
                         ┌──────▼───────┐
                         │  api-gateway │  (Spring Cloud Gateway, rate limit, circuit breaker)
                         └──────┬───────┘
              ┌─────────────────┼─────────────────┐
              │                 │                  │
      ┌───────▼──────┐  ┌───────▼───────┐  ┌───────▼───────┐
      │customer-service│  │account-service│  │payment-service│
      │ (auth + JWT)  │  │(saldo/reserva)│  │ (saga/outbox) │
      └───────┬───────┘  └───────┬───────┘  └───────┬───────┘
              │                  │                   │
              │             Postgres/Redis      RabbitMQ (outbox)
              │                                       │
              │                              ┌────────┴────────┐
              │                        ┌──────▼─────┐    ┌──────▼───────────┐
              │                        │payment-worker│   │notification-worker│
              │                        └─────────────┘   │ (WebSocket/STOMP) │
              └─────────────────────────────────────────►└───────────────────┘
```

Cada serviço tem seu próprio banco Postgres (schemas/databases separados), e se comunicam de duas formas:
- **Síncrona**: REST via OpenFeign (payment-service → account-service/customer-service), protegido por circuit breaker (Resilience4j).
- **Assíncrona**: eventos de domínio publicados no RabbitMQ via **transactional outbox** (a escrita no banco e o registro do evento a publicar acontecem na mesma transação local; um poller separado entrega o evento ao broker).

## Serviços

| Serviço | Porta | Responsabilidade |
|---|---|---|
| `api-gateway` | 8080 | Roteamento, rate limiting (Redis), circuit breaker/timeout por rota |
| `customer-service` | 8083 | Cadastro de clientes, autenticação (login/refresh/logout), emissão de JWT |
| `account-service` | 8082 | Contas e saldo, reservas de saldo (hold/confirm/release) |
| `payment-service` | 8081 (exposta em 8091 no compose) | Orquestração da saga de pagamento, outbox, idempotência |
| `payment-worker` | 8084 | Consome eventos de pagamento do RabbitMQ (dead-letter queue) |
| `notification-worker` | 8085 | Repassa eventos de pagamento a clientes conectados via WebSocket/STOMP |
| `frontend` | 5173 (dev) | SPA Vue 3 para clientes e admins |

`common-security` é um módulo Maven à parte (não um serviço executável): concentra o que toda validação de JWT tem em comum entre `account-service`, `customer-service`, `payment-service` e `notification-worker` — `JwtDecoder`, o mapeamento da claim `roles`, a checagem de token revogado e o guard de segredos de produção — como uma auto-configuration do Spring Boot, para não duplicar essas classes serviço a serviço. Cada Dockerfile que depende dele builda a partir da raiz do repositório e instala esse módulo no repositório Maven local da imagem antes de compilar o serviço.

### Infraestrutura
Postgres, Redis, RabbitMQ, Prometheus, Grafana, OpenTelemetry Collector e Tempo — tudo já provisionado no `docker-compose.yml`.

## Fluxo de um pagamento (saga)

1. Cliente cria um pagamento (`POST /payments`) → status `CREATED`.
2. Cliente dispara o processamento (`POST /payments/{id}/process`) → `PaymentSagaOrchestrator`:
   - `CREATED → PROCESSING`
   - reserva o saldo no `account-service` (`POST /accounts/{id}/reservations`)
   - `PROCESSING → AUTHORIZED`
   - confirma a reserva (captura) → `AUTHORIZED → CAPTURED`
   - `CAPTURED → SETTLED`
3. Se a reserva falhar por saldo insuficiente, ou uma etapa de processamento falhar, a saga executa a compensação (libera a reserva) e marca o pagamento como `FAILED`.
4. Um lock distribuído (Redis) serializa chamadas concorrentes de `/process` para o mesmo pagamento.
5. Cada transição de estado grava um evento na tabela de **outbox** na mesma transação; um publisher assíncrono entrega esse evento ao RabbitMQ e o marca como `FAILED` (dead-letter) após esgotar as tentativas, em vez de retentar para sempre.
6. Um **job de reconciliação** roda periodicamente e retoma pagamentos que ficaram presos em um estado intermediário (ex.: por causa de uma falha de rede entre confirmar a reserva e persistir a transição local), reaproveitando a idempotência das chamadas ao `account-service` em vez de compensar às cegas.

## Segurança

- `customer-service` é o único emissor de tokens: autentica com email/senha e emite um **JWT HS256** (1h de validade) com a claim `roles`.
- Os demais serviços só validam o token com o segredo compartilhado (`JWT_SECRET`) — nunca emitem um.
- **Refresh token** opaco (armazenado no Redis, TTL de 7 dias, rotativo a cada uso) permite renovar o access token sem exigir novo login; o front-end faz isso automaticamente ao receber um `401`.
- **Logout** revoga o access token corrente (denylist no Redis, com TTL igual ao tempo restante do token) e invalida o refresh token associado.
- Autorização é distribuída: cada serviço aplica suas próprias regras (dono do recurso ou `ADMIN`) a partir das claims do JWT.

## Observabilidade

- **Métricas**: cada serviço expõe `/actuator/prometheus`; Prometheus com regras de alerta em `infra/prometheus/`.
- **Dashboards**: Grafana pré-provisionado (`infra/grafana/`), acessível em `http://localhost:3001` (admin/admin).
- **Tracing distribuído**: OpenTelemetry Collector → Tempo, com 100% de amostragem em desenvolvimento.
- **Health checks**: todos os serviços expõem `/actuator/health` e possuem healthcheck no Docker/compose, incluindo o estado dos circuit breakers.

## Como rodar

### Com Docker Compose (recomendado)

```bash
docker compose up --build
```

Isso sobe toda a stack: bancos, RabbitMQ, observabilidade, os seis serviços Spring Boot e o front-end.

| URL | O quê |
|---|---|
| http://localhost:5173 | Frontend |
| http://localhost:8080 | API Gateway |
| http://localhost:15672 | RabbitMQ management (guest/guest) |
| http://localhost:9090 | Prometheus |
| http://localhost:3001 | Grafana (admin/admin) |
| http://localhost:3200 | Tempo |

Um usuário `ADMIN` é criado automaticamente no primeiro start do `customer-service` (ver variáveis `ADMIN_*` abaixo).

### Localmente, serviço a serviço

Cada serviço Spring Boot é independente (Maven + wrapper):

```bash
cd services/<nome-do-servico>
./mvnw spring-boot:run
```

É preciso ter Postgres, Redis e RabbitMQ rodando localmente (ou usar `docker compose up postgres redis rabbitmq` para subir só a infraestrutura).

Para o frontend:

```bash
cd frontend
npm install
npm run dev
```

### Variáveis de ambiente principais

| Variável | Usada por | Padrão (dev) |
|---|---|---|
| `JWT_SECRET` | todos os serviços com auth | segredo de desenvolvimento embutido — **trocar em produção** |
| `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | serviços com Postgres | `localhost` / por serviço / `postgres` / `postgres` |
| `REDIS_HOST`, `REDIS_PORT` | serviços com cache/lock/token | `localhost` / `6379` |
| `RABBITMQ_HOST` | serviços com outbox/eventos | `localhost` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | `customer-service` (bootstrap do admin) | `admin@payment-processing.local` / `admin12345` |
| `OUTBOX_MAX_ATTEMPTS` | `payment-service` | `10` |
| `RECONCILIATION_STUCK_AFTER`, `RECONCILIATION_INTERVAL` | `payment-service` | `PT2M` / `PT1M` |
| `RATE_LIMIT_REPLENISH_RATE`, `RATE_LIMIT_BURST_CAPACITY` | `api-gateway` | `10` / `20` |

## Testes

```bash
# um serviço específico
cd services/payment-service && ./mvnw test

# frontend
cd frontend && npm test
```

Os testes de integração usam Testcontainers (Postgres/Redis reais) onde fizer sentido — não há mocks de banco em testes que validam contratos de dados ou autorização ponta a ponta.

## CI/CD

`.github/workflows/ci-cd.yml` roda, em cada push/PR para `main`:
- testes de cada serviço Java (matriz, com RabbitMQ como serviço do runner);
- build + testes do frontend;
- validação do `docker-compose.yml`;
- em push para `main`: build e publish das imagens Docker de cada serviço no GitHub Container Registry.

## Stack

- **Backend**: Java 17, Spring Boot 4, Spring Cloud Gateway (WebFlux), Spring Data JPA, Spring Security (Resource Server/JWT), OpenFeign, Resilience4j, Flyway, RabbitMQ, Redis.
- **Frontend**: Vue 3, Vite, Pinia, TypeScript, Tailwind CSS, Vitest.
- **Infra**: Docker Compose, Prometheus, Grafana, OpenTelemetry, Tempo.
