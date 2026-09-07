<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <nav v-if="auth.isAuthenticated" class="border-b border-gray-200 bg-white">
      <div class="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <div class="flex items-center gap-6">
          <span class="font-semibold text-gray-800">Payment Processing</span>
          <RouterLink to="/" class="text-sm text-gray-600 hover:text-gray-900" active-class="text-gray-900 font-medium">Dashboard</RouterLink>
          <RouterLink to="/accounts" class="text-sm text-gray-600 hover:text-gray-900" active-class="text-gray-900 font-medium">Contas</RouterLink>
          <RouterLink to="/payments" class="text-sm text-gray-600 hover:text-gray-900" active-class="text-gray-900 font-medium">Pagamentos</RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/customers" class="text-sm text-gray-600 hover:text-gray-900" active-class="text-gray-900 font-medium">Clientes</RouterLink>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-sm text-gray-500">{{ auth.email }}</span>
          <span
            class="rounded-full px-2 py-0.5 text-xs font-medium"
            :class="auth.isAdmin ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'"
          >
            {{ auth.isAdmin ? 'ADMIN' : 'CUSTOMER' }}
          </span>
          <button class="text-sm text-gray-500 hover:text-gray-800" @click="handleLogout">Sair</button>
        </div>
      </div>
    </nav>
    <main class="mx-auto max-w-5xl px-4 py-8">
      <RouterView />
    </main>
  </div>
</template>
