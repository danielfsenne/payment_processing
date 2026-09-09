import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import { api, HttpError } from './client'

function fakeJwt(payload: Record<string, unknown>): string {
  const header = btoa(JSON.stringify({ alg: 'HS256' }))
  const body = btoa(JSON.stringify(payload))
  return `${header}.${body}.signature`
}

function jsonResponse(status: number, body: unknown) {
  return new Response(body === undefined ? null : JSON.stringify(body), { status })
}

describe('api client', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.restoreAllMocks()
  })

  it('does not send an Authorization header when there is no token', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(200, { ok: true }))

    await api.get('/accounts')

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/accounts')
    expect((init?.headers as Headers).has('Authorization')).toBe(false)
  })

  it('sends a Bearer Authorization header once a token is set', async () => {
    const auth = useAuthStore()
    auth.setToken(fakeJwt({ sub: 'x', email: 'x@example.com', roles: [], exp: Math.floor(Date.now() / 1000) + 3600 }))
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(200, { ok: true }))

    await api.get('/accounts')

    const [, init] = fetchMock.mock.calls[0]
    expect((init?.headers as Headers).get('Authorization')).toBe(`Bearer ${auth.token}`)
  })

  it('sends a JSON body and extra headers on post', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(201, { id: '1' }))

    await api.post('/payments', { amount: 10 }, { 'Idempotency-Key': 'abc-123' })

    const [url, init] = fetchMock.mock.calls[0]
    expect(url).toBe('/api/payments')
    expect(init?.method).toBe('POST')
    expect(init?.body).toBe(JSON.stringify({ amount: 10 }))
    expect((init?.headers as Headers).get('Idempotency-Key')).toBe('abc-123')
  })

  it('throws HttpError with the parsed body when the response is not ok', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      jsonResponse(409, { status: 409, error: 'Conflict', message: 'Email already in use', timestamp: 'now' }),
    )

    await expect(api.post('/customers', {})).rejects.toMatchObject({
      status: 409,
      message: 'Email already in use',
    })
  })

  it('resolves to undefined for a 204 response without parsing a body', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }))

    await expect(api.get('/accounts')).resolves.toBeUndefined()
  })

  it('is an instance of HttpError so callers can narrow with instanceof', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(jsonResponse(500, { message: 'boom' }))

    try {
      await api.get('/accounts')
      throw new Error('expected api.get to reject')
    } catch (err) {
      expect(err).toBeInstanceOf(HttpError)
    }
  })
})
