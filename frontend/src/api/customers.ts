import { api } from './client'
import type { Customer } from '@/types'

export interface RegisterCustomerRequest {
  name: string
  email: string
  document: string
  password: string
}

export function registerCustomer(request: RegisterCustomerRequest) {
  return api.post<Customer>('/customers', request)
}

export function listCustomers() {
  return api.get<Customer[]>('/customers')
}

export function getCustomer(id: string) {
  return api.get<Customer>(`/customers/${id}`)
}
