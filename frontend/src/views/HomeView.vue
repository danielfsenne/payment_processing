<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listAccounts } from '@/api/accounts'
import { listPayments } from '@/api/payments'
import { useAuthStore } from '@/stores/auth'
import type { Account, Payment } from '@/types'
import StatusBadge from '@/components/StatusBadge.vue'

const auth = useAuthStore()
const accounts = ref<Account[]>([])
const payments = ref<Payment[]>([])
const loading = ref(true)

onMounted(async () => {
  const [a, p] = await Promise.all([listAccounts(), listPayments()])
  accounts.value = a
  payments.value = p
  loading.value = false
})
</script>

<template>
  <div>
    <h1 class="text-2xl font-semibold text-gray-800">Olá, {{ auth.email }}</h1>
    <p class="mt-1 text-sm text-gray-500">
      {{ auth.isAdmin ? 'Visão geral de todas as contas e pagamentos.' : 'Visão geral das suas contas e pagamentos.' }}
    </p>

    <div v-if="loading" class="mt-8 text-sm text-gray-500">Carregando...</div>
    <div v-else class="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2">
      <div class="rounded-lg border border-gray-200 bg-white p-5">
        <div class="flex items-center justify-between">
          <h2 class="font-medium text-gray-700">Contas</h2>
          <RouterLink to="/accounts" class="text-sm text-blue-600 hover:underline">ver todas</RouterLink>
        </div>
        <p class="mt-2 text-3xl font-semibold text-gray-900">{{ accounts.length }}</p>
      </div>
      <div class="rounded-lg border border-gray-200 bg-white p-5">
        <div class="flex items-center justify-between">
          <h2 class="font-medium text-gray-700">Pagamentos</h2>
          <RouterLink to="/payments" class="text-sm text-blue-600 hover:underline">ver todos</RouterLink>
        </div>
        <p class="mt-2 text-3xl font-semibold text-gray-900">{{ payments.length }}</p>
      </div>
    </div>

    <div v-if="!loading && payments.length" class="mt-8">
      <h2 class="mb-3 font-medium text-gray-700">Pagamentos recentes</h2>
      <div class="overflow-hidden rounded-lg border border-gray-200 bg-white">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 text-left text-gray-500">
            <tr>
              <th class="px-4 py-2 font-medium">Valor</th>
              <th class="px-4 py-2 font-medium">Status</th>
              <th class="px-4 py-2 font-medium">Criado em</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in payments.slice(0, 5)" :key="p.id" class="border-t border-gray-100">
              <td class="px-4 py-2">{{ p.amount.toFixed(2) }} {{ p.currency }}</td>
              <td class="px-4 py-2"><StatusBadge :status="p.status" /></td>
              <td class="px-4 py-2 text-gray-500">{{ new Date(p.createdAt).toLocaleString() }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
