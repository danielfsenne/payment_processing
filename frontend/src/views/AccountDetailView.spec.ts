import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AccountDetailView from './AccountDetailView.vue'
import type { Account, Payment } from '@/types'

vi.mock('@/api/accounts', () => ({
  getAccount: vi.fn(),
}))
vi.mock('@/api/payments', () => ({
  listPayments: vi.fn(),
}))

import { getAccount } from '@/api/accounts'
import { listPayments } from '@/api/payments'

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

function paymentFor(accountId: string): Payment {
  return {
    id: `pay-${accountId}`,
    customerId: 'cust-1',
    accountId,
    amount: 100,
    currency: 'BRL',
    status: 'SETTLED',
    reservationId: null,
    failureReason: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
  }
}

async function mountDetailView(id = 'acc-1') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/accounts', component: { template: '<div>list</div>' } },
      { path: '/accounts/:id', component: AccountDetailView },
      { path: '/payments/:id', component: { template: '<div>payment</div>' } },
    ],
  })
  router.push(`/accounts/${id}`)
  await router.isReady()

  const wrapper = mount(AccountDetailView, { global: { plugins: [router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('AccountDetailView', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(getAccount).mockReset()
    vi.mocked(listPayments).mockReset()
  })

  it('renders the account balance and only its own payments', async () => {
    vi.mocked(getAccount).mockResolvedValue(account)
    vi.mocked(listPayments).mockResolvedValue([paymentFor('acc-1'), paymentFor('acc-2')])

    const { wrapper } = await mountDetailView()

    expect(wrapper.text()).toContain('450.00')
    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(1)
  })

  it('shows a not-found state when the account does not exist', async () => {
    vi.mocked(getAccount).mockResolvedValue(null as unknown as Account)
    vi.mocked(listPayments).mockResolvedValue([])

    const { wrapper } = await mountDetailView()

    expect(wrapper.text()).toContain('Conta não encontrada.')
  })

  it('hides the customer id for a non-admin viewer', async () => {
    const auth = useAuthStore()
    auth.setToken(fakeJwt({ sub: 'cust-1', email: 'c@example.com', roles: ['CUSTOMER'], exp: Math.floor(Date.now() / 1000) + 3600 }))
    vi.mocked(getAccount).mockResolvedValue(account)
    vi.mocked(listPayments).mockResolvedValue([])

    const { wrapper } = await mountDetailView()

    expect(wrapper.text()).not.toContain('Customer ID')
  })

  it('shows the customer id for an admin viewer', async () => {
    const auth = useAuthStore()
    auth.setToken(fakeJwt({ sub: 'admin-1', email: 'a@example.com', roles: ['ADMIN'], exp: Math.floor(Date.now() / 1000) + 3600 }))
    vi.mocked(getAccount).mockResolvedValue(account)
    vi.mocked(listPayments).mockResolvedValue([])

    const { wrapper } = await mountDetailView()

    expect(wrapper.text()).toContain('Customer ID')
    expect(wrapper.text()).toContain('cust-1')
  })
})
