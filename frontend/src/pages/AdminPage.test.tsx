import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { expect, it, vi } from 'vitest';
import * as api from '../api/admin';
import { AdminPage } from './AdminPage';
import { colorProduct } from '../test/catalogFixture';
import type { ReactNode } from 'react';

vi.mock('../components/ui/Dialog', () => ({
  Dialog: ({ children }: { children: ReactNode }) => <div role="dialog">{children}</div>,
}));

vi.mock('../hooks/useAdmin', async (original) => ({
  ...(await original<typeof import('../hooks/useAdmin')>()),
  useReport: () => ({ data: { revenue: 0, orders: 0, pendingOrders: 0, bestSellers: [] } }),
  useBehavior: () => ({ data: [] }),
  useAdminProducts: () => ({ data: { content: [], totalPages: 0 } }),
}));
vi.mock('../hooks/useCatalog', async (original) => ({
  ...(await original<typeof import('../hooks/useCatalog')>()),
  useCategories: () => ({ data: [{ categoryId: 1, categoryName: 'Áo nữ' }] }),
}));

it('saves uploaded gallery and color photos without asking for image links', async () => {
  const imageUrl = '/api/images/00000000-0000-0000-0000-000000000000.png';
  vi.spyOn(api, 'uploadProductImage').mockResolvedValue({ imageUrl });
  const save = vi.spyOn(api, 'saveProduct').mockResolvedValue(colorProduct);
  render(<QueryClientProvider client={new QueryClient()}><AdminPage /></QueryClientProvider>);
  const user = userEvent.setup();
  await user.click(screen.getByRole('tab', { name: 'Sản phẩm' }));
  await user.click(screen.getByRole('button', { name: '+ Thêm sản phẩm' }));
  await user.type(screen.getByLabelText('Tên sản phẩm'), 'Áo mới');
  await user.selectOptions(screen.getByLabelText('Danh mục'), '1');
  await user.type(screen.getByLabelText('Giá gốc (VNĐ)'), '150000');
  await user.type(screen.getByLabelText('SKU 1'), 'UPLOAD-TEST');
  const file = new File(['png'], 'photo.png', { type: 'image/png' });
  await user.upload(screen.getByLabelText('Ảnh sản phẩm'), file);
  await screen.findByAltText('Ảnh sản phẩm 1');
  await user.upload(screen.getByLabelText('Ảnh biến thể 1'), file);
  await screen.findByAltText('Ảnh biến thể 1 1');
  expect(screen.queryByPlaceholderText(/https:/)).not.toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: 'Lưu sản phẩm' }));
  await waitFor(() => expect(save).toHaveBeenCalledWith(expect.objectContaining({
    images: [imageUrl], variants: [expect.objectContaining({ imageUrl, size: '' })],
  }), undefined));
});
