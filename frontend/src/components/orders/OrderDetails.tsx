import { display } from '../../lib/labels';
import { Link } from 'react-router-dom';
import type { Order } from '../../types/domain';
import { money, date } from '../../lib/format';
import { Badge } from '../ui/Badge';
export function OrderDetails({
  order: o,
  productLinks = true,
}: {
  order: Order;
  productLinks?: boolean;
}) {
  return (
    <article className="order-details">
      <div className="order-heading">
        <div>
          <h2>Đơn hàng #{o.orderId}</h2>
          <small>{date(o.createdAt)}</small>
        </div>
        <Badge>{o.orderStatus}</Badge>
      </div>
      <div className="order-progress">
        {['Pending', 'Confirmed', 'Shipping', 'Delivered'].map((s, i) => (
          <span
            key={display(s)}
            className={
              o.orderStatus !== 'Cancelled' &&
              i <= ['Pending', 'Confirmed', 'Shipping', 'Delivered'].indexOf(o.orderStatus)
                ? 'done'
                : ''
            }
          >
            <i>{i + 1}</i>
            {display(s)}
          </span>
        ))}
      </div>
      <div className="address-display">
        <div>
          <small>THÔNG TIN GIAO HÀNG</small>
          <strong>{o.receiverName}</strong>
          <p>
            {o.receiverPhone}
            <br />
            {o.shippingAddress}
          </p>
        </div>
        <div>
          <small>THANH TOÁN</small>
          <strong>
            {display(o.payment.paymentMethod)} · {display(o.payment.paymentStatus)}
          </strong>
          {o.payment.transactionId && <p>Mã giao dịch: {o.payment.transactionId}</p>}
          {o.payment.paymentMethod === 'DemoQR' && (
            <p className="info-note">Thanh toán giả lập — không chuyển tiền thật.</p>
          )}
          {o.payment.paymentMethod !== 'COD' &&
            o.payment.paymentMethod !== 'DemoQR' &&
            o.payment.paymentStatus === 'Pending' && (
              <p className="subtle">
                Liên hệ cửa hàng để được hướng dẫn thanh toán.
                <br />
                Thanh toán đang chờ xác nhận.
              </p>
            )}
        </div>
      </div>
      {o.items.map((i) => (
        <div className="order-line" key={i.orderItemId}>
          {productLinks ? (
            <Link to={'/products/' + i.productId}>
              <strong>{i.productName}</strong>
              <small>
                {i.size} / {display(i.color)} × {i.quantity}
              </small>
            </Link>
          ) : (
            <div>
              <strong>{i.productName}</strong>
              <small>
                {i.size} / {display(i.color)} × {i.quantity}
              </small>
            </div>
          )}
          <span>{money(i.unitPrice * i.quantity)}</span>
        </div>
      ))}
      <div className="order-totals">
        <div className="summary-row">
          <span>Phí vận chuyển</span>
          <span>{money(o.shippingFee)}</span>
        </div>
        <div className="summary-row">
          <span>Giảm giá</span>
          <span>−{money(o.discountAmount)}</span>
        </div>
        <div className="summary-row total">
          <strong>Tổng tiền thanh toán</strong>
          <strong>{money(o.totalAmount)}</strong>
        </div>
      </div>
      {o.note && <p className="info-note">Ghi chú: {o.note}</p>}
    </article>
  );
}
