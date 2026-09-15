import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import PaymentDetailView from './PaymentDetailView.vue'
import type { Payment, PaymentEvent } from '@/types'

vi.mock('@/api/payments', () => ({
  getPayment: vi.fn(),
  getPaymentEvents: vi.fn(),
  processPayment: vi.fn(),
}))
vi.mock('@/ws/paymentSocket', () => ({
  connectPaymentSocket: vi.fn(() => ({ deactivate: vi.fn() })),
}))

import { getPayment, getPaymentEvents, processPayment } from '@/api/payments'

const payment: Payment = {
  id: 'pay-1',
  customerId: 'cust-1',
  accountId: 'acc-1',
  amount: 250,
  currency: 'BRL',
  status: 'CREATED',
  reservationId: null,
  failureReason: null,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-01T00:00:00Z',
}

const event: PaymentEvent = {
  id: 'evt-1',
  eventType: 'CREATED',
  fromStatus: null,
  toStatus: 'CREATED',
  createdAt: '2026-01-01T00:00:00Z',
}

async function mountDetailView(id = 'pay-1') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/payments', component: { template: '<div>list</div>' } },
      { path: '/payments/:id', component: PaymentDetailView },
      { path: '/accounts/:id', component: { template: '<div>account</div>' } },
    ],
  })
  router.push(`/payments/${id}`)
  await router.isReady()

  const wrapper = mount(PaymentDetailView, { global: { plugins: [router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('PaymentDetailView', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(getPayment).mockReset()
    vi.mocked(getPaymentEvents).mockReset()
    vi.mocked(processPayment).mockReset()
  })

  it('renders the payment and its event timeline', async () => {
    vi.mocked(getPayment).mockResolvedValue(payment)
    vi.mocked(getPaymentEvents).mockResolvedValue([event])

    const { wrapper } = await mountDetailView()

    expect(getPayment).toHaveBeenCalledWith('pay-1')
    expect(wrapper.text()).toContain('250.00 BRL')
    expect(wrapper.text()).toContain('CREATED')
  })

  it('shows a not-found state when the payment does not exist', async () => {
    vi.mocked(getPayment).mockResolvedValue(null as unknown as Payment)
    vi.mocked(getPaymentEvents).mockResolvedValue([])

    const { wrapper } = await mountDetailView()

    expect(wrapper.text()).toContain('Pagamento não encontrado.')
  })

  it('shows the failure reason when the payment failed', async () => {
    vi.mocked(getPayment).mockResolvedValue({ ...payment, status: 'FAILED', failureReason: 'Saldo insuficiente' })
    vi.mocked(getPaymentEvents).mockResolvedValue([event])

    const { wrapper } = await mountDetailView()

    expect(wrapper.text()).toContain('Saldo insuficiente')
  })

  it('processes the payment and refreshes its status and events', async () => {
    vi.mocked(getPayment).mockResolvedValue(payment)
    vi.mocked(getPaymentEvents).mockResolvedValue([event])
    vi.mocked(processPayment).mockResolvedValue({ ...payment, status: 'AUTHORIZED' })

    const { wrapper } = await mountDetailView()
    await wrapper.find('button').trigger('click')
    await flushPromises()

    expect(processPayment).toHaveBeenCalledWith('pay-1', false)
    expect(wrapper.text()).toContain('Autorizado')
  })

  it('does not show a process button once the payment is settled', async () => {
    vi.mocked(getPayment).mockResolvedValue({ ...payment, status: 'SETTLED' })
    vi.mocked(getPaymentEvents).mockResolvedValue([event])

    const { wrapper } = await mountDetailView()

    expect(wrapper.find('button').exists()).toBe(false)
  })
})
