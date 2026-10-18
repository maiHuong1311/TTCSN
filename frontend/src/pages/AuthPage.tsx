import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import * as api from '../api/auth';
import { useAuthStore } from '../stores/authStore';
import { ErrorMessage } from '../components/ui/Feedback';
import { form, str } from '../lib/format';
import { Button } from '../components/ui/Button';
import { vietnamPhonePattern } from '../lib/validation';
export function AuthPage() {
  const [signup, setSignup] = useState(false);
  const [email, setEmail] = useState('');
  const [sentEmail, setSentEmail] = useState('');
  const [resendAt, setResendAt] = useState(0);
  const [now, setNow] = useState(Date.now());
  const emailInput = useRef<HTMLInputElement>(null);
  useEffect(() => {
    if (!resendAt) return;
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, [resendAt]);
  const seconds = Math.max(0, Math.ceil((resendAt - now) / 1000));
  const emailMutation = useMutation({
    mutationFn: api.sendEmailCode,
    onSuccess: (r, address) => {
      setSentEmail(address);
      setResendAt(Date.parse(r.resendAt));
      setNow(Date.now());
    },
  });
  const navigate = useNavigate();
  const location = useLocation();
  const qc = useQueryClient();
  const auth = useAuthStore();
  const mutation = useMutation({
    mutationFn: (f: FormData) =>
      signup
        ? api.register({
            email: str(f, 'email'),
            password: str(f, 'password'),
            fullName: str(f, 'fullName'),
            username: str(f, 'username'),
            phoneNumber: str(f, 'phoneNumber'),
            verificationCode: str(f, 'verificationCode'),
          })
        : api.login(str(f, 'email'), str(f, 'password')),
    onSuccess: (r) => {
      qc.clear();
      auth.login(r);
      navigate(
        r.user.role === 'Admin'
          ? '/admin'
          : location.state?.from === '/admin'
            ? '/shop'
            : location.state?.from || '/shop',
        { replace: true },
      );
    },
  });
  return (
    <div className="auth-layout">
      <div className="auth-art">
        <img src="/assets/photos/product-1.jpg" alt="Tủ đồ tinh tế cho mỗi ngày" />
        <div>
          <span className="eyebrow">PHONG CÁCH FORME</span>
          <h2>
            Trang phục đẹp.
            <br />
            Câu chuyện riêng.
          </h2>
        </div>
      </div>
      <section className="auth-form">
        <div className="eyebrow">CHÀO MỪNG ĐẾN VỚI FORME</div>
        <h1>{signup ? 'Khởi đầu phong cách của bạn.' : 'Rất vui được gặp lại bạn.'}</h1>
        <p className="subtle">
          {signup
            ? 'Tạo tài khoản để tìm những thiết kế bạn yêu thích.'
            : 'Đăng nhập để mua sắm, lưu giỏ hàng và theo dõi đơn hàng.'}
        </p>
        <form onSubmit={(e) => mutation.mutate(form(e))} key={String(signup)}>
          {signup && (
            <>
              <label>
                Họ và tên
                <input name="fullName" autoComplete="name" maxLength={100} required />
              </label>
              <label>
                Tên đăng nhập
                <input
                  name="username"
                  autoComplete="username"
                  pattern="[a-zA-Z0-9_]{3,50}"
                  title="Từ 3–50 chữ cái không dấu, chữ số hoặc dấu gạch dưới"
                  required
                />
              </label>
              <label>
                Số điện thoại
                <input
                  name="phoneNumber"
                  type="tel"
                  autoComplete="tel"
                  pattern={vietnamPhonePattern}
                  required
                />
                <small>Ví dụ: 0912345678 hoặc +84912345678</small>
              </label>
            </>
          )}
          <label>
            {signup ? 'Địa chỉ email' : 'Email hoặc tên đăng nhập'}
            <input
              name="email"
              ref={emailInput}
              value={email}
              onChange={(e) => {
                setEmail(e.target.value);
                emailMutation.reset();
              }}
              type={signup ? 'email' : 'text'}
              autoComplete={signup ? 'email' : 'username'}
              required
              maxLength={255}
            />
          </label>
          {signup && (
            <div className="email-verification">
              <Button
                type="button"
                className="outline"
                disabled={emailMutation.isPending || seconds > 0}
                onClick={() => {
                  if (emailInput.current?.reportValidity())
                    emailMutation.mutate(email.trim().toLowerCase());
                }}
              >
                {emailMutation.isPending
                  ? 'Đang gửi mã…'
                  : seconds > 0
                    ? `Gửi lại sau ${seconds}s`
                    : sentEmail
                      ? 'Gửi lại mã xác minh'
                      : 'Gửi mã xác minh email'}
              </Button>
              <ErrorMessage error={emailMutation.error} />
              {sentEmail === email.trim().toLowerCase() && (
                <p role="status">
                  Mã đã gửi đến {sentEmail}. Có hiệu lực 10 phút. Vui lòng kiểm tra cả thư rác.
                </p>
              )}
              <label>
                Mã xác minh email
                <input
                  name="verificationCode"
                  inputMode="numeric"
                  autoComplete="one-time-code"
                  pattern="[0-9]{6}"
                  minLength={6}
                  maxLength={6}
                  required
                  placeholder="Nhập mã 6 số trong email"
                />
              </label>
            </div>
          )}
          <label>
            Mật khẩu
            <input
              name="password"
              type="password"
              autoComplete={signup ? 'new-password' : 'current-password'}
              minLength={8}
              maxLength={72}
              required
            />
          </label>
          <ErrorMessage error={mutation.error} />
          <Button disabled={mutation.isPending}>
            {mutation.isPending ? 'Vui lòng chờ…' : signup ? 'Tạo tài khoản ↗' : 'Đăng nhập ↗'}
          </Button>
        </form>
        {!signup && (
          <Link className="forgot-password-link" to="/forgot-password">
            Quên mật khẩu?
          </Link>
        )}
        <p>
          {signup ? 'Bạn đã có tài khoản?' : 'Bạn chưa có tài khoản?'}{' '}
          <button
            className="text-button"
            onClick={() => {
              setSignup(!signup);
              mutation.reset();
              emailMutation.reset();
            }}
          >
            {signup ? 'Đăng nhập' : 'Đăng ký tài khoản'}
          </button>
        </p>
        <Link className="subtle" to="/">
          ← Về trang khám phá
        </Link>
      </section>
    </div>
  );
}
