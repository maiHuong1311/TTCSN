import { request, body } from './client';
import type { AuthMessageResponse, ResetPasswordRequest } from '../types/domain';
import type { VerifyPasswordOtpRequest, PasswordOtpVerificationResponse } from '../types/domain';
export const verifyPasswordOtp = (r: VerifyPasswordOtpRequest) =>
  request<PasswordOtpVerificationResponse>('/auth/reset-password/verify', {
    method: 'POST',
    body: body(r),
  });
export const requestPasswordReset = (email: string) =>
  request<AuthMessageResponse>('/auth/forgot-password', { method: 'POST', body: body({ email }) });
export const resetPassword = (r: ResetPasswordRequest) =>
  request<AuthMessageResponse>('/auth/reset-password', { method: 'POST', body: body(r) });
import type {
  AuthResponse,
  RegisterRequest,
  User,
  ProfileRequest,
  EmailCodeResponse,
} from '../types/domain';
export const sendEmailCode = (email: string) =>
  request<EmailCodeResponse>('/auth/register/email-code', {
    method: 'POST',
    body: body({ email }),
  });
export const login = (email: string, password: string) =>
  request<AuthResponse>('/auth/login', {
    method: 'POST',
    body: body({ identifier: email, password }),
  });
export const register = (r: RegisterRequest) =>
  request<AuthResponse>('/auth/register', { method: 'POST', body: body(r) });
export const fetchProfile = () => request<User>('/auth/me');
export const updateProfile = (r: ProfileRequest) =>
  request<User>('/auth/me', { method: 'PUT', body: body(r) });
