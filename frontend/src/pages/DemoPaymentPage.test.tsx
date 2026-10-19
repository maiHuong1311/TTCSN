import { afterEach, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import * as api from '../api/shop';
import { DemoPaymentPage } from './DemoPaymentPage';
import { DemoPaymentQr } from '../components/orders/DemoPaymentQr';
import type { DemoPayment } from '../types/domain';

const token = 'a'.repeat(43);
const payment: DemoPayment = {
  orderId: 15,
  amount: 420000,
  paymentStatus: 'Pending',
  orderStatus: 'Pending',
  expiresAt: '2099-01-01T00:00:00Z',
  token: null,
};
function mount(element: React.ReactNode) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>{element}</MemoryRouter>
    </QueryClientProvider>,
  );
}
afterEach(() => {
  vi.restoreAllMocks();
  window.location.hash = '';
});

it('allows a QR visitor to complete a demo without login and removes outcome buttons afterwards', async () => {
  window.location.hash = token;
  vi.spyOn(api, 'fetchDemoPayment').mockResolvedValue(payment);
  const complete = vi
    .spyOn(api, 'completeDemoPayment')
    .mockResolvedValue({ ...payment, paymentStatus: 'Paid' });
  mount(<DemoPaymentPage />);
  expect(screen.getByText(/Không chuyển tiền thật/)).toBeInTheDocument();
  await userEvent.click(await screen.findByRole('button', { name: 'Giả lập thành công' }));
  await waitFor(() => expect(screen.getByRole('status')).toHaveTextContent('Đã thanh toán'));
  expect(complete).toHaveBeenCalledWith(token, 'Paid');
  expect(screen.queryByRole('button', { name: 'Giả lập thất bại' })).not.toBeInTheDocument();
});

it('rejects a missing capability locally and never sends a payment request', () => {
  const fetch = vi.spyOn(api, 'fetchDemoPayment');
  mount(<DemoPaymentPage />);
  expect(screen.getByRole('alert')).toHaveTextContent('Liên kết thanh toán không hợp lệ');
  expect(fetch).not.toHaveBeenCalled();
});

it('creates a local QR image containing the current site payment link', async () => {
  vi.spyOn(api, 'createDemoPayment').mockResolvedValue({ ...payment, token });
  mount(<DemoPaymentQr orderId={15} />);
  await userEvent.click(screen.getByRole('button', { name: 'Tạo mã QR thanh toán' }));
  const link = await screen.findByRole('link', {
    name: 'Mở trang thanh toán demo ↗',
  });
  expect(link).toHaveAttribute('href', `${window.location.origin}/demo-payment#${token}`);
  expect(
    screen.getByAltText('Mã QR thanh toán giả lập cho đơn hàng 15').getAttribute('src'),
  ).toMatch(/^data:image\/png;base64,/);
});
