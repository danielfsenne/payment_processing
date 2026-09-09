<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getAccount } from '@/api/accounts'
import { listPayments } from '@/api/payments'
import { useAuthStore } from '@/stores/auth'
import type { Account, Payment } from '@/types'
import StatusBadge from '@/components/StatusBadge.vue'

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
    <RouterLink to="/accounts" class="text-sm text-blue-600 hover:underline">&larr; voltar às contas</RouterLink>

    <div v-if="loading" class="mt-4 text-sm text-gray-500">Carregando...</div>

    <div v-else-if="!account" class="mt-4 text-sm text-gray-500">Conta não encontrada.</div>

    <div v-else class="mt-4">
      <h1 class="text-2xl font-semibold text-gray-800">
        {{ account.availableBalance.toFixed(2) }} {{ account.currency }} disponível
      </h1>
      <p class="mt-1 font-mono text-xs text-gray-400">{{ account.id }}</p>

      <div class="mt-6 grid grid-cols-1 gap-4 rounded-lg border border-gray-200 bg-white p-5 sm:grid-cols-3">
        <div v-if="auth.isAdmin">
          <p class="text-xs font-medium text-gray-500">Customer ID</p>
          <p class="font-mono text-sm text-gray-800">{{ account.customerId }}</p>
        </div>
        <div>
          <p class="text-xs font-medium text-gray-500">Saldo</p>
          <p class="text-sm text-gray-800">{{ account.balance.toFixed(2) }} {{ account.currency }}</p>
        </div>
        <div>
          <p class="text-xs font-medium text-gray-500">Reservado</p>
          <p class="text-sm text-gray-800">{{ account.reservedAmount.toFixed(2) }} {{ account.currency }}</p>
        </div>
        <div>
          <p class="text-xs font-medium text-gray-500">Criada em</p>
          <p class="text-sm text-gray-800">{{ new Date(account.createdAt).toLocaleString() }}</p>
        </div>
      </div>

      <div class="mt-8">
        <h2 class="mb-3 font-medium text-gray-700">Pagamentos desta conta</h2>
        <div v-if="!accountPayments.length" class="text-sm text-gray-500">Nenhum pagamento encontrado.</div>
        <div v-else class="overflow-hidden rounded-lg border border-gray-200 bg-white">
          <table class="w-full text-sm">
            <thead class="bg-gray-50 text-left text-gray-500">
              <tr>
                <th class="px-4 py-2 font-medium">Valor</th>
                <th class="px-4 py-2 font-medium">Status</th>
                <th class="px-4 py-2 font-medium">Atualizado em</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="p in accountPayments"
                :key="p.id"
                class="cursor-pointer border-t border-gray-100 hover:bg-gray-50"
                @click="$router.push(`/payments/${p.id}`)"
              >
                <td class="px-4 py-2">{{ p.amount.toFixed(2) }} {{ p.currency }}</td>
                <td class="px-4 py-2"><StatusBadge :status="p.status" /></td>
                <td class="px-4 py-2 text-gray-500">{{ new Date(p.updatedAt).toLocaleString() }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
