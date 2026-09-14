import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { JwtPayload, Role } from '@/types'

const STORAGE_KEY = 'payment-processing.token'
const REFRESH_STORAGE_KEY = 'payment-processing.refreshToken'

function decodeJwt(token: string): JwtPayload {
  const [, payload] = token.split('.')
  const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
  return JSON.parse(json)
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem(STORAGE_KEY))
  const refreshToken = ref<string | null>(localStorage.getItem(REFRESH_STORAGE_KEY))
  const payload = ref<JwtPayload | null>(token.value ? decodeJwt(token.value) : null)

  const isAuthenticated = computed(() => !!token.value && !isExpired.value)
  const isExpired = computed(() => {
    if (!payload.value) return true
    return payload.value.exp * 1000 < Date.now()
  })
  const customerId = computed(() => payload.value?.sub ?? null)
  const email = computed(() => payload.value?.email ?? null)
  const roles = computed<Role[]>(() => payload.value?.roles ?? [])
  const isAdmin = computed(() => roles.value.includes('ADMIN'))

  function setToken(newToken: string) {
    token.value = newToken
    payload.value = decodeJwt(newToken)
    localStorage.setItem(STORAGE_KEY, newToken)
  }

  function setRefreshToken(newRefreshToken: string) {
    refreshToken.value = newRefreshToken
    localStorage.setItem(REFRESH_STORAGE_KEY, newRefreshToken)
  }

  function setSession(newToken: string, newRefreshToken: string) {
    setToken(newToken)
    setRefreshToken(newRefreshToken)
  }

  function logout() {
    token.value = null
    refreshToken.value = null
    payload.value = null
    localStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem(REFRESH_STORAGE_KEY)
  }

  return {
    token,
    refreshToken,
    isAuthenticated,
    customerId,
    email,
    roles,
    isAdmin,
    setToken,
    setRefreshToken,
    setSession,
    logout,
  }
})
