<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listCustomers } from '@/api/customers'
import type { Customer } from '@/types'
import LoadingState from '@/components/LoadingState.vue'
import EmptyState from '@/components/EmptyState.vue'

const customers = ref<Customer[]>([])
const loading = ref(true)

onMounted(async () => {
  customers.value = await listCustomers()
  loading.value = false
})
</script>

<template>
  <div>
    <h1 class="text-2xl font-bold text-slate-900">Clientes</h1>
    <p class="mt-1 text-sm text-slate-500">Todos os clientes cadastrados na plataforma.</p>

    <div class="mt-6">
      <LoadingState v-if="loading" />
      <EmptyState v-else-if="!customers.length" message="Nenhum cliente encontrado." />
      <div v-else class="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
        <table class="w-full text-sm">
          <thead class="bg-slate-50 text-left text-xs font-semibold uppercase tracking-wide text-slate-500">
            <tr>
              <th class="px-4 py-3">Nome</th>
              <th class="px-4 py-3">E-mail</th>
              <th class="px-4 py-3">Documento</th>
              <th class="px-4 py-3">Perfil</th>
              <th class="px-4 py-3">Criado em</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr
              v-for="c in customers"
              :key="c.id"
              class="cursor-pointer transition hover:bg-slate-50"
              @click="$router.push(`/customers/${c.id}`)"
            >
              <td class="px-4 py-3 font-medium text-indigo-600">{{ c.name }}</td>
              <td class="px-4 py-3 text-slate-600">{{ c.email }}</td>
              <td class="px-4 py-3 text-slate-600">{{ c.document }}</td>
              <td class="px-4 py-3">
                <span
                  class="rounded-full px-2.5 py-1 text-xs font-semibold"
                  :class="c.role === 'ADMIN' ? 'bg-violet-100 text-violet-700' : 'bg-indigo-100 text-indigo-700'"
                >
                  {{ c.role === 'ADMIN' ? 'Admin' : 'Cliente' }}
                </span>
              </td>
              <td class="px-4 py-3 text-slate-500">{{ new Date(c.createdAt).toLocaleString() }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
