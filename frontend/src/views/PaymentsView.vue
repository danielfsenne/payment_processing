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
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'

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

const inputClass =
  'w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 shadow-sm transition focus:border-indigo-500 focus:outline-none focus:ring-4 focus:ring-indigo-500/15'
const labelClass = 'mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500'
</script>

<template>
  <div>
    <div class="flex items-center gap-3">
      <h1 class="text-2xl font-bold text-slate-900">Pagamentos</h1>
      <span v-if="live" class="flex items-center gap-1.5 rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700">
        <span class="relative flex h-1.5 w-1.5">
          <span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-75"></span>
          <span class="relative inline-flex h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
        </span>
        tempo real conectado
      </span>
    </div>
    <p class="mt-1 text-sm text-slate-500">Crie e acompanhe pagamentos em tempo real.</p>

    <form class="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm" @submit.prevent="handleCreate">
      <div class="flex items-center gap-2">
        <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-violet-50 text-violet-600">
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
        </span>
        <h2 class="font-semibold text-slate-800">Novo pagamento</h2>
      </div>
      <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-4">
        <div v-if="auth.isAdmin">
          <label :class="labelClass">Customer ID</label>
          <input v-model="customerId" required :class="inputClass" />
        </div>
        <div>
          <label :class="labelClass">Conta</label>
          <select v-model="accountId" required :class="inputClass">
            <option v-for="a in accounts" :key="a.id" :value="a.id">{{ a.id.slice(0, 8) }}... ({{ a.availableBalance.toFixed(2) }} {{ a.currency }})</option>
          </select>
        </div>
        <div>
          <label :class="labelClass">Valor</label>
          <input v-model.number="amount" type="number" step="0.01" min="0.01" required :class="inputClass" />
        </div>
        <div>
          <label :class="labelClass">Moeda</label>
          <input v-model="currency" required :class="inputClass" />
        </div>
      </div>
      <p v-if="error" class="mt-3 rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ error }}</p>
      <button
        type="submit"
        :disabled="submitting || !accounts.length"
        class="mt-4 rounded-lg bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-2 text-sm font-semibold text-white shadow-sm shadow-indigo-500/30 transition hover:brightness-110 disabled:opacity-60"
      >
        {{ submitting ? 'Criando...' : 'Criar pagamento' }}
      </button>
      <p v-if="!accounts.length" class="mt-2 text-xs text-slate-400">Crie uma conta antes de criar um pagamento.</p>
    </form>

    <div class="mt-8">
      <LoadingState v-if="loading" />
      <EmptyState v-else-if="!payments.length" message="Nenhum pagamento encontrado." />
      <div v-else class="space-y-2.5">
        <div
          v-for="p in payments"
          :key="p.id"
          class="flex cursor-pointer items-center justify-between rounded-xl border border-slate-200 bg-white px-4 py-3.5 shadow-sm transition hover:border-indigo-200 hover:shadow-md"
          @click="$router.push(`/payments/${p.id}`)"
        >
          <div class="flex items-center gap-3">
            <span class="font-semibold text-slate-900">{{ p.amount.toFixed(2) }} {{ p.currency }}</span>
            <span class="font-mono text-xs text-slate-400">{{ p.id.slice(0, 8) }}...</span>
            <StatusBadge :status="p.status" />
          </div>
          <div class="flex items-center gap-3">
            <span class="text-xs text-slate-400">{{ new Date(p.updatedAt).toLocaleString() }}</span>
            <button
              v-if="p.status === 'CREATED'"
              :disabled="processingId === p.id"
              class="rounded-lg bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-slate-800 disabled:opacity-50"
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
