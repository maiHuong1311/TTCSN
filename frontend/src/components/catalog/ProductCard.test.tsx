import { expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { ProductCard } from './ProductCard';
import { colorProduct } from '../../test/catalogFixture';

it('previews a color and carries that selection into the product link', async () => {
  render(
    <MemoryRouter>
      <ProductCard product={colorProduct} />
    </MemoryRouter>,
  );
  await userEvent.setup().click(screen.getByRole('button', { name: 'Xem màu Đen' }));
  expect(screen.getByAltText(colorProduct.productName)).toHaveAttribute('src', '/assets/black.jpg');
  expect(screen.getByRole('button', { name: 'Xem màu Đen' })).toHaveAttribute(
    'aria-pressed',
    'true',
  );
  for (const link of screen.getAllByRole('link'))
    expect(link).toHaveAttribute('href', '/products/1?color=' + encodeURIComponent('Đen'));
});
