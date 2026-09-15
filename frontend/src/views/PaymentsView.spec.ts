import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import PaymentsView from './PaymentsView.vue'
import type { Account, Payment } from '@/types'

vi.mock('@/api/payments', () => ({
  listPayments: vi.fn(),
  createPayment: vi.fn(),
  processPayment: vi.fn(),
}))
vi.mock('@/api/accounts', () => ({
  listAccounts: vi.fn(),
}))
vi.mock('@/ws/paymentSocket', () => ({
  connectPaymentSocket: vi.fn(() => ({ deactivate: vi.fn() })),
}))

import { listPayments, createPayment, processPayment } from '@/api/payments'
import { listAccounts } from '@/api/accounts'

function fakeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.signature`
}

const account: Account = {
  id: 'acc-1',
  customerId: 'cust-1',
  balance: 500,
  reservedAmount: 0,
  availableBalance: 500,
  currency: 'BRL',
  createdAt: '2026-01-01T00:00:00Z',
}

const payment: Payment = {
  id: 'pay-1',
  customerId: 'cust-1',
  accountId: 'acc-1',
  amount: 100,
  currency: 'BRL',
  status: 'CREATED',
  reservationId: null,
  failureReason: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

async function mountPaymentsView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/payments', component: PaymentsView },
      { path: '/payments/:id', component: { template: '<div>detail</div>' } },
    ],
  })
  router.push('/payments')
  await router.isReady()

  const wrapper = mount(PaymentsView, { global: { plugins: [router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('PaymentsView', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(listPayments).mockReset()
    vi.mocked(createPayment).mockReset()
    vi.mocked(processPayment).mockReset()
    vi.mocked(listAccounts).mockReset()
    vi.mocked(listAccounts).mockResolvedValue([account])
  })

  it('renders the fetched payments once loading finishes', async () => {
    vi.mocked(listPayments).mockResolvedValue([payment])

    const { wrapper } = await mountPaymentsView()

    expect(wrapper.text()).toContain('100.00 BRL')
    expect(wrapper.text()).not.toContain('Carregando')
  })

  it('shows an empty state when there are no payments', async () => {
    vi.mocked(listPayments).mockResolvedValue([])

    const { wrapper } = await mountPaymentsView()

    expect(wrapper.text()).toContain('Nenhum pagamento encontrado.')
  })

  it('hides the customer id field for a non-admin customer', async () => {
    const auth = useAuthStore()
    auth.setToken(fakeJwt({ sub: 'cust-1', email: 'c@example.com', roles: ['CUSTOMER'], exp: Math.floor(Date.now() / 1000) + 3600 }))
    vi.mocked(listPayments).mockResolvedValue([])

    const { wrapper } = await mountPaymentsView()

    expect(wrapper.findAll('label').map((l) => l.text())).not.toContain('Customer ID')
  })

  it('shows the customer id field for an admin', async () => {
    const auth = useAuthStore()
    auth.setToken(fakeJwt({ sub: 'admin-1', email: 'a@example.com', roles: ['ADMIN'], exp: Math.floor(Date.now() / 1000) + 3600 }))
    vi.mocked(listPayments).mockResolvedValue([])

    const { wrapper } = await mountPaymentsView()

    expect(wrapper.findAll('label').map((l) => l.text())).toContain('Customer ID')
  })

  it('creates a payment with a fresh idempotency key and adds it to the list', async () => {
    vi.mocked(listPayments).mockResolvedValue([])
    vi.mocked(createPayment).mockResolvedValue(payment)

    const { wrapper } = await mountPaymentsView()

    await wrapper.find('input[type="number"]').setValue(100)
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(createPayment).toHaveBeenCalledWith(
      { customerId: '', accountId: 'acc-1', amount: 100, currency: 'BRL' },
      expect.any(String),
    )
    expect(wrapper.text()).toContain('100.00 BRL')
  })

  it('shows an error message when creating a payment fails', async () => {
    vi.mocked(listPayments).mockResolvedValue([])
    const { HttpError } = await import('@/api/client')
    vi.mocked(createPayment).mockRejectedValue(new HttpError(409, { status: 409, error: 'Conflict', message: 'Saldo insuficiente', timestamp: 'now' }))

    const { wrapper } = await mountPaymentsView()

    await wrapper.find('input[type="number"]').setValue(100)
    await wrapper.find('form').trigger('submit.prevent')
    await flushPromises()

    expect(wrapper.text()).toContain('Saldo insuficiente')
  })

  it('processes a CREATED payment and reflects its updated status', async () => {
    vi.mocked(listPayments).mockResolvedValue([payment])
    vi.mocked(processPayment).mockResolvedValue({ ...payment, status: 'AUTHORIZED' })

    const { wrapper } = await mountPaymentsView()

    await wrapper.find('button:not([type="submit"])').trigger('click')
    await flushPromises()

    expect(processPayment).toHaveBeenCalledWith('pay-1', false)
    expect(wrapper.text()).toContain('Autorizado')
  })
})
