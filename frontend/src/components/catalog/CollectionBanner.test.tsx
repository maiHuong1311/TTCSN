import { it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { CollectionBanner } from './CollectionBanner';
it('switches real-photo banners and keeps the matching collection link', async () => {
  render(
    <MemoryRouter>
      <CollectionBanner />
    </MemoryRouter>,
  );
  expect(screen.getByAltText('Người mẫu mặc áo cổ nhún xanh pastel')).toHaveAttribute(
    'src',
    '/assets/photos/product-1.jpg',
  );
  await userEvent.click(screen.getByRole('button', { name: 'Banner tiếp theo' }));
  expect(screen.getByAltText('Người mẫu mặc đầm sơ mi kẻ sọc thanh lịch')).toHaveAttribute(
    'src',
    '/assets/photos/product-3.jpg',
  );
  expect(screen.getByRole('link', { name: /XEM VÁY & ĐẦM/ })).toHaveAttribute(
    'href',
    '/shop?categoryId=3',
  );
  await userEvent.click(screen.getByRole('button', { name: 'Banner trước' }));
  expect(screen.getByRole('button', { name: 'Xem banner 1' })).toHaveAttribute(
    'aria-current',
    'true',
  );
});
