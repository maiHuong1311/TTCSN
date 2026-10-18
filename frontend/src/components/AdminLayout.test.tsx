import { afterEach, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, useLocation, useRoutes } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { appRoutes } from '../router';
import { useAuthStore } from '../stores/authStore';
import * as authApi from '../api/auth';
import type { User } from '../types/domain';

vi.mock('../pages/AdminPage', () => ({ AdminPage: () => <h1>Quản lý cửa hàng.</h1> }));
vi.mock('../pages/ShopPage', () => ({ ShopPage: () => <h1>Cửa hàng dành cho khách</h1> }));
vi.mock('../hooks/useShop', async (original) => ({
  ...(await original<typeof import('../hooks/useShop')>()),
  useCart: () => ({ data: { items: [] } }),
}));
const admin: User = {
  userId: 1,
  fullName: 'Quản trị FORME',
  username: 'admin',
  email: 'admin@forme.local',
  phoneNumber: null,
  role: 'Admin',
  status: 'Active',
  createdAt: '2026-01-01T00:00:00Z',
  heightCm: null,
  weightKg: null,
  preferredStyle: null,
};
function session(user: User) {
  useAuthStore.setState({ token: 'session-token', user, expiresAt: '2099-01-01T00:00:00Z' });
}
function mount(path: string, state?: { from: string }) {
  const location = { pathname: path };
  function TestRoutes() {
    location.pathname = useLocation().pathname;
    return useRoutes(appRoutes);
  }
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[{ pathname: path, state }]}><TestRoutes /></MemoryRouter>
    </QueryClientProvider>,
  );
  return { location, client };
}
afterEach(() => {
  useAuthStore.getState().logout();
  vi.restoreAllMocks();
});

it('redirects an admin away from shopping into a separate management layout', async () => {
  session(admin);
  const { location, client } = mount('/cart');
  expect(await screen.findByRole('heading', { name: 'Quản lý cửa hàng.' })).toBeInTheDocument();
  expect(location.pathname).toBe('/admin');
  expect(screen.queryByRole('search')).not.toBeInTheDocument();
  expect(
    screen.queryByRole('link', { name: /Giỏ hàng|TÀI KHOẢN|Mở trợ lý tư vấn|ÁO NỮ/i }),
  ).not.toBeInTheDocument();
  expect(screen.queryByText('GIAO HÀNG TOÀN QUỐC')).not.toBeInTheDocument();
  client.setQueryData(['private-admin-data'], { revenue: 1 });
  await userEvent.click(screen.getByRole('button', { name: 'Đăng xuất' }));
  await waitFor(() => expect(location.pathname).toBe('/login'));
  expect(useAuthStore.getState().token).toBeNull();
  expect(client.getQueryData(['private-admin-data'])).toBeUndefined();
});

it('does not grant customers access to the management area', async () => {
  session({ ...admin, role: 'Customer' });
  const { location } = mount('/admin');
  expect(
    await screen.findByRole('heading', { name: 'Cửa hàng dành cho khách' }),
  ).toBeInTheDocument();
  expect(location.pathname).toBe('/shop');
  expect(screen.queryByRole('heading', { name: 'Quản lý cửa hàng.' })).not.toBeInTheDocument();
});

it('sends admin login to management even when a shopping return path was requested', async () => {
  useAuthStore.getState().logout();
  vi.spyOn(authApi, 'login').mockResolvedValue({
    accessToken: 'admin-token',
    tokenType: 'Bearer',
    expiresAt: '2099-01-01T00:00:00Z',
    user: admin,
  });
  const { location } = mount('/login', { from: '/checkout' });
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('Email hoặc tên đăng nhập'), 'admin@forme.local');
  await user.type(screen.getByLabelText('Mật khẩu'), 'AdminPassword123');
  await user.click(screen.getByRole('button', { name: 'Đăng nhập ↗' }));
  expect(await screen.findByRole('heading', { name: 'Quản lý cửa hàng.' })).toBeInTheDocument();
  expect(location.pathname).toBe('/admin');
});
