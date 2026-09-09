import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from './auth'

function fakeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.signature`
}

const STORAGE_KEY = 'payment-processing.token'

describe('auth store', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('starts unauthenticated when there is no stored token', () => {
    const auth = useAuthStore()
    expect(auth.isAuthenticated).toBe(false)
    expect(auth.customerId).toBeNull()
    expect(auth.roles).toEqual([])
  })

  it('decodes the token on setToken and persists it to localStorage', () => {
    const auth = useAuthStore()
    const token = fakeJwt({
      sub: 'customer-123',
      email: 'alice@example.com',
      roles: ['CUSTOMER'],
      exp: Math.floor(Date.now() / 1000) + 3600,
    })

    auth.setToken(token)

    expect(auth.isAuthenticated).toBe(true)
    expect(auth.customerId).toBe('customer-123')
    expect(auth.email).toBe('alice@example.com')
    expect(auth.isAdmin).toBe(false)
    expect(localStorage.getItem(STORAGE_KEY)).toBe(token)
  })

  it('recognizes the ADMIN role', () => {
    const auth = useAuthStore()
    auth.setToken(
      fakeJwt({
        sub: 'admin-1',
        email: 'admin@example.com',
        roles: ['ADMIN'],
        exp: Math.floor(Date.now() / 1000) + 3600,
      }),
    )

    expect(auth.isAdmin).toBe(true)
  })

  it('treats an expired token as unauthenticated', () => {
    const auth = useAuthStore()
    auth.setToken(
      fakeJwt({
        sub: 'customer-123',
        email: 'alice@example.com',
        roles: ['CUSTOMER'],
        exp: Math.floor(Date.now() / 1000) - 60,
      }),
    )

    expect(auth.isAuthenticated).toBe(false)
  })

  it('clears everything on logout', () => {
    const auth = useAuthStore()
    auth.setToken(
      fakeJwt({ sub: 'x', email: 'x@example.com', roles: [], exp: Math.floor(Date.now() / 1000) + 3600 }),
    )

    auth.logout()

    expect(auth.isAuthenticated).toBe(false)
    expect(auth.token).toBeNull()
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull()
  })

  it('hydrates from a token already in localStorage when the store is created', () => {
    localStorage.setItem(
      STORAGE_KEY,
      fakeJwt({ sub: 'restored', email: 'r@example.com', roles: ['CUSTOMER'], exp: Math.floor(Date.now() / 1000) + 3600 }),
    )

    const auth = useAuthStore()

    expect(auth.isAuthenticated).toBe(true)
    expect(auth.customerId).toBe('restored')
  })
})
