import { useAuthStore } from '@/stores/auth'
import type { ApiError } from '@/types'

export class HttpError extends Error {
  constructor(
    public status: number,
    public body: ApiError | null,
  ) {
    super(body?.message ?? `Request failed with status ${status}`)
  }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const auth = useAuthStore()
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  if (auth.token) {
    headers.set('Authorization', `Bearer ${auth.token}`)
  }

  const response = await fetch(`/api${path}`, { ...init, headers })

  if (response.status === 204) {
    return undefined as T
  }

  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    throw new HttpError(response.status, data)
  }
  return data as T
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body?: unknown, extraHeaders?: Record<string, string>) =>
    request<T>(path, { method: 'POST', body: body ? JSON.stringify(body) : undefined, headers: extraHeaders }),
}
