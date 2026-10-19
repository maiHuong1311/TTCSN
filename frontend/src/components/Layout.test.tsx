import { expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { Layout } from './Layout';
import { useAuthStore } from '../stores/authStore';
vi.mock('../hooks/useShop', () => ({ useCart: () => ({ data: { items: [] } }) }));

it('provides a visible header logout button that clears session and private cached data', async () => {
  useAuthStore.setState({ token: 'session-token', user: null, expiresAt: '2030-01-01T00:00:00Z' });
  const qc = new QueryClient();
  qc.setQueryData(['private-test-data'], { receiverName: 'Khách hàng' });
  render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <Layout />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  const logout = screen.getByRole('button', { name: 'Đăng xuất' });
  expect(logout.closest('header')).toBeTruthy();
  await userEvent.setup().click(logout);
  await waitFor(() => expect(useAuthStore.getState().token).toBeNull());
  expect(qc.getQueryData(['private-test-data'])).toBeUndefined();
  expect(screen.queryByRole('button', { name: 'Đăng xuất' })).not.toBeInTheDocument();
  expect(screen.getByRole('link', { name: /TÀI KHOẢN/ })).toHaveAttribute('href', '/login');
});
