import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { HttpError } from '@/api/client'
import { useAuthStore } from '@/stores/auth'
import LoginView from './LoginView.vue'

vi.mock('@/api/auth', () => ({
  login: vi.fn(),
}))

import { login } from '@/api/auth'

function fakeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.signature`
}

async function mountLoginView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div>home</div>' } },
      { path: '/login', component: LoginView },
      { path: '/register', component: { template: '<div>register</div>' } },
    ],
  })
  router.push('/login')
  await router.isReady()

  const wrapper = mount(LoginView, { global: { plugins: [router] } })
  return { wrapper, router }
}

describe('LoginView', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(login).mockReset()
  })

  it('logs in and redirects home on success', async () => {
    const token = fakeJwt({ sub: 'c1', email: 'alice@example.com', roles: ['CUSTOMER'], exp: Math.floor(Date.now() / 1000) + 3600 })
    vi.mocked(login).mockResolvedValue({ accessToken: token, tokenType: 'Bearer', expiresInSeconds: 3600 })

    const { wrapper, router } = await mountLoginView()
    await wrapper.find('input[type="email"]').setValue('alice@example.com')
    await wrapper.find('input[type="password"]').setValue('password123')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(login).toHaveBeenCalledWith({ email: 'alice@example.com', password: 'password123' })
    expect(useAuthStore().isAuthenticated).toBe(true)
    expect(router.currentRoute.value.path).toBe('/')
  })

  it('shows the server error message and stays on the page when login fails', async () => {
    vi.mocked(login).mockRejectedValue(
      new HttpError(401, { status: 401, error: 'Unauthorized', message: 'Invalid email or password', timestamp: 'now' }),
    )

    const { wrapper, router } = await mountLoginView()
    await wrapper.find('input[type="email"]').setValue('alice@example.com')
    await wrapper.find('input[type="password"]').setValue('wrong-password')
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Invalid email or password')
    expect(useAuthStore().isAuthenticated).toBe(false)
    expect(router.currentRoute.value.path).toBe('/login')
  })
})
