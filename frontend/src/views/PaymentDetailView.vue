<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import type { Client } from '@stomp/stompjs'
import { getPayment, getPaymentEvents, processPayment } from '@/api/payments'
import { connectPaymentSocket } from '@/ws/paymentSocket'
import { useAuthStore } from '@/stores/auth'
import { HttpError } from '@/api/client'
import type { Payment, PaymentEvent } from '@/types'
import StatusBadge from '@/components/StatusBadge.vue'

const route = useRoute()
const auth = useAuthStore()

const payment = ref<Payment | null>(null)
const events = ref<PaymentEvent[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const processing = ref(false)
const live = ref(false)

let socket: Client | null = null

async function refresh() {
  loading.value = true
  const id = route.params.id as string
  const [p, e] = await Promise.all([getPayment(id), getPaymentEvents(id)])
  payment.value = p
  events.value = e
  loading.value = false
}

onMounted(async () => {
  await refresh()
  if (auth.customerId && auth.token) {
    socket = connectPaymentSocket(auth.customerId, auth.token, (event) => {
      live.value = true
      if (payment.value && event.paymentId === payment.value.id) {
        payment.value.status = event.toStatus
        payment.value.updatedAt = event.occurredAt
        getPaymentEvents(payment.value.id).then((e) => (events.value = e))
      }
    })
  }
})

onUnmounted(() => {
  socket?.deactivate()
})

async function handleProcess() {
  if (!payment.value) return
  error.value = null
  processing.value = true
  try {
    payment.value = await processPayment(payment.value.id, false)
    events.value = await getPaymentEvents(payment.value.id)
  } catch (err) {
    error.value = err instanceof HttpError ? err.message : 'Falha ao processar pagamento'
  } finally {
    processing.value = false
  }
}
</script>

<template>
  <div>
    <RouterLink to="/payments" class="text-sm text-blue-600 hover:underline">&larr; voltar aos pagamentos</RouterLink>

    <div v-if="loading" class="mt-4 text-sm text-gray-500">Carregando...</div>

    <div v-else-if="!payment" class="mt-4 text-sm text-gray-500">Pagamento não encontrado.</div>

    <div v-else class="mt-4">
      <div class="flex items-center gap-3">
        <h1 class="text-2xl font-semibold text-gray-800">{{ payment.amount.toFixed(2) }} {{ payment.currency }}</h1>
        <StatusBadge :status="payment.status" />
        <span v-if="live" class="flex items-center gap-1 text-xs text-green-600">
          <span class="h-2 w-2 rounded-full bg-green-500"></span> tempo real conectado
        </span>
      </div>
      <p class="mt-1 font-mono text-xs text-gray-400">{{ payment.id }}</p>

      <div class="mt-6 grid grid-cols-1 gap-4 rounded-lg border border-gray-200 bg-white p-5 sm:grid-cols-2">
        <div>
          <p class="text-xs font-medium text-gray-500">Customer ID</p>
          <p class="font-mono text-sm text-gray-800">{{ payment.customerId }}</p>
        </div>
        <div>
          <p class="text-xs font-medium text-gray-500">Account ID</p>
          <RouterLink :to="`/accounts/${payment.accountId}`" class="font-mono text-sm text-blue-600 hover:underline">
            {{ payment.accountId }}
          </RouterLink>
        </div>
        <div>
          <p class="text-xs font-medium text-gray-500">Reservation ID</p>
          <p class="font-mono text-sm text-gray-800">{{ payment.reservationId ?? '-' }}</p>
        </div>
        <div>
          <p class="text-xs font-medium text-gray-500">Atualizado em</p>
          <p class="text-sm text-gray-800">{{ new Date(payment.updatedAt).toLocaleString() }}</p>
        </div>
      </div>

      <p v-if="payment.failureReason" class="mt-4 text-sm text-red-600">
        Motivo da falha: {{ payment.failureReason }}
      </p>

      <p v-if="error" class="mt-4 text-sm text-red-600">{{ error }}</p>
      <button
        v-if="payment.status === 'CREATED'"
        :disabled="processing"
        class="mt-4 rounded-md bg-gray-800 px-4 py-2 text-sm font-medium text-white hover:bg-gray-900 disabled:opacity-50"
        @click="handleProcess"
      >
        {{ processing ? 'Processando...' : 'Processar' }}
      </button>

      <div class="mt-8">
        <h2 class="mb-3 font-medium text-gray-700">Linha do tempo</h2>
        <ul class="space-y-2 rounded-lg border border-gray-200 bg-white p-4 text-sm text-gray-700">
          <li v-for="e in events" :key="e.id" class="flex items-center gap-2">
            <span class="text-xs text-gray-400">{{ new Date(e.createdAt).toLocaleString() }}</span>
            <span>{{ e.eventType }}</span>
            <span v-if="e.fromStatus" class="text-xs text-gray-400">{{ e.fromStatus }} &rarr;</span>
            <StatusBadge :status="e.toStatus" />
          </li>
          <li v-if="!events.length" class="text-sm text-gray-500">Nenhum evento registrado.</li>
        </ul>
      </div>
    </div>
  </div>
</template>
