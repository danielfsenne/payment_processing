<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getCustomer } from '@/api/customers'
import { listAccounts } from '@/api/accounts'
import type { Account, Customer } from '@/types'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'

const route = useRoute()

const customer = ref<Customer | null>(null)
const accounts = ref<Account[]>([])
const loading = ref(true)

const customerAccounts = computed(() =>
  accounts.value.filter((a) => a.customerId === (route.params.id as string)),
)

onMounted(async () => {
  const id = route.params.id as string
  const [c, a] = await Promise.all([getCustomer(id), listAccounts()])
  customer.value = c
  accounts.value = a
  loading.value = false
})
</script>

<template>
  <div>
    <RouterLink to="/customers" class="inline-flex items-center gap-1 text-sm font-medium text-slate-500 hover:text-indigo-600">
      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
      </svg>
      Clientes
    </RouterLink>

    <LoadingState v-if="loading" />
    <EmptyState v-else-if="!customer" message="Cliente não encontrado." />

    <div v-else class="mt-4">
      <div class="rounded-2xl bg-gradient-to-br from-slate-800 to-slate-900 p-6 text-white shadow-lg shadow-slate-300/50 sm:p-8">
        <div class="flex flex-wrap items-center gap-3">
          <span class="flex h-12 w-12 items-center justify-center rounded-full bg-white/10 text-lg font-semibold">
            {{ customer.name.slice(0, 2).toUpperCase() }}
          </span>
          <div>
            <div class="flex items-center gap-2">
              <h1 class="text-2xl font-bold">{{ customer.name }}</h1>
              <span
                class="rounded-full px-2.5 py-1 text-xs font-semibold"
                :class="customer.role === 'ADMIN' ? 'bg-violet-500/20 text-violet-200' : 'bg-indigo-500/20 text-indigo-200'"
              >
                {{ customer.role === 'ADMIN' ? 'Admin' : 'Cliente' }}
              </span>
            </div>
            <p class="mt-1 font-mono text-xs text-slate-400">{{ customer.id }}</p>
          </div>
        </div>
      </div>

      <div class="mt-6 grid grid-cols-1 gap-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-3">
        <div>
          <p class="text-xs font-medium uppercase tracking-wide text-slate-400">E-mail</p>
          <p class="mt-1 text-sm text-slate-800">{{ customer.email }}</p>
        </div>
        <div>
          <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Documento</p>
          <p class="mt-1 text-sm text-slate-800">{{ customer.document }}</p>
        </div>
        <div>
          <p class="text-xs font-medium uppercase tracking-wide text-slate-400">Criado em</p>
          <p class="mt-1 text-sm text-slate-800">{{ new Date(customer.createdAt).toLocaleString() }}</p>
        </div>
      </div>

      <div class="mt-8">
        <h2 class="mb-3 text-sm font-semibold text-slate-700">Contas deste cliente</h2>
        <EmptyState v-if="!customerAccounts.length" message="Nenhuma conta encontrada." />
        <div v-else class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
          <table class="w-full text-sm">
            <thead class="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
              <tr>
                <th class="px-4 py-3">Saldo</th>
                <th class="px-4 py-3">Disponível</th>
                <th class="px-4 py-3">Moeda</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr
                v-for="a in customerAccounts"
                :key="a.id"
                class="cursor-pointer transition hover:bg-slate-50"
                @click="$router.push(`/accounts/${a.id}`)"
              >
                <td class="px-4 py-3 text-slate-600">{{ a.balance.toFixed(2) }}</td>
                <td class="px-4 py-3 font-semibold text-slate-900">{{ a.availableBalance.toFixed(2) }}</td>
                <td class="px-4 py-3 text-slate-500">{{ a.currency }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
