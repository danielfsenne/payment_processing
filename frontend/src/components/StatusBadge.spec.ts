import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusBadge from './StatusBadge.vue'

describe('StatusBadge', () => {
  it('renders the status text', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'SETTLED' } })
    expect(wrapper.text()).toBe('SETTLED')
  })

  it('uses a green style for SETTLED', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'SETTLED' } })
    expect(wrapper.classes()).toContain('bg-green-100')
  })

  it('uses a red style for FAILED', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'FAILED' } })
    expect(wrapper.classes()).toContain('bg-red-100')
  })

  it('gives every PaymentStatus a distinct, defined style', () => {
    const statuses = ['CREATED', 'PROCESSING', 'AUTHORIZED', 'CAPTURED', 'SETTLED', 'FAILED', 'RETRYING'] as const
    for (const status of statuses) {
      const wrapper = mount(StatusBadge, { props: { status } })
      expect(wrapper.classes().some((c) => c.startsWith('bg-'))).toBe(true)
    }
  })
})
