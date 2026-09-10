<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listAccounts } from '@/api/accounts'
import { listPayments } from '@/api/payments'
import { useAuthStore } from '@/stores/auth'
import type { Account, Payment } from '@/types'
import StatusBadge from '@/components/StatusBadge.vue'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'

const auth = useAuthStore()
const accounts = ref<Account[]>([])
const payments = ref<Payment[]>([])
const loading = ref(true)

const totalAvailable = computed(() =>
  accounts.value.reduce((sum, a) => sum + a.availableBalance, 0),
)
const currency = computed(() => accounts.value[0]?.currency ?? 'BRL')
const settledCount = computed(() => payments.value.filter((p) => p.status === 'SETTLED').length)

onMounted(async () => {
  const [a, p] = await Promise.all([listAccounts(), listPayments()])
  accounts.value = a
  payments.value = p
  loading.value = false
})
</script>

<template>
  <div>
    <div class="overflow-hidden rounded-2xl bg-gradient-to-br from-indigo-600 via-indigo-600 to-violet-600 p-6 text-white shadow-lg shadow-indigo-500/25 sm:p-8">
      <p class="text-sm font-medium text-indigo-100">Bem-vindo de volta</p>
      <h1 class="mt-1 text-2xl font-bold sm:text-3xl">{{ auth.email }}</h1>
      <p class="mt-2 max-w-xl text-sm text-indigo-100">
        {{ auth.isAdmin ? 'Visão geral de todas as contas e pagamentos da plataforma.' : 'Aqui está o resumo das suas contas e pagamentos.' }}
      </p>
    </div>

    <LoadingState v-if="loading" label="Carregando painel..." />

    <template v-else>
      <div class="mt-6 grid grid-cols-1 gap-5 sm:grid-cols-3">
        <div class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75">
                <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 8.25h19.5M2.25 8.25v10.5A2.25 2.25 0 0 0 4.5 21h15a2.25 2.25 0 0 0 2.25-2.25V8.25M2.25 8.25V6A2.25 2.25 0 0 1 4.5 3.75h15A2.25 2.25 0 0 1 21.75 6v2.25M6 16.5h.008v.008H6V16.5Zm3 0h6" />
              </svg>
            </span>
            <RouterLink to="/accounts" class="text-xs font-medium text-indigo-600 hover:text-indigo-700">ver todas &rarr;</RouterLink>
          </div>
          <p class="mt-4 text-xs font-medium uppercase tracking-wide text-slate-400">Saldo disponível</p>
          <p class="mt-1 text-2xl font-bold text-slate-900">{{ totalAvailable.toFixed(2) }} <span class="text-sm font-medium text-slate-400">{{ currency }}</span></p>
        </div>

        <div class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-violet-50 text-violet-600">
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75">
                <path stroke-linecap="round" stroke-linejoin="round" d="M2.25 6h19.5M2.25 6v12a2.25 2.25 0 0 0 2.25 2.25h15A2.25 2.25 0 0 0 21.75 18V6M2.25 6l9-3 9 3M12 12.75a2.25 2.25 0 1 0 0-4.5 2.25 2.25 0 0 0 0 4.5Z" />
              </svg>
            </span>
            <RouterLink to="/payments" class="text-xs font-medium text-indigo-600 hover:text-indigo-700">ver todos &rarr;</RouterLink>
          </div>
          <p class="mt-4 text-xs font-medium uppercase tracking-wide text-slate-400">Pagamentos</p>
          <p class="mt-1 text-2xl font-bold text-slate-900">{{ payments.length }}</p>
        </div>

        <div class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
          <div class="flex items-center justify-between">
            <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-emerald-50 text-emerald-600">
              <svg class="h-5 w-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75">
                <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
              </svg>
            </span>
          </div>
          <p class="mt-4 text-xs font-medium uppercase tracking-wide text-slate-400">Liquidados</p>
          <p class="mt-1 text-2xl font-bold text-slate-900">{{ settledCount }}</p>
        </div>
      </div>

      <div class="mt-8">
        <h2 class="mb-3 text-sm font-semibold text-slate-700">Pagamentos recentes</h2>
        <EmptyState v-if="!payments.length" message="Nenhum pagamento por aqui ainda." />
        <div v-else class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <table class="w-full text-sm">
            <thead class="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
              <tr>
                <th class="px-4 py-3">Valor</th>
                <th class="px-4 py-3">Status</th>
                <th class="px-4 py-3">Criado em</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="p in payments.slice(0, 5)" :key="p.id" class="transition hover:bg-slate-50">
                <td class="px-4 py-3 font-medium text-slate-800">{{ p.amount.toFixed(2) }} {{ p.currency }}</td>
                <td class="px-4 py-3"><StatusBadge :status="p.status" /></td>
                <td class="px-4 py-3 text-slate-500">{{ new Date(p.createdAt).toLocaleString() }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>
