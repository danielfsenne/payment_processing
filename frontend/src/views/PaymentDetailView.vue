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
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'

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
    <RouterLink to="/payments" class="inline-flex items-center gap-1 text-sm font-medium text-slate-500 hover:text-indigo-600">
      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
      </svg>
      Pagamentos
    </RouterLink>

    <LoadingState v-if="loading" />
    <EmptyState v-else-if="!payment" message="Pagamento não encontrado." />

    <div v-else class="mt-4">
      <div class="rounded-2xl bg-gradient-to-br from-slate-800 to-slate-900 p-6 text-white shadow-lg shadow-slate-300/50 sm:p-8">
        <div class="flex flex-wrap items-center gap-3">
          <h1 class="text-3xl font-bold">{{ payment.amount.toFixed(2) }} {{ payment.currency }}</h1>
          <StatusBadge :status="payment.status" />
          <span v-if="live" class="flex items-center gap-1.5 rounded-full bg-emerald-500/15 px-2.5 py-1 text-xs font-medium text-emerald-300">
            <span class="relative flex h-1.5 w-1.5">
              <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75"></span>
              <span class="relative inline-flex h-1.5 w-1.5 rounded-full bg-emerald-400"></span>
            </span>
            tempo real conectado
          </span>
        </div>
        <p class="mt-2 font-mono text-xs text-slate-400">{{ payment.id }}</p>
      </div>

      <div class="mt-6 grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-2">
        <div>
          <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Customer ID</p>
          <p class="mt-1 font-mono text-sm text-slate-800">{{ payment.customerId }}</p>
        </div>
        <div>
          <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Account ID</p>
          <RouterLink :to="`/accounts/${payment.accountId}`" class="mt-1 block font-mono text-sm text-indigo-600 hover:underline">
            {{ payment.accountId }}
          </RouterLink>
        </div>
        <div>
          <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Reservation ID</p>
          <p class="mt-1 font-mono text-sm text-slate-800">{{ payment.reservationId ?? '-' }}</p>
        </div>
        <div>
          <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Atualizado em</p>
          <p class="mt-1 text-sm text-slate-800">{{ new Date(payment.updatedAt).toLocaleString() }}</p>
        </div>
      </div>

      <p v-if="payment.failureReason" class="mt-4 rounded-lg bg-rose-50 px-4 py-3 text-sm text-rose-700">
        <span class="font-semibold">Motivo da falha:</span> {{ payment.failureReason }}
      </p>

      <p v-if="error" class="mt-4 rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ error }}</p>
      <button
        v-if="payment.status === 'CREATED'"
        :disabled="processing"
        class="mt-4 rounded-lg bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-2 text-sm font-semibold text-white shadow-sm shadow-indigo-500/30 transition hover:brightness-110 disabled:opacity-60"
        @click="handleProcess"
      >
        {{ processing ? 'Processando...' : 'Processar' }}
      </button>

      <div class="mt-8">
        <h2 class="mb-3 text-sm font-semibold text-slate-700">Linha do tempo</h2>
        <EmptyState v-if="!events.length" message="Nenhum evento registrado." />
        <ul v-else class="relative space-y-5 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <li v-for="(e, index) in events" :key="e.id" class="relative flex gap-3 pl-1">
            <div class="flex flex-col items-center">
              <span class="mt-1 h-2.5 w-2.5 shrink-0 rounded-full bg-indigo-500 ring-4 ring-indigo-100"></span>
              <span v-if="index < events.length - 1" class="mt-1 w-px flex-1 bg-slate-200"></span>
            </div>
            <div class="flex flex-1 flex-wrap items-center gap-x-2 gap-y-1 pb-1 text-sm text-slate-700">
              <span class="font-medium text-slate-900">{{ e.eventType }}</span>
              <span v-if="e.fromStatus" class="text-xs text-slate-400">{{ e.fromStatus }} &rarr;</span>
              <StatusBadge :status="e.toStatus" />
              <span class="ml-auto text-xs text-slate-400">{{ new Date(e.createdAt).toLocaleString() }}</span>
            </div>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>
