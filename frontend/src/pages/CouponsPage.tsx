import { Link } from 'react-router-dom';
import { AvailableCoupons } from '../components/catalog/AvailableCoupons';
import { useAuthStore } from '../stores/authStore';

export function CouponsPage() {
  const token = useAuthStore((s) => s.token);
  return (
    <section className="section coupons-page">
      <div className="eyebrow">ƯU ĐÃI TỪ FORME</div>
      <h1>Mã giảm giá</h1>
      <p>Chọn ưu đãi cho đơn hàng tiếp theo của bạn.</p>
      <p className="subtle">
        Danh sách hiển thị mã đang có hiệu lực, còn lượt sử dụng và bạn chưa dùng.
        Sao chép mã rồi áp dụng tại bước thanh toán.
      </p>
      {!token && <p><Link to="/login">Đăng nhập</Link> để xem các mã dành cho tài khoản của bạn.</p>}
      <AvailableCoupons showRefresh />
      <Link className="button" to="/shop">Mua sắm ngay →</Link>
    </section>
  );
}
