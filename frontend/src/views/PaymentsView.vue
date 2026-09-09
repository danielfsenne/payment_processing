<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import type { Client } from '@stomp/stompjs'
import { listPayments, createPayment, processPayment } from '@/api/payments'
import { listAccounts } from '@/api/accounts'
import { connectPaymentSocket } from '@/ws/paymentSocket'
import { useAuthStore } from '@/stores/auth'
import { HttpError } from '@/api/client'
import type { Account, Payment } from '@/types'
import StatusBadge from '@/components/StatusBadge.vue'

const auth = useAuthStore()
const payments = ref<Payment[]>([])
const accounts = ref<Account[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const live = ref(false)

const customerId = ref(auth.customerId ?? '')
const accountId = ref('')
const amount = ref(0)
const currency = ref('BRL')
const submitting = ref(false)

const processingId = ref<string | null>(null)

let socket: Client | null = null

async function refresh() {
  loading.value = true
  const [p, a] = await Promise.all([listPayments(), listAccounts()])
  payments.value = p
  accounts.value = a
  if (!accountId.value && a.length) accountId.value = a[0].id
  loading.value = false
}

function upsertPayment(updated: Payment) {
  const index = payments.value.findIndex((p) => p.id === updated.id)
  if (index >= 0) {
    payments.value[index] = updated
  } else {
    payments.value.unshift(updated)
  }
}

onMounted(async () => {
  await refresh()
  if (auth.customerId && auth.token) {
    socket = connectPaymentSocket(auth.customerId, auth.token, (event) => {
      live.value = true
      const existing = payments.value.find((p) => p.id === event.paymentId)
      if (existing) {
        existing.status = event.toStatus
        existing.updatedAt = event.occurredAt
      }
    })
  }
})

onUnmounted(() => {
  socket?.deactivate()
})

async function handleCreate() {
  error.value = null
  submitting.value = true
  try {
    const payment = await createPayment(
      { customerId: customerId.value, accountId: accountId.value, amount: amount.value, currency: currency.value },
      crypto.randomUUID(),
    )
    upsertPayment(payment)
    amount.value = 0
  } catch (err) {
    error.value = err instanceof HttpError ? err.message : 'Falha ao criar pagamento'
  } finally {
    submitting.value = false
  }
}

async function handleProcess(payment: Payment) {
  error.value = null
  processingId.value = payment.id
  try {
    const updated = await processPayment(payment.id, false)
    upsertPayment(updated)
  } catch (err) {
    error.value = err instanceof HttpError ? err.message : 'Falha ao processar pagamento'
  } finally {
    processingId.value = null
  }
}

</script>

<template>
  <div>
    <div class="flex items-center gap-3">
      <h1 class="text-2xl font-semibold text-gray-800">Pagamentos</h1>
      <span v-if="live" class="flex items-center gap-1 text-xs text-green-600">
        <span class="h-2 w-2 rounded-full bg-green-500"></span> tempo real conectado
      </span>
    </div>

    <form class="mt-6 rounded-lg border border-gray-200 bg-white p-5" @submit.prevent="handleCreate">
      <h2 class="mb-4 font-medium text-gray-700">Novo pagamento</h2>
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-4">
        <div v-if="auth.isAdmin">
          <label class="block text-sm font-medium text-gray-700">Customer ID</label>
          <input v-model="customerId" required class="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700">Conta</label>
          <select v-model="accountId" required class="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm">
            <option v-for="a in accounts" :key="a.id" :value="a.id">{{ a.id.slice(0, 8) }}... ({{ a.availableBalance.toFixed(2) }} {{ a.currency }})</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700">Valor</label>
          <input v-model.number="amount" type="number" step="0.01" min="0.01" required class="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700">Moeda</label>
          <input v-model="currency" required class="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
        </div>
      </div>
      <p v-if="error" class="mt-3 text-sm text-red-600">{{ error }}</p>
      <button
        type="submit"
        :disabled="submitting || !accounts.length"
        class="mt-4 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {{ submitting ? 'Criando...' : 'Criar pagamento' }}
      </button>
      <p v-if="!accounts.length" class="mt-2 text-xs text-gray-500">Crie uma conta antes de criar um pagamento.</p>
    </form>

    <div class="mt-8">
      <div v-if="loading" class="text-sm text-gray-500">Carregando...</div>
      <div v-else-if="!payments.length" class="text-sm text-gray-500">Nenhum pagamento encontrado.</div>
      <div v-else class="space-y-2">
        <div
          v-for="p in payments"
          :key="p.id"
          class="flex cursor-pointer items-center justify-between rounded-lg border border-gray-200 bg-white px-4 py-3 hover:bg-gray-50"
          @click="$router.push(`/payments/${p.id}`)"
        >
          <div class="flex items-center gap-3">
            <span class="font-medium text-gray-800">{{ p.amount.toFixed(2) }} {{ p.currency }}</span>
            <span class="font-mono text-xs text-gray-400">{{ p.id.slice(0, 8) }}...</span>
            <StatusBadge :status="p.status" />
          </div>
          <div class="flex items-center gap-2">
            <span class="text-xs text-gray-400">{{ new Date(p.updatedAt).toLocaleString() }}</span>
            <button
              v-if="p.status === 'CREATED'"
              :disabled="processingId === p.id"
              class="rounded-md bg-gray-800 px-3 py-1 text-xs font-medium text-white hover:bg-gray-900 disabled:opacity-50"
              @click.stop="handleProcess(p)"
            >
              {{ processingId === p.id ? 'Processando...' : 'Processar' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
