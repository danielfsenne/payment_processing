<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listAccounts, createAccount } from '@/api/accounts'
import { useAuthStore } from '@/stores/auth'
import { HttpError } from '@/api/client'
import type { Account } from '@/types'

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
</script>

<template>
  <div>
    <h1 class="text-2xl font-semibold text-gray-800">Contas</h1>

    <form class="mt-6 rounded-lg border border-gray-200 bg-white p-5" @submit.prevent="handleCreate">
      <h2 class="mb-4 font-medium text-gray-700">Nova conta</h2>
      <div class="grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div v-if="auth.isAdmin">
          <label class="block text-sm font-medium text-gray-700">Customer ID</label>
          <input v-model="customerId" required class="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700">Saldo inicial</label>
          <input v-model.number="initialBalance" type="number" step="0.01" min="0" class="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700">Moeda</label>
          <input v-model="currency" required class="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm" />
        </div>
      </div>
      <p v-if="error" class="mt-3 text-sm text-red-600">{{ error }}</p>
      <button
        type="submit"
        :disabled="submitting"
        class="mt-4 rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {{ submitting ? 'Criando...' : 'Criar conta' }}
      </button>
    </form>

    <div class="mt-8">
      <div v-if="loading" class="text-sm text-gray-500">Carregando...</div>
      <div v-else-if="!accounts.length" class="text-sm text-gray-500">Nenhuma conta encontrada.</div>
      <div v-else class="overflow-hidden rounded-lg border border-gray-200 bg-white">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 text-left text-gray-500">
            <tr>
              <th class="px-4 py-2 font-medium">ID</th>
              <th v-if="auth.isAdmin" class="px-4 py-2 font-medium">Customer</th>
              <th class="px-4 py-2 font-medium">Saldo</th>
              <th class="px-4 py-2 font-medium">Reservado</th>
              <th class="px-4 py-2 font-medium">Disponível</th>
              <th class="px-4 py-2 font-medium">Moeda</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in accounts" :key="a.id" class="border-t border-gray-100">
              <td class="px-4 py-2 font-mono text-xs text-gray-500">{{ a.id }}</td>
              <td v-if="auth.isAdmin" class="px-4 py-2 font-mono text-xs text-gray-500">{{ a.customerId }}</td>
              <td class="px-4 py-2">{{ a.balance.toFixed(2) }}</td>
              <td class="px-4 py-2">{{ a.reservedAmount.toFixed(2) }}</td>
              <td class="px-4 py-2 font-medium">{{ a.availableBalance.toFixed(2) }}</td>
              <td class="px-4 py-2">{{ a.currency }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
