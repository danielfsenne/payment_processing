import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { HttpError } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import RegisterView from './RegisterView.vue'

vi.mock('@/api/customers', () => ({
  registerCustomer: vi.fn(),
}))
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
}))

import { registerCustomer } from '@/api/customers'
import { login } from '@/api/auth'

function fakeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.signature`
}

async function mountRegisterView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>home</div>' } },
      { path: '/login', component: { template: '<div>login</div>' } },
      { path: '/register', component: RegisterView },
    ],
  })
  router.push('/register')
  await router.isReady()

  const wrapper = mount(RegisterView, { global: { plugins: [router] } })
  return { wrapper, router }
}

describe('RegisterView', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(registerCustomer).mockReset()
    vi.mocked(login).mockReset()
  })

  it('registers, logs in automatically and redirects home', async () => {
    vi.mocked(registerCustomer).mockResolvedValue({
      id: 'cust-1',
      name: 'Alice',
      email: 'alice@example.com',
      document: '12345678900',
      role: 'CUSTOMER',
      createdAt: '2026-01-01T00:00:00Z',
    })
    const token = fakeJwt({ sub: 'cust-1', email: 'alice@example.com', roles: ['CUSTOMER'], exp: Math.floor(Date.now() / 1000) + 3600 })
    vi.mocked(login).mockResolvedValue({ accessToken: token, tokenType: 'Bearer', expiresInSeconds: 3600, refreshToken: 'refresh-1' })

    const { wrapper, router } = await mountRegisterView()
    await wrapper.find('input[required]:not([type])').setValue('Alice')
    await wrapper.find('input[type="email"]').setValue('alice@example.com')
    await wrapper.find('input[type="password"]').setValue('password123')
    const documentInput = wrapper.findAll('input').find((i) => i.attributes('placeholder') === 'CPF ou CNPJ')
    await documentInput!.setValue('12345678900')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(registerCustomer).toHaveBeenCalledWith({
      name: 'Alice',
      email: 'alice@example.com',
      document: '12345678900',
      password: 'password123',
    })
    expect(login).toHaveBeenCalledWith({ email: 'alice@example.com', password: 'password123' })
    expect(useAuthStore().isAuthenticated).toBe(true)
    expect(useAuthStore().refreshToken).toBe('refresh-1')
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('shows the server error and stays on the page when registration fails', async () => {
    vi.mocked(registerCustomer).mockRejectedValue(
      new HttpError(409, { status: 409, error: 'Conflict', message: 'Email already in use', timestamp: 'now' }),
    )

    const { wrapper, router } = await mountRegisterView()
    await wrapper.find('input[type="email"]').setValue('taken@example.com')
    await wrapper.find('input[type="password"]').setValue('password123')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Email already in use')
    expect(login).not.toHaveBeenCalled()
    expect(useAuthStore().isAuthenticated).toBe(false)
    expect(router.currentRoute.value.path).toBe('/register')
  })
})
