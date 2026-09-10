<script setup lang="ts">
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BrandMark from '@/components/BrandMark.vue'

const auth = useAuthStore()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}

const initials = () =>
  (auth.email ?? '')
    .split('@')[0]
    .slice(0, 2)
    .toUpperCase()

const navLinks = [
  { to: '/', label: 'Painel' },
  { to: '/accounts', label: 'Contas' },
  { to: '/payments', label: 'Pagamentos' },
]
</script>

<template>
  <div class="min-h-screen bg-slate-50">
    <nav v-if="auth.isAuthenticated" class="sticky top-0 z-10 border-b border-slate-200 bg-white/80 backdrop-blur">
      <div class="mx-auto flex max-w-6xl items-center justify-between px-4 py-3 sm:px-6">
        <div class="flex items-center gap-8">
          <BrandMark size="sm" />
          <div class="hidden items-center gap-1 sm:flex">
            <RouterLink
              v-for="link in navLinks"
              :key="link.to"
              :to="link.to"
              class="rounded-lg px-3 py-1.5 text-sm font-medium text-slate-500 transition hover:bg-slate-100 hover:text-slate-900"
              active-class="!bg-indigo-50 !text-indigo-700"
              :exact="link.to === '/'"
            >
              {{ link.label }}
            </RouterLink>
            <RouterLink
              v-if="auth.isAdmin"
              to="/customers"
              class="rounded-lg px-3 py-1.5 text-sm font-medium text-slate-500 transition hover:bg-slate-100 hover:text-slate-900"
              active-class="!bg-indigo-50 !text-indigo-700"
            >
              Clientes
            </RouterLink>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <span
            class="hidden rounded-full px-2.5 py-1 text-xs font-semibold sm:inline-flex"
            :class="auth.isAdmin ? 'bg-violet-100 text-violet-700' : 'bg-indigo-100 text-indigo-700'"
          >
            {{ auth.isAdmin ? 'Admin' : 'Cliente' }}
          </span>
          <div class="flex items-center gap-2 border-l border-slate-200 pl-3">
            <span class="flex h-8 w-8 items-center justify-center rounded-full bg-slate-800 text-xs font-semibold text-white">
              {{ initials() }}
            </span>
            <span class="hidden text-sm text-slate-600 md:inline">{{ auth.email }}</span>
            <button
              class="rounded-lg p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-700"
              title="Sair"
              @click="handleLogout"
            >
              <svg class="h-4 w-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0 0 13.5 3h-6a2.25 2.25 0 0 0-2.25 2.25v13.5A2.25 2.25 0 0 0 7.5 21h6a2.25 2.25 0 0 0 2.25-2.25V15M18 12H8.25m9.75 0-3-3m3 3-3 3" />
              </svg>
            </button>
          </div>
        </div>
      </div>
      <div class="flex items-center gap-1 overflow-x-auto border-t border-slate-100 px-4 py-1.5 sm:hidden">
        <RouterLink
          v-for="link in navLinks"
          :key="link.to"
          :to="link.to"
          class="shrink-0 rounded-lg px-3 py-1 text-sm font-medium text-slate-500"
          active-class="bg-indigo-50 text-indigo-700"
          :exact="link.to === '/'"
        >
          {{ link.label }}
        </RouterLink>
        <RouterLink
          v-if="auth.isAdmin"
          to="/customers"
          class="shrink-0 rounded-lg px-3 py-1 text-sm font-medium text-slate-500"
          active-class="bg-indigo-50 text-indigo-700"
        >
          Clientes
        </RouterLink>
      </div>
    </nav>
    <main class="mx-auto max-w-6xl px-4 py-8 sm:px-6">
      <RouterView />
    </main>
  </div>
</template>
