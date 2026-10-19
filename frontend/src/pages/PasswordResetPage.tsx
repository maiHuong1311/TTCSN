import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import * as api from '../api/auth';
import { Button } from '../components/ui/Button';
import { ErrorMessage } from '../components/ui/Feedback';
import { form, str } from '../lib/format';
import { useAuthStore } from '../stores/authStore';

export function PasswordResetPage() {
  const location = useLocation();
  const [email, setEmail] = useState('');
  const [otp, setOtp] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [resendAt, setResendAt] = useState(0);
  const [now, setNow] = useState(Date.now());
  const qc = useQueryClient();
  const logout = useAuthStore((s) => s.logout);
  useEffect(() => {
    if (location.hash) window.history.replaceState(window.history.state, '', location.pathname);
  }, [location.hash, location.pathname]);
  useEffect(() => {
    if (!resendAt) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [resendAt]);
  const seconds = Math.max(0, Math.ceil((resendAt - now) / 1000));
  const forgot = useMutation({
    mutationFn: api.requestPasswordReset,
    onSuccess: (_, address) => {
      setEmail(address.trim().toLowerCase());
      setOtp('');
      verify.reset();
      setResendAt(Date.now() + 60000);
      setNow(Date.now());
    },
  });
  const verify = useMutation({ mutationFn: api.verifyPasswordOtp });
  const reset = useMutation({
    mutationFn: api.resetPassword,
    onSuccess: () => {
      verify.reset();
      setOtp('');
      logout();
      void qc.cancelQueries();
      qc.clear();
    },
  });
  const step = reset.isSuccess ? 'done' : verify.isSuccess ? 'password' : email ? 'otp' : 'email';
  return (
    <section className="password-reset-page">
      <div className="eyebrow">TÀI KHOẢN FORME</div>
      <h1>
        {step === 'password'
          ? 'Đặt mật khẩu mới'
          : step === 'done'
            ? 'Đổi mật khẩu thành công'
            : 'Bạn quên mật khẩu?'}
      </h1>
      {step !== 'done' && (
        <ol className="password-reset-steps" aria-label="Các bước đổi mật khẩu">
          {['Email', 'Xác minh OTP', 'Mật khẩu mới'].map((label, i) => (
            <li
              key={label}
              aria-current={
                i === (step === 'email' ? 0 : step === 'otp' ? 1 : 2) ? 'step' : undefined
              }
            >
              {i + 1}. {label}
            </li>
          ))}
        </ol>
      )}
      {step === 'done' ? (
        <div role="status" className="password-reset-success">
          <p>{reset.data?.message}</p>
          <Link className="button" to="/login">
            Đăng nhập với mật khẩu mới
          </Link>
        </div>
      ) : step === 'email' ? (
        <>
          <p className="subtle">Nhập email đã đăng ký để nhận mã OTP đặt lại mật khẩu.</p>
          <form onSubmit={(e) => forgot.mutate(str(form(e), 'email').trim().toLowerCase())}>
            <label>
              Email đã đăng ký
              <input name="email" type="email" autoComplete="email" required maxLength={255} />
            </label>
            <ErrorMessage error={forgot.error} />
            <Button disabled={forgot.isPending}>
              {forgot.isPending ? 'Đang gửi mã…' : 'Gửi mã OTP'}
            </Button>
          </form>
        </>
      ) : step === 'otp' ? (
        <>
          <p role="status" className="password-reset-success">
            {forgot.data?.message}
          </p>
          <p className="subtle">
            Nhập mã 6 số được gửi đến <strong>{email}</strong>. Mã có hiệu lực 10 phút, tối đa 5 lần
            nhập sai.
          </p>
          <form
            onSubmit={(e) => {
              e.preventDefault();
              verify.mutate({ email, otp });
            }}
          >
            <label>
              Mã OTP
              <input
                name="otp"
                inputMode="numeric"
                autoComplete="one-time-code"
                pattern="[0-9]{6}"
                minLength={6}
                maxLength={6}
                required
                value={otp}
                onChange={(e) => setOtp(e.target.value)}
                placeholder="Nhập mã 6 số"
              />
            </label>
            <ErrorMessage error={verify.error} />
            <Button disabled={verify.isPending || forgot.isPending}>
              {verify.isPending ? 'Đang xác minh…' : 'Xác minh OTP'}
            </Button>
          </form>
          <ErrorMessage error={forgot.error} />
          <button
            type="button"
            className="text-button"
            disabled={seconds > 0 || forgot.isPending || verify.isPending}
            onClick={() => forgot.mutate(email)}
          >
            {forgot.isPending
              ? 'Đang gửi mã…'
              : seconds > 0
                ? `Gửi lại mã sau ${seconds}s`
                : 'Gửi lại mã OTP'}
          </button>
        </>
      ) : (
        <>
          <p className="subtle">
            Email đã được xác minh. Nhập và xác nhận mật khẩu mới trong 5 phút.
          </p>
          <form
            onSubmit={(e) => {
              const data = form(e);
              if (str(data, 'password') !== str(data, 'confirmPassword')) {
                setPasswordError('Mật khẩu xác nhận chưa trùng khớp.');
                return;
              }
              setPasswordError('');
              reset.mutate({ email, token: verify.data!.token, password: str(data, 'password') });
            }}
          >
            <label>
              Mật khẩu mới
              <input
                name="password"
                type="password"
                autoComplete="new-password"
                minLength={8}
                maxLength={72}
                required
              />
            </label>
            <label>
              Xác nhận mật khẩu mới
              <input
                name="confirmPassword"
                type="password"
                autoComplete="new-password"
                minLength={8}
                maxLength={72}
                required
                onChange={() => setPasswordError('')}
              />
            </label>
            {passwordError && (
              <p className="error" role="alert">
                {passwordError}
              </p>
            )}
            <ErrorMessage error={reset.error} />
            <Button disabled={reset.isPending}>
              {reset.isPending ? 'Đang cập nhật…' : 'Lưu mật khẩu mới'}
            </Button>
          </form>
        </>
      )}
      {step !== 'email' && step !== 'done' && (
        <button
          type="button"
          className="text-button"
          disabled={forgot.isPending || verify.isPending || reset.isPending}
          onClick={() => {
            setEmail('');
            setOtp('');
            forgot.reset();
            verify.reset();
            reset.reset();
            setPasswordError('');
            setResendAt(0);
          }}
        >
          Nhập lại email / yêu cầu mã mới
        </button>
      )}
      <Link className="back-to-login" to="/login">
        ← Quay lại đăng nhập
      </Link>
    </section>
  );
}
