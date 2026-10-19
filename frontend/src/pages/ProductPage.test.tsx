import { beforeEach, afterEach, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProductPage } from './ProductPage';
import { colorProduct } from '../test/catalogFixture';
import { useUIStore } from '../stores/uiStore';
import * as shop from '../api/shop';
import * as catalog from '../api/catalog';
const reviewState = vi.hoisted(() => ({
  canReview: false,
  deliveredPurchase: false,
  alreadyReviewed: false,
}));
beforeEach(() => {
  Object.assign(reviewState, {
    canReview: false,
    deliveredPurchase: false,
    alreadyReviewed: false,
  });
});
afterEach(() => vi.restoreAllMocks());
vi.mock('../hooks/useCatalog', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../hooks/useCatalog')>()),
  useProduct: () => ({ data: colorProduct, isPending: false, isError: false }),
  useRecommendations: () => ({ data: [], isPending: false, isError: false }),
  useReviews: () => ({ data: [], isPending: false, isError: false }),
  useReviewEligibility: () => ({ data: reviewState, isPending: false, isError: false }),
}));
function page(query = '') {
  useUIStore.setState({ tracking: false });
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { mutations: { retry: false } } })}
    >
      <MemoryRouter initialEntries={['/products/1' + query]}>
        <Routes>
          <Route path="/products/:id" element={<ProductPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

it('changes photo, retains available size and submits the selected color variant', async () => {
  const add = vi
    .spyOn(shop, 'addCartItem')
    .mockResolvedValue({ cartId: 1, items: [], subtotal: 0 });
  page();
  expect(screen.getByAltText('Áo mẫu nhiều màu — Trắng')).toHaveAttribute(
    'src',
    '/assets/white.jpg',
  );
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: 'M' }));
  await user.click(screen.getByRole('button', { name: 'Đen' }));
  expect(screen.getByAltText('Áo mẫu nhiều màu — Đen')).toHaveAttribute('src', '/assets/black.jpg');
  expect(screen.getByRole('button', { name: 'M' })).toHaveAttribute('aria-pressed', 'true');
  expect(screen.getByLabelText('Số lượng')).toHaveAttribute('max', '2');
  await user.click(screen.getByRole('button', { name: 'Thêm vào giỏ ↗' }));
  await waitFor(() => expect(add).toHaveBeenCalledWith(3, 1));
});

it('allows viewing a sold-out color without permitting purchase', async () => {
  page();
  await userEvent.setup().click(screen.getByRole('button', { name: /Be/ }));
  expect(screen.getByAltText('Áo mẫu nhiều màu — Be')).toHaveAttribute('src', '/assets/beige.jpg');
  expect(screen.getByRole('button', { name: 'L' })).toBeDisabled();
  expect(screen.getByRole('button', { name: 'Thêm vào giỏ ↗' })).toBeDisabled();
});

it('opens the color selected from a catalog card', () => {
  page('?color=' + encodeURIComponent('Đen'));
  expect(screen.getByAltText('Áo mẫu nhiều màu — Đen')).toHaveAttribute('src', '/assets/black.jpg');
  expect(screen.getByRole('button', { name: 'Đen' })).toHaveAttribute('aria-pressed', 'true');
});

it('hides the review form when the account has no delivered purchase', () => {
  page();
  expect(
    screen.getByText(
      'Bạn chỉ có thể đánh giá sản phẩm đã mua sau khi đơn hàng được giao thành công.',
    ),
  ).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Gửi đánh giá' })).not.toBeInTheDocument();
  expect(screen.queryByLabelText('Nội dung đánh giá')).not.toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Theo dõi đơn hàng →' })).toHaveAttribute(
    'href',
    '/orders',
  );
});

it('allows the delivered buyer to submit a review', async () => {
  Object.assign(reviewState, { canReview: true, deliveredPurchase: true });
  const create = vi
    .spyOn(catalog, 'createReview')
    .mockResolvedValue({
      reviewId: 1,
      userId: 1,
      productId: 1,
      fullName: 'Khách hàng',
      rating: 5,
      comment: 'Sản phẩm đẹp',
      imageUrl: null,
      createdAt: '2026-10-18T00:00:00Z',
    });
  page();
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('Nội dung đánh giá'), 'Sản phẩm đẹp');
  await user.click(screen.getByRole('button', { name: 'Gửi đánh giá' }));
  await waitFor(() =>
    expect(create).toHaveBeenCalledWith(1, { rating: 5, comment: 'Sản phẩm đẹp', imageUrl: null }),
  );
});

it('does not show another review form after the buyer has already reviewed', () => {
  Object.assign(reviewState, { deliveredPurchase: true, alreadyReviewed: true });
  page();
  expect(screen.getByText(/Bạn đã đánh giá sản phẩm này/)).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Gửi đánh giá' })).not.toBeInTheDocument();
});
