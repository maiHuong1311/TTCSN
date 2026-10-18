import { useEffect } from 'react';
import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '../stores/authStore';
import { useUIStore } from '../stores/uiStore';

export function AdminLayout() {
  const auth = useAuthStore();
  const { notice, notify } = useUIStore();
  const qc = useQueryClient();
  const navigate = useNavigate();
  useEffect(() => {
    if (!notice) return;
    const id = window.setTimeout(() => notify(''), 4500);
    return () => window.clearTimeout(id);
  }, [notice, notify]);
  function signOut() {
    auth.logout();
    void qc.cancelQueries();
    qc.clear();
    notify('');
    navigate('/login', { replace: true });
  }
  return (
    <div className="admin-shell">
      <header className="admin-header">
        <Link className="wordmark" to="/admin" aria-label="FORME - Trang quản trị">
          FORME<span>TRANG QUẢN TRỊ</span>
        </Link>
        <div className="admin-header-actions">
          <span>
            {auth.user?.fullName}
            <small>Quản trị viên</small>
          </span>
          <button className="button outline" type="button" onClick={signOut}>
            Đăng xuất
          </button>
        </div>
      </header>
      <main>
        <Outlet />
      </main>
      <footer className="admin-footer">© 2026 FORME · Quản trị cửa hàng</footer>
      {notice && (
        <div className="toast" role="status">
          {notice}
          <button aria-label="Đóng thông báo" onClick={() => notify('')}>
            ×
          </button>
        </div>
      )}
    </div>
  );
}
