import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import router from './index'

function fakeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.signature`
}

const customerToken = () =>
  fakeJwt({ sub: 'c1', email: 'c@example.com', roles: ['CUSTOMER'], exp: Math.floor(Date.now() / 1000) + 3600 })

const adminToken = () =>
  fakeJwt({ sub: 'a1', email: 'a@example.com', roles: ['ADMIN'], exp: Math.floor(Date.now() / 1000) + 3600 })

describe('router navigation guard', () => {
  beforeEach(async () => {
    localStorage.clear()
    setActivePinia(createPinia())
    await router.push('/login')
  })

  it('redirects an unauthenticated visitor away from a protected route', async () => {
    await router.push('/payments')
    expect(router.currentRoute.value.name).toBe('login')
  })

  it('lets an authenticated customer reach a protected route', async () => {
    useAuthStore().setToken(customerToken())

    await router.push('/payments')

    expect(router.currentRoute.value.name).toBe('payments')
  })

  it('sends an already-authenticated visitor away from the login page', async () => {
    useAuthStore().setToken(customerToken())

    // already sitting on /login from beforeEach; navigating to a different public
    // route first ensures the next push to /login actually re-runs the guard
    await router.push('/register')
    await router.push('/login')

    expect(router.currentRoute.value.name).toBe('home')
  })

  it('blocks a non-admin customer from the admin-only customers route', async () => {
    useAuthStore().setToken(customerToken())

    await router.push('/customers')

    expect(router.currentRoute.value.name).toBe('home')
  })

  it('lets an admin reach the admin-only customers route', async () => {
    useAuthStore().setToken(adminToken())

    await router.push('/customers')

    expect(router.currentRoute.value.name).toBe('customers')
  })
})
