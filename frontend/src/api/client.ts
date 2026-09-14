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

// Endpoints the retry-after-refresh logic must never touch, otherwise a 401 from
// login/refresh itself would try to refresh using the very token that just failed.
const AUTH_ENDPOINTS = new Set(['/auth/login', '/auth/refresh'])

// Concurrent 401s (e.g. several widgets fetching at once right as the access token
// expires) must not each fire their own refresh call - that would race two rotations
// against the single-use refresh token and fail every request but the first. This
// makes every caller share the one in-flight refresh.
let refreshPromise: Promise<boolean> | null = null

async function refreshAccessToken(): Promise<boolean> {
  const auth = useAuthStore()
  if (!auth.refreshToken) {
    return false
  }
  if (!refreshPromise) {
    refreshPromise = doRefresh(auth.refreshToken).finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

async function doRefresh(refreshToken: string): Promise<boolean> {
  const auth = useAuthStore()
  try {
    const response = await fetch('/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
    if (!response.ok) {
      auth.logout()
      return false
    }
    const data = await response.json()
    auth.setSession(data.accessToken, data.refreshToken)
    return true
  } catch {
    return false
  }
}

async function request<T>(path: string, init: RequestInit = {}, isRetry = false): Promise<T> {
  const auth = useAuthStore()
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  if (auth.token) {
    headers.set('Authorization', `Bearer ${auth.token}`)
  }

  const response = await fetch(`/api${path}`, { ...init, headers })

  if (response.status === 401 && !isRetry && !AUTH_ENDPOINTS.has(path) && auth.refreshToken) {
    const refreshed = await refreshAccessToken()
    if (refreshed) {
      return request<T>(path, init, true)
    }
  }

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
