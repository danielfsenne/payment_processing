import { Client } from '@stomp/stompjs'
import type { PaymentTopicEvent } from '@/types'

/**
 * One STOMP connection per subscribed customer topic. The token travels as a query
 * param (not a STOMP/HTTP header) because that's the only thing a browser WebSocket
 * handshake can carry - see JwtHandshakeInterceptor on notification-worker.
 */
export function connectPaymentSocket(
  customerId: string,
  token: string,
  onEvent: (event: PaymentTopicEvent) => void,
): Client {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  const client = new Client({
    brokerURL: `${protocol}://${window.location.host}/ws?token=${encodeURIComponent(token)}`,
    reconnectDelay: 5000,
    onConnect: () => {
      client.subscribe(`/topic/payments/${customerId}`, (message) => {
        onEvent(JSON.parse(message.body) as PaymentTopicEvent)
      })
    },
  })
  client.activate()
  return client
}
