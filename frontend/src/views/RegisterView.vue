<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerCustomer } from '@/api/customers'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import { HttpError } from '@/api/client'

const name = ref('')
const email = ref('')
const document = ref('')
const password = ref('')
const error = ref<string | null>(null)
const loading = ref(false)

const auth = useAuthStore()
const router = useRouter()

async function handleSubmit() {
  error.value = null
  loading.value = true
  try {
    await registerCustomer({ name: name.value, email: email.value, document: document.value, password: password.value })
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
  <div class="mx-auto mt-16 max-w-sm">
    <h1 class="mb-6 text-center text-2xl font-semibold text-gray-800">Criar conta</h1>
    <form class="space-y-4" @submit.prevent="handleSubmit">
      <div>
        <label class="block text-sm font-medium text-gray-700">Nome</label>
        <input v-model="name" required class="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none" />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700">E-mail</label>
        <input v-model="email" type="email" required class="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none" />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700">Documento</label>
        <input v-model="document" required class="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none" />
      </div>
      <div>
        <label class="block text-sm font-medium text-gray-700">Senha</label>
        <input v-model="password" type="password" required minlength="8" class="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none" />
      </div>
      <p v-if="error" class="text-sm text-red-600">{{ error }}</p>
      <button
        type="submit"
        :disabled="loading"
        class="w-full rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {{ loading ? 'Criando...' : 'Criar conta' }}
      </button>
    </form>
    <p class="mt-4 text-center text-sm text-gray-600">
      Já tem conta?
      <RouterLink to="/login" class="text-blue-600 hover:underline">Entrar</RouterLink>
    </p>
  </div>
</template>
