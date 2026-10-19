import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest';
import { request, ApiError } from './client';
import { useAuthStore } from '../stores/authStore';
beforeEach(() => useAuthStore.setState({ token: 'test-token' }));
afterEach(() => vi.unstubAllGlobals());
describe('API client', () => {
  it('lets the browser set multipart boundaries while retaining authentication', async () => {
    const fetch = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify({ imageUrl: '/api/images/test.png' })));
    vi.stubGlobal('fetch', fetch);
    const data = new FormData();
    data.append('file', new File(['image'], 'photo.png', { type: 'image/png' }));
    await request('/admin/product-images', { method: 'POST', body: data });
    expect(fetch.mock.calls[0][1].body).toBe(data);
    expect(fetch.mock.calls[0][1].headers).not.toHaveProperty('Content-Type');
    expect(fetch.mock.calls[0][1].headers.Authorization).toBe('Bearer test-token');
  });
  it('sends the Bearer token and handles an empty response', async () => {
    const fetch = vi.fn().mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal('fetch', fetch);
    await request('/cart/items/1', { method: 'DELETE' });
    expect(fetch.mock.calls[0][1].headers.Authorization).toBe('Bearer test-token');
  });
  it('logs out on 401 and exposes field validation details', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ detail: 'Expired', errors: { email: 'Invalid' } }), {
          status: 401,
          headers: { 'Content-Type': 'application/problem+json' },
        }),
      ),
    );
    try {
      await request('/auth/me');
      expect.fail('Should reject');
    } catch (e) {
      expect(e).toBeInstanceOf(ApiError);
      expect((e as ApiError).errors.email).toBe('Invalid');
    }
    expect(useAuthStore.getState().token).toBeNull();
  });
});
