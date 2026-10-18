import { afterEach, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PasswordResetPage } from './PasswordResetPage';
import * as api from '../api/auth';
import { useAuthStore } from '../stores/authStore';
afterEach(() => vi.restoreAllMocks());
function page(path = '/forgot-password') {
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { mutations: { retry: false } } })}
    >
      <MemoryRouter initialEntries={[path]}>
        <PasswordResetPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
}
function mockRequest() {
  return vi
    .spyOn(api, 'requestPasswordReset')
    .mockResolvedValue({
      message: 'Nếu email thuộc tài khoản đang hoạt động, bạn sẽ nhận được mã OTP.',
    });
}
async function requestCode(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Email đã đăng ký'), 'khach@gmail.com');
  await user.click(screen.getByRole('button', { name: 'Gửi mã OTP' }));
  await screen.findByLabelText('Mã OTP');
}
it('requests OTP and requires verification before displaying password fields', async () => {
  const request = mockRequest();
  page();
  const user = userEvent.setup();
  await requestCode(user);
  expect(request).toHaveBeenCalledWith('khach@gmail.com', expect.anything());
  expect(screen.queryByLabelText('Mật khẩu mới')).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Gửi lại mã sau/ })).toBeDisabled();
  expect(screen.getByRole('status')).toHaveTextContent('mã OTP');
});
it('verifies OTP before changing password and rejects mismatched confirmation', async () => {
  mockRequest();
  const token = 'A'.repeat(43);
  const verify = vi
    .spyOn(api, 'verifyPasswordOtp')
    .mockResolvedValue({ token, expiresInSeconds: 300 });
  const reset = vi
    .spyOn(api, 'resetPassword')
    .mockResolvedValue({ message: 'Đã đặt lại mật khẩu.' });
  useAuthStore.setState({ token: 'old-session', user: null });
  page();
  const user = userEvent.setup();
  await requestCode(user);
  await user.type(screen.getByLabelText('Mã OTP'), '123456');
  await user.click(screen.getByRole('button', { name: 'Xác minh OTP' }));
  await screen.findByLabelText('Mật khẩu mới');
  expect(verify).toHaveBeenCalledWith(
    { email: 'khach@gmail.com', otp: '123456' },
    expect.anything(),
  );
  await user.type(screen.getByLabelText('Mật khẩu mới'), 'NewPassword123');
  await user.type(screen.getByLabelText('Xác nhận mật khẩu mới'), 'OtherPassword123');
  await user.click(screen.getByRole('button', { name: 'Lưu mật khẩu mới' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('chưa trùng khớp');
  expect(reset).not.toHaveBeenCalled();
  await user.clear(screen.getByLabelText('Xác nhận mật khẩu mới'));
  await user.type(screen.getByLabelText('Xác nhận mật khẩu mới'), 'NewPassword123');
  await user.click(screen.getByRole('button', { name: 'Lưu mật khẩu mới' }));
  await waitFor(() =>
    expect(reset).toHaveBeenCalledWith(
      { email: 'khach@gmail.com', token, password: 'NewPassword123' },
      expect.anything(),
    ),
  );
  expect(await screen.findByRole('status')).toHaveTextContent('Đã đặt lại mật khẩu.');
  expect(useAuthStore.getState().token).toBeNull();
  expect(screen.getByRole('link', { name: 'Đăng nhập với mật khẩu mới' })).toHaveAttribute(
    'href',
    '/login',
  );
}, 10000);
it('stays on OTP entry when server rejects the code', async () => {
  mockRequest();
  vi.spyOn(api, 'verifyPasswordOtp').mockRejectedValue(
    new Error('Mã OTP không chính xác hoặc đã hết hạn.'),
  );
  page();
  const user = userEvent.setup();
  await requestCode(user);
  await user.type(screen.getByLabelText('Mã OTP'), '000000');
  await user.click(screen.getByRole('button', { name: 'Xác minh OTP' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Mã OTP không chính xác');
  expect(screen.queryByLabelText('Mật khẩu mới')).not.toBeInTheDocument();
});
it('does not accept an old reset link as OTP verification', () => {
  page('/reset-password#token=' + 'A'.repeat(43));
  expect(screen.getByLabelText('Email đã đăng ký')).toBeInTheDocument();
  expect(screen.queryByLabelText('Mật khẩu mới')).not.toBeInTheDocument();
});
