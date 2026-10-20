import { it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { HomePage } from './HomePage';
import { useAuthStore } from '../stores/authStore';
it('shows a public photographic lookbook and color previews without requesting protected catalog APIs', async () => {
  useAuthStore.setState({ token: null, user: null });
  const fetch = vi.spyOn(globalThis, 'fetch');
  render(
    <MemoryRouter>
      <HomePage />
    </MemoryRouter>,
  );
  expect(screen.getAllByAltText('Áo kiểu cổ nhún buộc dây dài tay')[0]).toHaveAttribute(
    'src',
    '/assets/photos/product-1.jpg',
  );
  expect(screen.getAllByText(/Đăng nhập để xem giá/).length).toBeGreaterThan(0);
  expect(fetch).not.toHaveBeenCalled();
  const saleGrid = document.querySelector('#uu-dai .product-grid');
  expect(saleGrid).toHaveClass('three-products');
  expect(saleGrid?.children).toHaveLength(3);
  await userEvent.setup().click(screen.getAllByRole('button', { name: 'Xem màu Hồng đỗ' })[0]);
  expect(screen.getAllByAltText('Áo kiểu cổ nhún buộc dây dài tay')[0]).toHaveAttribute(
    'src',
    '/assets/photos/product-1-color-2.jpg',
  );
  expect(fetch).not.toHaveBeenCalled();
  fetch.mockRestore();
});
