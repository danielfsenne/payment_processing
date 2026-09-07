export type Role = 'CUSTOMER' | 'ADMIN'

export interface JwtPayload {
  sub: string
  email: string
  roles: Role[]
  iss: string
  iat: number
  exp: number
}

export interface Customer {
  id: string
  name: string
  email: string
  document: string
  role: Role
  createdAt: string
}

export interface Account {
  id: string
  customerId: string
  balance: number
  reservedAmount: number
  availableBalance: number
  currency: string
  createdAt: string
}

export type PaymentStatus =
  | 'CREATED'
  | 'PROCESSING'
  | 'AUTHORIZED'
  | 'CAPTURED'
  | 'SETTLED'
  | 'FAILED'
  | 'RETRYING'

export interface Payment {
  id: string
  customerId: string
  accountId: string
  amount: number
  currency: string
  status: PaymentStatus
  reservationId: string | null
  failureReason: string | null
  createdAt: string
  updatedAt: string
}

export interface PaymentEvent {
  id: string
  eventType: string
  fromStatus: PaymentStatus | null
  toStatus: PaymentStatus
  createdAt: string
}

export interface PaymentTopicEvent {
  paymentId: string
  customerId: string
  accountId: string
  amount: number
  currency: string
  eventType: string
  fromStatus: PaymentStatus | null
  toStatus: PaymentStatus
  occurredAt: string
}

export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
}
