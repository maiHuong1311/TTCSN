import { afterEach, expect, it, vi } from 'vitest';
import { act, renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useAvailableCoupons } from './useCoupons';
import { useAuthStore } from '../stores/authStore';
import * as api from '../api/coupons';
import type { User } from '../types/domain';

const user: User = { userId: 1, email: 'one@test.local', username: 'one', fullName: 'One',
  phoneNumber: null, role: 'Customer', status: 'Active', createdAt: '2026-01-01T00:00:00Z',
  heightCm: null, weightKg: null, preferredStyle: null };
afterEach(() => { vi.restoreAllMocks(); useAuthStore.setState({ user: null, token: null }); });

it('fetches a separate coupon list when the signed-in customer changes', async () => {
  const fetch = vi.spyOn(api, 'fetchAvailableCoupons')
    .mockResolvedValueOnce([])
    .mockResolvedValueOnce([{ couponId: 1, couponCode: 'FIRST', discountType: 'Fixed',
      discountValue: 10000, minOrderValue: 0, usageLimit: 100,
      startDate: '2020-01-01T00:00:00Z', endDate: '2099-01-01T00:00:00Z' }]);
  useAuthStore.setState({ token: 'one', user });
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const { result } = renderHook(useAvailableCoupons, { wrapper: ({ children }) =>
    <QueryClientProvider client={client}>{children}</QueryClientProvider> });
  await waitFor(() => expect(result.current.isSuccess).toBe(true));
  expect(result.current.data).toEqual([]);
  act(() => useAuthStore.setState({ token: 'two', user: { ...user, userId: 2 } }));
  await waitFor(() => expect(result.current.data?.[0]?.couponCode).toBe('FIRST'));
  expect(fetch).toHaveBeenCalledTimes(2);
});
