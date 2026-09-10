<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { HttpError } from '@/api/client'
import BrandMark from '@/components/BrandMark.vue'

const email = ref('')
const password = ref('')
const error = ref<string | null>(null)
const loading = ref(false)

const auth = useAuthStore()
const router = useRouter()

async function handleSubmit() {
  error.value = null
  loading.value = true
  try {
    const response = await login({ email: email.value, password: password.value })
    auth.setToken(response.accessToken)
    router.push('/')
  } catch (err) {
    error.value = err instanceof HttpError ? err.message : 'Falha ao conectar com o servidor'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="relative flex min-h-[calc(100vh-2rem)] items-center justify-center overflow-hidden">
    <div class="pointer-events-none absolute -top-32 left-1/2 h-80 w-[36rem] -translate-x-1/2 rounded-full bg-gradient-to-r from-indigo-200 to-violet-200 opacity-50 blur-3xl"></div>

    <div class="relative w-full max-w-sm">
      <div class="mb-8 flex justify-center">
        <BrandMark size="lg" />
      </div>
      <div class="rounded-2xl border border-slate-200 bg-white p-8 shadow-xl shadow-slate-200/60">
        <h1 class="text-lg font-semibold text-slate-900">Entrar na sua conta</h1>
        <p class="mt-1 text-sm text-slate-500">Acesse suas contas e pagamentos.</p>

        <form class="mt-6 space-y-4" @submit.prevent="handleSubmit">
          <div>
            <label class="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">E-mail</label>
            <input
              v-model="email"
              type="email"
              required
              placeholder="voce@exemplo.com"
              class="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm text-slate-900 shadow-sm transition placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-4 focus:ring-indigo-500/15"
            />
          </div>
          <div>
            <label class="mb-1 block text-xs font-semibold uppercase tracking-wide text-slate-500">Senha</label>
            <input
              v-model="password"
              type="password"
              required
              placeholder="••••••••"
              class="w-full rounded-lg border border-slate-300 px-3 py-2.5 text-sm text-slate-900 shadow-sm transition placeholder:text-slate-400 focus:border-indigo-500 focus:outline-none focus:ring-4 focus:ring-indigo-500/15"
            />
          </div>
          <p v-if="error" class="rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-700">{{ error }}</p>
          <button
            type="submit"
            :disabled="loading"
            class="flex w-full items-center justify-center gap-2 rounded-lg bg-gradient-to-r from-indigo-600 to-violet-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm shadow-indigo-500/30 transition hover:brightness-110 disabled:opacity-60"
          >
            <svg v-if="loading" class="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
              <circle class="opacity-30" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" />
              <path d="M22 12a10 10 0 0 0-10-10" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
            </svg>
            {{ loading ? 'Entrando...' : 'Entrar' }}
          </button>
        </form>
      </div>
      <p class="mt-6 text-center text-sm text-slate-500">
        Não tem conta?
        <RouterLink to="/register" class="font-medium text-indigo-600 hover:text-indigo-700">Cadastre-se</RouterLink>
      </p>
    </div>
  </div>
</template>
