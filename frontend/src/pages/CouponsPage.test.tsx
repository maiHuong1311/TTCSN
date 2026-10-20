import { afterEach, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useRoutes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { appRoutes } from '../router';
import { useAuthStore } from '../stores/authStore';
import * as api from '../api/coupons';
import type { Coupon } from '../types/domain';

const coupon: Coupon = { couponId: 2, couponCode: 'SUMMERSALES', discountType: 'Percentage',
  discountValue: 20, minOrderValue: 100000, usageLimit: 100,
  startDate: '2020-01-01T00:00:00Z', endDate: '2099-12-01T08:45:00Z' };
function Routes() { return useRoutes(appRoutes); }
function mount(path: string) {
  useAuthStore.setState({ token: null, user: null, expiresAt: null });
  return render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
    <MemoryRouter initialEntries={[path]}><Routes /></MemoryRouter>
  </QueryClientProvider>);
}
afterEach(() => vi.restoreAllMocks());

it('opens the public coupon page from the main navigation and shows server coupons', async () => {
  vi.spyOn(api, 'fetchAvailableCoupons').mockResolvedValue([coupon]);
  mount('/');
  const nav = screen.getByRole('navigation', { name: 'Điều hướng chính' });
  const link = within(nav).getByRole('link', { name: 'MÃ GIẢM GIÁ' });
  expect(link).toHaveAttribute('href', '/coupons');
  await userEvent.setup().click(link);
  expect(await screen.findByRole('heading', { level: 1, name: 'Mã giảm giá' })).toBeInTheDocument();
  expect(await screen.findByText('SUMMERSALES')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Sao chép mã SUMMERSALES' })).toBeInTheDocument();
});

it('supports a direct visit and manual refresh after an admin adds a coupon', async () => {
  const fetch = vi.spyOn(api, 'fetchAvailableCoupons').mockResolvedValueOnce([]).mockResolvedValue([coupon]);
  mount('/coupons');
  expect(await screen.findByText('Hiện chưa có mã giảm giá còn hiệu lực.')).toBeInTheDocument();
  await userEvent.setup().click(screen.getByRole('button', { name: 'Làm mới danh sách' }));
  expect(await screen.findByText('SUMMERSALES')).toBeInTheDocument();
  expect(fetch).toHaveBeenCalledTimes(2);
});
