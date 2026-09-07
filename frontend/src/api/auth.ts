import { api } from './client'

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
}

export function login(request: LoginRequest) {
  return api.post<LoginResponse>('/auth/login', request)
}
