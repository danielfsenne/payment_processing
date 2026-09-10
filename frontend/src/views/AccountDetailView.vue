<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getAccount } from '@/api/accounts'
import { listPayments } from '@/api/payments'
import { useAuthStore } from '@/stores/auth'
import type { Account, Payment } from '@/types'
import StatusBadge from '@/components/StatusBadge.vue'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()
const auth = useAuthStore()

const account = ref<Account | null>(null)
const payments = ref<Payment[]>([])
const loading = ref(true)

const accountPayments = computed(() =>
  payments.value.filter((p) => p.accountId === (route.params.id as string)),
)

onMounted(async () => {
  const id = route.params.id as string
  const [a, p] = await Promise.all([getAccount(id), listPayments()])
  account.value = a
  payments.value = p
  loading.value = false
})
</script>

<template>
  <div>
    <RouterLink to="/accounts" class="inline-flex items-center gap-1 text-sm font-medium text-slate-500 hover:text-indigo-600">
      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
      </svg>
      Contas
    </RouterLink>

    <LoadingState v-if="loading" />
    <EmptyState v-else-if="!account" message="Conta não encontrada." />

    <div v-else class="mt-4">
      <div class="rounded-2xl bg-gradient-to-br from-slate-800 to-slate-900 p-6 text-white shadow-lg shadow-slate-300/50 sm:p-8">
        <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Disponível</p>
        <h1 class="mt-1 text-3xl font-bold">
          {{ account.availableBalance.toFixed(2) }} <span class="text-lg font-medium text-slate-300">{{ account.currency }}</span>
        </h1>
        <p class="mt-2 font-mono text-xs text-slate-400">{{ account.id }}</p>
      </div>

      <div class="mt-6 grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-3">
        <div v-if="auth.isAdmin">
          <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Customer ID</p>
          <p class="mt-1 font-mono text-sm text-slate-800">{{ account.customerId }}</p>
        </div>
        <div>
          <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Saldo</p>
          <p class="mt-1 text-sm text-slate-800">{{ account.balance.toFixed(2) }} {{ account.currency }}</p>
        </div>
        <div>
          <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Reservado</p>
          <p class="mt-1 text-sm text-slate-800">{{ account.reservedAmount.toFixed(2) }} {{ account.currency }}</p>
        </div>
        <div>
          <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Criada em</p>
          <p class="mt-1 text-sm text-slate-800">{{ new Date(account.createdAt).toLocaleString() }}</p>
        </div>
      </div>

      <div class="mt-8">
        <h2 class="mb-3 text-sm font-semibold text-slate-700">Pagamentos desta conta</h2>
        <EmptyState v-if="!accountPayments.length" message="Nenhum pagamento encontrado." />
        <div v-else class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <table class="w-full text-sm">
            <thead class="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
              <tr>
                <th class="px-4 py-3">Valor</th>
                <th class="px-4 py-3">Status</th>
                <th class="px-4 py-3">Atualizado em</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr
                v-for="p in accountPayments"
                :key="p.id"
                class="cursor-pointer transition hover:bg-slate-50"
                @click="$router.push(`/payments/${p.id}`)"
              >
                <td class="px-4 py-3 font-medium text-slate-800">{{ p.amount.toFixed(2) }} {{ p.currency }}</td>
                <td class="px-4 py-3"><StatusBadge :status="p.status" /></td>
                <td class="px-4 py-3 text-slate-500">{{ new Date(p.updatedAt).toLocaleString() }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
