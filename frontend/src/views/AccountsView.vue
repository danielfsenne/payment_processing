<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listAccounts, createAccount } from '@/api/accounts'
import { useAuthStore } from '@/stores/auth'
import { HttpError } from '@/api/client'
import type { Account } from '@/types'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'

const auth = useAuthStore()
const accounts = ref<Account[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const customerId = ref(auth.customerId ?? '')
const initialBalance = ref(0)
const currency = ref('BRL')
const submitting = ref(false)

async function refresh() {
  loading.value = true
  accounts.value = await listAccounts()
  loading.value = false
}

onMounted(refresh)

async function handleCreate() {
  error.value = null
  submitting.value = true
  try {
    await createAccount({
      customerId: customerId.value,
      initialBalance: initialBalance.value,
      currency: currency.value,
    })
    initialBalance.value = 0
    await refresh()
  } catch (err) {
    error.value = err instanceof HttpError ? err.message : 'Falha ao criar conta'
  } finally {
    submitting.value = false
  }
}

const inputClass =
  'w-full rounded-lg border border-slate-300 px-3 py-2 text-sm text-slate-900 shadow-sm transition focus:border-indigo-500 focus:outline-none focus:ring-4 focus:ring-indigo-500/15'
const labelClass = 'mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500'
</script>

<template>
  <div>
    <div class="flex items-center justify-between">
      <div>
        <h1 class="text-2xl font-bold text-slate-900">Contas</h1>
        <p class="mt-1 text-sm text-slate-500">Gerencie as contas e acompanhe os saldos.</p>
      </div>
    </div>

    <form class="mt-6 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm" @submit.prevent="handleCreate">
      <div class="flex items-center gap-2">
        <span class="flex h-8 w-8 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
          <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
          </svg>
        </span>
        <h2 class="font-semibold text-slate-800">Nova conta</h2>
      </div>
      <div class="mt-4 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div v-if="auth.isAdmin">
          <label :class="labelClass">Customer ID</label>
          <input v-model="customerId" required :class="inputClass" />
        </div>
        <div>
          <label :class="labelClass">Saldo inicial</label>
          <input v-model.number="initialBalance" type="number" step="0.01" min="0" :class="inputClass" />
        </div>
        <div>
          <label :class="labelClass">Moeda</label>
          <input v-model="currency" required :class="inputClass" />
        </div>
      </div>
      <p v-if="error" class="mt-3 rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ error }}</p>
      <button
        type="submit"
        :disabled="submitting"
        class="mt-4 rounded-lg bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-2 text-sm font-semibold text-white shadow-sm shadow-indigo-500/30 transition hover:brightness-110 disabled:opacity-60"
      >
        {{ submitting ? 'Criando...' : 'Criar conta' }}
      </button>
    </form>

    <div class="mt-8">
      <LoadingState v-if="loading" />
      <EmptyState v-else-if="!accounts.length" message="Nenhuma conta encontrada." />
      <div v-else class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table class="w-full text-sm">
          <thead class="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
            <tr>
              <th class="px-4 py-3">ID</th>
              <th v-if="auth.isAdmin" class="px-4 py-3">Customer</th>
              <th class="px-4 py-3">Saldo</th>
              <th class="px-4 py-3">Reservado</th>
              <th class="px-4 py-3">Disponível</th>
              <th class="px-4 py-3">Moeda</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr
              v-for="a in accounts"
              :key="a.id"
              class="cursor-pointer transition hover:bg-slate-50"
              @click="$router.push(`/accounts/${a.id}`)"
            >
              <td class="px-4 py-3 font-mono text-xs text-indigo-600">{{ a.id }}</td>
              <td v-if="auth.isAdmin" class="px-4 py-3 font-mono text-xs text-slate-500">{{ a.customerId }}</td>
              <td class="px-4 py-3 text-slate-600">{{ a.balance.toFixed(2) }}</td>
              <td class="px-4 py-3 text-slate-600">{{ a.reservedAmount.toFixed(2) }}</td>
              <td class="px-4 py-3 font-semibold text-slate-900">{{ a.availableBalance.toFixed(2) }}</td>
              <td class="px-4 py-3 text-slate-500">{{ a.currency }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
