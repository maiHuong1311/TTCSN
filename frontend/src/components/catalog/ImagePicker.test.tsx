import { useState } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, expect, it, vi } from 'vitest';
import * as api from '../../api/admin';
import { ImagePicker } from './ImagePicker';

afterEach(() => vi.restoreAllMocks());
function Picker() {
  const [images, setImages] = useState(['/assets/old.jpg']);
  return (
    <ImagePicker
      label="Ảnh sản phẩm"
      images={images}
      onChange={setImages}
      multiple
      onBusyChange={() => {}}
    />
  );
}
it('uploads selected files, preserves existing images and allows removing an image', async () => {
  const upload = vi
    .spyOn(api, 'uploadProductImage')
    .mockResolvedValue({ imageUrl: '/api/images/new.png' });
  render(<Picker />);
  const file = new File(['png'], 'photo.png', { type: 'image/png' });
  await userEvent.upload(screen.getByLabelText('Ảnh sản phẩm'), file);
  await waitFor(() =>
    expect(screen.getByAltText('Ảnh sản phẩm 2')).toHaveAttribute('src', '/api/images/new.png'),
  );
  expect(upload).toHaveBeenCalledWith(file);
  await userEvent.click(screen.getByRole('button', { name: 'Xóa ảnh 1' }));
  expect(screen.getAllByRole('img')).toHaveLength(1);
});
it('shows upload failures without discarding existing images', async () => {
  vi.spyOn(api, 'uploadProductImage').mockRejectedValue(
    new Error('Không thể lưu ảnh. Vui lòng thử lại.'),
  );
  render(<Picker />);
  await userEvent.upload(
    screen.getByLabelText('Ảnh sản phẩm'),
    new File(['png'], 'photo.png', { type: 'image/png' }),
  );
  expect(await screen.findByRole('alert')).toHaveTextContent('Không thể lưu ảnh');
  expect(screen.getByRole('img')).toHaveAttribute('src', '/assets/old.jpg');
});
