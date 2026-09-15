import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import CustomerDetailView from './CustomerDetailView.vue'
import type { Account, Customer } from '@/types'

vi.mock('@/api/customers', () => ({
  getCustomer: vi.fn(),
}))
vi.mock('@/api/accounts', () => ({
  listAccounts: vi.fn(),
}))

import { getCustomer } from '@/api/customers'
import { listAccounts } from '@/api/accounts'

const customer: Customer = {
  id: 'cust-1',
  name: 'Alice',
  email: 'alice@example.com',
  document: '12345678900',
  role: 'CUSTOMER',
  createdAt: '2026-01-01T00:00:00Z',
}

function accountFor(customerId: string): Account {
  return {
    id: `acc-${customerId}`,
    customerId,
    balance: 500,
    reservedAmount: 0,
    availableBalance: 500,
    currency: 'BRL',
    createdAt: '2026-01-01T00:00:00Z',
  }
}

async function mountDetailView(id = 'cust-1') {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/customers', component: { template: '<div>list</div>' } },
      { path: '/customers/:id', component: CustomerDetailView },
      { path: '/accounts/:id', component: { template: '<div>account</div>' } },
    ],
  })
  router.push(`/customers/${id}`)
  await router.isReady()

  const wrapper = mount(CustomerDetailView, { global: { plugins: [router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('CustomerDetailView', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(getCustomer).mockReset()
    vi.mocked(listAccounts).mockReset()
  })

  it('renders the customer and only their own accounts', async () => {
    vi.mocked(getCustomer).mockResolvedValue(customer)
    vi.mocked(listAccounts).mockResolvedValue([accountFor('cust-1'), accountFor('cust-2')])

    const { wrapper } = await mountDetailView()

    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('alice@example.com')
    const rows = wrapper.findAll('tbody tr')
    expect(rows).toHaveLength(1)
  })

  it('shows a not-found state when the customer does not exist', async () => {
    vi.mocked(getCustomer).mockResolvedValue(null as unknown as Customer)
    vi.mocked(listAccounts).mockResolvedValue([])

    const { wrapper } = await mountDetailView()

    expect(wrapper.text()).toContain('Cliente não encontrado.')
  })

  it('shows an empty state when the customer has no accounts', async () => {
    vi.mocked(getCustomer).mockResolvedValue(customer)
    vi.mocked(listAccounts).mockResolvedValue([])

    const { wrapper } = await mountDetailView()

    expect(wrapper.text()).toContain('Nenhuma conta encontrada.')
  })
})
