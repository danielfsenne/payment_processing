import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import CustomersView from './CustomersView.vue'
import type { Customer } from '@/types'

vi.mock('@/api/customers', () => ({
  listCustomers: vi.fn(),
}))

import { listCustomers } from '@/api/customers'

const customer: Customer = {
  id: 'cust-1',
  name: 'Alice',
  email: 'alice@example.com',
  document: '12345678900',
  role: 'CUSTOMER',
  createdAt: '2026-01-01T00:00:00Z',
}

async function mountCustomersView() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/customers', component: CustomersView },
      { path: '/customers/:id', component: { template: '<div>detail</div>' } },
    ],
  })
  router.push('/customers')
  await router.isReady()

  const wrapper = mount(CustomersView, { global: { plugins: [router] } })
  await flushPromises()
  return { wrapper, router }
}

describe('CustomersView', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.mocked(listCustomers).mockReset()
  })

  it('renders the fetched customers', async () => {
    vi.mocked(listCustomers).mockResolvedValue([customer])

    const { wrapper } = await mountCustomersView()

    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('alice@example.com')
    expect(wrapper.text()).toContain('Cliente')
  })

  it('labels an admin customer accordingly', async () => {
    vi.mocked(listCustomers).mockResolvedValue([{ ...customer, role: 'ADMIN' }])

    const { wrapper } = await mountCustomersView()

    expect(wrapper.text()).toContain('Admin')
  })

  it('shows an empty state when there are no customers', async () => {
    vi.mocked(listCustomers).mockResolvedValue([])

    const { wrapper } = await mountCustomersView()

    expect(wrapper.text()).toContain('Nenhum cliente encontrado.')
  })

  it('navigates to the customer detail page on row click', async () => {
    vi.mocked(listCustomers).mockResolvedValue([customer])

    const { wrapper, router } = await mountCustomersView()
    await wrapper.find('tbody tr').trigger('click')
    await flushPromises()

    expect(router.currentRoute.value.path).toBe('/customers/cust-1')
  })
})
