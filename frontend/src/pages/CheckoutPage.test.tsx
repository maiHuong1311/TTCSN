import { it, expect, vi, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import * as api from '../api/shop';
import { CheckoutPage } from './CheckoutPage';
import { CartPage } from './CartPage';
import type { Cart } from '../types/domain';
vi.mock('../api/coupons', () => ({ fetchAvailableCoupons: async () => [] }));
const cart: Cart = {
  cartId: 1,
  subtotal: 390000,
  items: [
    {
      cartItemId: 1,
      variantId: 1,
      productId: 1,
      productName: 'Linen shirt',
      size: 'M',
      color: 'Sand',
      quantity: 1,
      stockQuantity: 1,
      unitPrice: 390000,
      imageUrl: '/assets/photos/product-1.jpg',
    },
  ],
};
function mount(ui: React.ReactNode) {
  return render(
    <QueryClientProvider
      client={
        new QueryClient({
          defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
        })
      }
    >
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  );
}
afterEach(() => vi.restoreAllMocks());
it('prevents increasing a cart item beyond available stock', async () => {
  vi.spyOn(api, 'fetchCart').mockResolvedValue(cart);
  mount(<CartPage />);
  expect(await screen.findByRole('button', { name: 'Tăng số lượng Linen shirt' })).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Giảm số lượng Linen shirt' })).toBeDisabled();
});
it('blocks checkout when an applied coupon is invalid', async () => {
  vi.spyOn(api, 'fetchCart').mockResolvedValue(cart);
  vi.spyOn(api, 'fetchAddresses').mockResolvedValue([
    {
      addressId: 5,
      receiverName: 'Customer',
      phoneNumber: '0901234567',
      province: 'Ha Noi',
      detailAddress: '123 Street',
      isDefault: true,
    },
  ]);
  vi.spyOn(api, 'createQuote').mockImplementation(async (code) => {
    if (code === 'BAD') throw new Error('Coupon not found');
    return { subtotal: 390000, shippingFee: 30000, discountAmount: 0, totalAmount: 420000 };
  });
  const order = vi.spyOn(api, 'createOrder');
  mount(<CheckoutPage />);
  await waitFor(() => expect(screen.getByRole('button', { name: /Đặt hàng/ })).toBeEnabled());
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('Mã giảm giá'), 'BAD');
  await user.click(screen.getByRole('button', { name: 'Áp dụng' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Không tìm thấy mã giảm giá.');
  expect(screen.getByRole('button', { name: /Đặt hàng/ })).toBeDisabled();
  expect(order).not.toHaveBeenCalled();
});
