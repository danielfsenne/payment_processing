<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listCustomers } from '@/api/customers'
import type { Customer } from '@/types'

const customers = ref<Customer[]>([])
const loading = ref(true)

onMounted(async () => {
  customers.value = await listCustomers()
  loading.value = false
})
</script>

<template>
  <div>
    <h1 class="text-2xl font-semibold text-gray-800">Clientes</h1>

    <div class="mt-6">
      <div v-if="loading" class="text-sm text-gray-500">Carregando...</div>
      <div v-else-if="!customers.length" class="text-sm text-gray-500">Nenhum cliente encontrado.</div>
      <div v-else class="overflow-hidden rounded-lg border border-gray-200 bg-white">
        <table class="w-full text-sm">
          <thead class="bg-gray-50 text-left text-gray-500">
            <tr>
              <th class="px-4 py-2 font-medium">Nome</th>
              <th class="px-4 py-2 font-medium">E-mail</th>
              <th class="px-4 py-2 font-medium">Documento</th>
              <th class="px-4 py-2 font-medium">Perfil</th>
              <th class="px-4 py-2 font-medium">Criado em</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="c in customers"
              :key="c.id"
              class="cursor-pointer border-t border-gray-100 hover:bg-gray-50"
              @click="$router.push(`/customers/${c.id}`)"
            >
              <td class="px-4 py-2 text-blue-600 hover:underline">{{ c.name }}</td>
              <td class="px-4 py-2">{{ c.email }}</td>
              <td class="px-4 py-2">{{ c.document }}</td>
              <td class="px-4 py-2">
                <span
                  class="rounded-full px-2 py-0.5 text-xs font-medium"
                  :class="c.role === 'ADMIN' ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'"
                >
                  {{ c.role }}
                </span>
              </td>
              <td class="px-4 py-2 text-gray-500">{{ new Date(c.createdAt).toLocaleString() }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
