import { api } from './client'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  refreshToken: string
}

export function login(request: LoginRequest) {
  return api.post<LoginResponse>('/auth/login', request)
}

export function refresh(refreshToken: string) {
  return api.post<LoginResponse>('/auth/refresh', { refreshToken })
}

export function logout(refreshToken: string | null) {
  return api.post<void>('/auth/logout', refreshToken ? { refreshToken } : undefined)
}
