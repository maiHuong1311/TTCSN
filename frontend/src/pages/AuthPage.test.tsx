import { it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { AuthPage } from './AuthPage';
import * as api from '../api/auth';
import { useAuthStore } from '../stores/authStore';
it('submits login credentials and shows the server error', async () => {
  const login = vi.spyOn(api, 'login').mockRejectedValue(new Error('Invalid email or password'));
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { mutations: { retry: false } } })}
    >
      <MemoryRouter>
        <AuthPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('Email hoặc tên đăng nhập'), 'customer@example.com');
  await user.type(screen.getByLabelText('Mật khẩu'), 'WrongPassword123');
  await user.click(screen.getByRole('button', { name: 'Đăng nhập ↗' }));
  await waitFor(() =>
    expect(login).toHaveBeenCalledWith('customer@example.com', 'WrongPassword123'),
  );
  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Email, tên đăng nhập hoặc mật khẩu không chính xác.',
  );
  expect(useAuthStore.getState().user).toBeNull();
});

it('requests an email code and includes it when registering', async () => {
  const send = vi.spyOn(api, 'sendEmailCode').mockResolvedValue({
    message: 'Đã gửi mã',
    expiresAt: new Date(Date.now() + 600000).toISOString(),
    resendAt: new Date(Date.now() + 60000).toISOString(),
  });
  const register = vi
    .spyOn(api, 'register')
    .mockRejectedValue(new Error('Mã xác minh không chính xác.'));
  render(
    <QueryClientProvider
      client={new QueryClient({ defaultOptions: { mutations: { retry: false } } })}
    >
      <MemoryRouter>
        <AuthPage />
      </MemoryRouter>
    </QueryClientProvider>,
  );
  const user = userEvent.setup();
  await user.click(screen.getByRole('button', { name: 'Đăng ký tài khoản' }));
  await user.type(screen.getByLabelText('Họ và tên'), 'Nguyễn An');
  await user.type(screen.getByLabelText('Tên đăng nhập'), 'nguyen_an');
  await user.type(screen.getByLabelText(/Số điện thoại/), '0912345678');
  await user.type(screen.getByLabelText('Địa chỉ email'), 'an@gmail.com');
  await user.click(screen.getByRole('button', { name: 'Gửi mã xác minh email' }));
  await waitFor(() => expect(send).toHaveBeenCalledWith('an@gmail.com', expect.anything()));
  expect(await screen.findByRole('status')).toHaveTextContent('an@gmail.com');
  await user.type(screen.getByLabelText('Mã xác minh email'), '123456');
  await user.type(screen.getByLabelText('Mật khẩu'), 'TestPassword123');
  await user.click(screen.getByRole('button', { name: 'Tạo tài khoản ↗' }));
  await waitFor(() =>
    expect(register).toHaveBeenCalledWith(
      expect.objectContaining({
        email: 'an@gmail.com',
        verificationCode: '123456',
        phoneNumber: '0912345678',
      }),
    ),
  );
  expect(await screen.findByRole('alert')).toHaveTextContent('Mã xác minh không chính xác.');
});
