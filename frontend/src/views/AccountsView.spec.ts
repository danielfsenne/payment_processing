import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AccountsView from './AccountsView.vue'
import type { Account } from '@/types'

vi.mock('@/api/accounts', () => ({
  listAccounts: vi.fn(),
  createAccount: vi.fn(),
}))

import { listAccounts, createAccount } from '@/api/accounts'

function fakeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.signature`
}

const account: Account = {
  id: 'acc-1',
  customerId: 'cust-1',
  balance: 500,
  reservedAmount: 50,
  availableBalance: 450,
  currency: 'BRL',
  createdAt: '2026-01-01T00:00:00Z',
}

async function mountAccountsView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/accounts', component: AccountsView },
      { path: '/accounts/:id', component: { template: '<div>detail</div>' } },
    ],
  })
  router.push('/accounts')
  await router.isReady()

  const wrapper = mount(AccountsView, { global: { plugins: [router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('AccountsView', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(listAccounts).mockReset()
    vi.mocked(createAccount).mockReset()
  })

  it('renders the fetched accounts in the table', async () => {
    vi.mocked(listAccounts).mockResolvedValue([account])

    const { wrapper } = await mountAccountsView()

    expect(wrapper.text()).toContain('450.00')
    expect(wrapper.text()).toContain('BRL')
  })

  it('shows an empty state when there are no accounts', async () => {
    vi.mocked(listAccounts).mockResolvedValue([])

    const { wrapper } = await mountAccountsView()

    expect(wrapper.text()).toContain('Nenhuma conta encontrada.')
  })

  it('hides the customer id column and field for a non-admin customer', async () => {
    const auth = useAuthStore()
    auth.setToken(fakeJwt({ sub: 'cust-1', email: 'c@example.com', roles: ['CUSTOMER'], exp: Math.floor(Date.now() / 1000) + 3600 }))
    vi.mocked(listAccounts).mockResolvedValue([account])

    const { wrapper } = await mountAccountsView()

    expect(wrapper.text()).not.toContain('Customer ID')
    expect(wrapper.text()).not.toContain('cust-1')
  })

  it('creates an account and refreshes the list', async () => {
    vi.mocked(listAccounts).mockResolvedValueOnce([]).mockResolvedValueOnce([account])
    vi.mocked(createAccount).mockResolvedValue(account)

    const { wrapper } = await mountAccountsView()
    await wrapper.find('input[type="number"]').setValue(500)
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(createAccount).toHaveBeenCalledWith({ customerId: '', initialBalance: 500, currency: 'BRL' })
    expect(wrapper.text()).toContain('450.00')
  })

  it('shows an error message when creating an account fails', async () => {
    vi.mocked(listAccounts).mockResolvedValue([])
    const { HttpError } = await import('@/api/client')
    vi.mocked(createAccount).mockRejectedValue(new HttpError(400, { status: 400, error: 'Bad Request', message: 'Moeda inválida', timestamp: 'now' }))

    const { wrapper } = await mountAccountsView()
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Moeda inválida')
  })
})
