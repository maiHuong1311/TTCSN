import { useEffect, useState } from 'react';
import { NavLink, Link, Navigate, Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '../stores/authStore';
import { useUIStore } from '../stores/uiStore';
import { useCart } from '../hooks/useShop';
import { Icon } from './ui/Icon';
const navigation = [
  { label: 'HÀNG MỚI VỀ', to: '/shop', end: true },
  { label: 'ÁO NỮ', to: '/shop?categoryId=1' },
  { label: 'VÁY - ĐẦM', to: '/shop?categoryId=3' },
  { label: 'QUẦN NỮ', to: '/shop?categoryId=2' },
  { label: 'PHỤ KIỆN', to: '/shop?categoryId=4' },
  { label: 'ƯU ĐÃI', to: '/#uu-dai', sale: true },
];
export function Layout() {
  const [menu, setMenu] = useState(false);
  const [search, setSearch] = useState('');
  const auth = useAuthStore();
  const { notice, notify } = useUIStore();
  const navigate = useNavigate();
  const location = useLocation();
  const qc = useQueryClient();
  useEffect(() => {
    if (!location.hash) return;
    const frame = requestAnimationFrame(() => {
      const target = document.getElementById(decodeURIComponent(location.hash.slice(1)));
      target?.scrollIntoView({
        behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
      });
    });
    return () => cancelAnimationFrame(frame);
  }, [location.hash, location.key]);
  function signOut() {
    auth.logout();
    void qc.cancelQueries();
    qc.clear();
    setMenu(false);
    navigate('/');
  }
  useEffect(() => {
    if (!notice) return;
    const id = setTimeout(() => notify(''), 4500);
    return () => clearTimeout(id);
  }, [notice, notify]);
  if (auth.token && auth.user?.role === 'Admin' && (!auth.expiresAt || Date.parse(auth.expiresAt) > Date.now()))
    return <Navigate to="/admin" replace />;
  return (
    <>
      <div className="announcement">
        <span>CHÀO BẠN ĐẾN VỚI FORME</span>
        <Link to="/shop">
          Ưu đãi 10% với mã WELCOME10 · Đơn từ 300.000₫ <b>→</b>
        </Link>
        <span>THANH TOÁN KHI NHẬN HÀNG</span>
      </div>
      <header className="site-header">
        <div className="header-main">
          <button
            className="icon-button mobile-menu"
            aria-label="Mở hoặc đóng menu"
            aria-expanded={menu}
            onClick={() => setMenu(!menu)}
          >
            <Icon name="menu" />
          </button>
          <Link className="wordmark" to="/" aria-label="FORME - Trang chủ">
            FORME<span>THỜI TRANG CHO BẠN</span>
          </Link>
          <form
            className="header-search"
            role="search"
            onSubmit={(e) => {
              e.preventDefault();
              navigate('/shop' + (search.trim() ? '?q=' + encodeURIComponent(search.trim()) : ''));
              setMenu(false);
            }}
          >
            <input
              aria-label="Tìm kiếm sản phẩm"
              placeholder="Bạn đang tìm kiếm sản phẩm gì?"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <button aria-label="Tìm kiếm" type="submit">
              <Icon name="search" size={21} />
            </button>
          </form>
          <div className="header-actions">
            {auth.token && (
              <button type="button" className="header-logout" onClick={signOut}>
                Đăng xuất
              </button>
            )}
            <Link className="header-service" to="/advisor">
              <Icon name="spark" />
              <span>
                TRỢ LÝ<small>Tư vấn phong cách</small>
              </span>
            </Link>
            <Link className="header-account" to={auth.token ? '/account' : '/login'}>
              <Icon name="user" />
              <span>
                TÀI KHOẢN<small>{auth.token ? 'Thông tin của bạn' : 'Đăng nhập / Đăng ký'}</small>
              </span>
            </Link>
            {auth.token ? (
              <CartLink />
            ) : (
              <Link className="header-bag" to="/cart" aria-label="Giỏ hàng">
                <Icon name="bag" />
                <span>
                  GIỎ HÀNG<small>Sản phẩm của bạn</small>
                </span>
              </Link>
            )}
          </div>
        </div>
        <nav
          className={'store-navigation ' + (menu ? 'open' : '')}
          aria-label="Điều hướng chính"
          onClick={() => setMenu(false)}
        >
          <div className="navigation-inner">
            <Link className="home-nav-link" to="/" aria-label="Trang chủ">
              <Icon name="home" size={18} />
            </Link>
            {navigation.map((n) =>
              n.sale ? (
                <Link key={n.label} className="nav-sale" to={n.to}>
                  {n.label} <span>HOT</span>
                </Link>
              ) : (
                <NavLink
                  key={n.label}
                  className={({ isActive }) => (n.end && isActive ? 'nav-link active' : 'nav-link')}
                  to={n.to}
                  end={n.end}
                >
                  {n.label}
                </NavLink>
              ),
            )}
            <Link className="nav-link" to="/orders">
              ĐƠN HÀNG
            </Link>
          </div>
        </nav>
      </header>
      <main>
        <Outlet />
      </main>
      <section className="footer-service-strip" aria-label="Dịch vụ FORME">
        <div>
          <strong>GIAO HÀNG TOÀN QUỐC</strong>
          <small>Miễn phí cho đơn từ 1.000.000₫</small>
        </div>
        <div>
          <strong>ĐỔI TRẢ TRONG 7 NGÀY</strong>
          <small>Theo chính sách của cửa hàng</small>
        </div>
        <div>
          <strong>MUA SẮM AN TÂM</strong>
          <small>Thông tin đơn hàng rõ ràng</small>
        </div>
        <div>
          <strong>TƯ VẤN PHONG CÁCH</strong>
          <small>Chọn trang phục phù hợp với bạn</small>
        </div>
      </section>
      <footer className="store-footer">
        <div>
          <Link className="wordmark" to="/">
            FORME
          </Link>
          <p>
            Thời trang là cách bạn kể câu chuyện của mình.
            <br />
            Cùng FORME tìm những thiết kế bạn yêu thích.
          </p>
          <div className="social-links">
            <Link to="/shop">Bộ sưu tập</Link>
            <Link to="/advisor">Trợ lý phong cách</Link>
          </div>
        </div>
        <div>
          <strong>HỖ TRỢ KHÁCH HÀNG</strong>
          <Link to="/orders">Tra cứu đơn hàng</Link>
          <Link to="/advisor">Chính sách giao hàng</Link>
          <Link to="/advisor">Chính sách đổi trả</Link>
          <Link to="/account">Quản lý tài khoản</Link>
        </div>
        <div>
          <strong>KHÁM PHÁ FORME</strong>
          <Link to="/shop?categoryId=1">Áo nữ</Link>
          <Link to="/shop?categoryId=3">Váy & đầm</Link>
          <Link to="/shop?categoryId=2">Quần nữ</Link>
          <Link to="/shop?categoryId=4">Phụ kiện</Link>
        </div>
        <div className="footer-inspiration">
          <strong>PHONG CÁCH CỦA BẠN</strong>
          <p>Cảm hứng mới luôn bắt đầu từ một lựa chọn nhỏ.</p>
          <Link className="button outline" to="/shop">
            KHÁM PHÁ NGAY →
          </Link>
          {auth.token && (
            <button className="text-button" onClick={signOut}>
              Đăng xuất tài khoản
            </button>
          )}
        </div>
        <div className="footer-bottom">
          <span>© 2026 FORME. Thời trang cho phong cách của bạn.</span>
          <span>Thanh toán: COD · VietQR · Ví điện tử</span>
        </div>
      </footer>
      <Link className="floating-advisor" to="/advisor" aria-label="Mở trợ lý tư vấn">
        <Icon name="spark" size={23} />
        <span>Bạn cần tư vấn?</span>
      </Link>
      {notice && (
        <div className="toast" role="status">
          {notice}
          <button aria-label="Đóng thông báo" onClick={() => notify('')}>
            ×
          </button>
        </div>
      )}
    </>
  );
}
function CartLink() {
  const cart = useCart();
  const count = cart.data?.items.reduce((n, i) => n + i.quantity, 0) || 0;
  return (
    <Link className="header-bag" to="/cart" aria-label={'Giỏ hàng, ' + count + ' sản phẩm'}>
      <span className="bag-symbol">
        <Icon name="bag" size={23} />
        <b>{count}</b>
      </span>
      <span>
        GIỎ HÀNG<small>{count} sản phẩm</small>
      </span>
    </Link>
  );
}
