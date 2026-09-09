<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getCustomer } from '@/api/customers'
import { listAccounts } from '@/api/accounts'
import type { Account, Customer } from '@/types'

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
    <RouterLink to="/customers" class="text-sm text-blue-600 hover:underline">&larr; voltar aos clientes</RouterLink>

    <div v-if="loading" class="mt-4 text-sm text-gray-500">Carregando...</div>

    <div v-else-if="!customer" class="mt-4 text-sm text-gray-500">Cliente não encontrado.</div>

    <div v-else class="mt-4">
      <div class="flex items-center gap-3">
        <h1 class="text-2xl font-semibold text-gray-800">{{ customer.name }}</h1>
        <span
          class="rounded-full px-2 py-0.5 text-xs font-medium"
          :class="customer.role === 'ADMIN' ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'"
        >
          {{ customer.role }}
        </span>
      </div>
      <p class="mt-1 font-mono text-xs text-gray-400">{{ customer.id }}</p>

      <div class="mt-6 grid grid-cols-1 gap-4 rounded-lg border border-gray-200 bg-white p-5 sm:grid-cols-3">
        <div>
          <p class="text-xs font-medium text-gray-500">E-mail</p>
          <p class="text-sm text-gray-800">{{ customer.email }}</p>
        </div>
        <div>
          <p class="text-xs font-medium text-gray-500">Documento</p>
          <p class="text-sm text-gray-800">{{ customer.document }}</p>
        </div>
        <div>
          <p class="text-xs font-medium text-gray-500">Criado em</p>
          <p class="text-sm text-gray-800">{{ new Date(customer.createdAt).toLocaleString() }}</p>
        </div>
      </div>

      <div class="mt-8">
        <h2 class="mb-3 font-medium text-gray-700">Contas deste cliente</h2>
        <div v-if="!customerAccounts.length" class="text-sm text-gray-500">Nenhuma conta encontrada.</div>
        <div v-else class="overflow-hidden rounded-lg border border-gray-200 bg-white">
          <table class="w-full text-sm">
            <thead class="bg-gray-50 text-left text-gray-500">
              <tr>
                <th class="px-4 py-2 font-medium">Saldo</th>
                <th class="px-4 py-2 font-medium">Disponível</th>
                <th class="px-4 py-2 font-medium">Moeda</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="a in customerAccounts"
                :key="a.id"
                class="cursor-pointer border-t border-gray-100 hover:bg-gray-50"
                @click="$router.push(`/accounts/${a.id}`)"
              >
                <td class="px-4 py-2">{{ a.balance.toFixed(2) }}</td>
                <td class="px-4 py-2 font-medium">{{ a.availableBalance.toFixed(2) }}</td>
                <td class="px-4 py-2">{{ a.currency }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
