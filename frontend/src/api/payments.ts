import { api } from './client'
import type { Payment, PaymentEvent } from '@/types'

export interface CreatePaymentRequest {
  customerId: string
  accountId: string
  amount: number
  currency: string
}

export function listPayments() {
  return api.get<Payment[]>('/payments')
}

export function createPayment(request: CreatePaymentRequest, idempotencyKey: string) {
  return api.post<Payment>('/payments', request, { 'Idempotency-Key': idempotencyKey })
}

export function processPayment(id: string, simulateFailure: boolean) {
  return api.post<Payment>(`/payments/${id}/process`, { simulateProcessingFailure: simulateFailure })
}

export function getPaymentEvents(id: string) {
  return api.get<PaymentEvent[]>(`/payments/${id}/events`)
}
