import { api } from './client'
import type { Account } from '@/types'

export interface CreateAccountRequest {
  customerId: string
  initialBalance: number
  currency: string
}

export function listAccounts() {
  return api.get<Account[]>('/accounts')
}

export function createAccount(request: CreateAccountRequest) {
  return api.post<Account>('/accounts', request)
}
