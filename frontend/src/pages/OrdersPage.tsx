import { Link, useParams } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useOrders, useOrder, shopKeys } from '../hooks/useShop';
import { cancelOrder } from '../api/shop';
import { ErrorMessage, Loading, Empty } from '../components/ui/Feedback';
import { OrderDetails } from '../components/orders/OrderDetails';
import { DemoPaymentQr } from '../components/orders/DemoPaymentQr';
import { Badge } from '../components/ui/Badge';
import { money, date } from '../lib/format';
export function OrdersPage() {
  const q = useOrders();
  return (
    <section className="section">
      <div className="eyebrow">TỪ CỬA HÀNG ĐẾN TAY BẠN</div>
      <h1>Đơn hàng của bạn.</h1>
      {q.isPending ? (
        <Loading />
      ) : q.isError ? (
        <ErrorMessage error={q.error} />
      ) : !q.data.length ? (
        <Empty title="Hành trình mua sắm bắt đầu từ đây.">
          <Link className="button" to="/shop">
            Khám phá sản phẩm đầu tiên ↗
          </Link>
        </Empty>
      ) : (
        <div className="order-list">
          {q.data.map((o) => (
            <Link to={'/orders/' + o.orderId} key={o.orderId} className="panel order-card">
              <div>
                <h3>Đơn hàng #{o.orderId}</h3>
                <small>{date(o.createdAt)}</small>
                <p>{o.items.map((i) => i.productName).join(', ')}</p>
              </div>
              <Badge>{o.orderStatus}</Badge>
              <strong>{money(o.totalAmount)}</strong>
              <span>Xem đơn hàng ↗</span>
            </Link>
          ))}
        </div>
      )}
    </section>
  );
}
export function OrderPage() {
  const id = Number(useParams().id);
  const q = useOrder(id);
  const qc = useQueryClient();
  const cancel = useMutation({
    mutationFn: () => cancelOrder(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: shopKeys.orders() }),
  });
  return (
    <section className="section narrow">
      <Link className="text-button" to="/orders">
        ← Danh sách đơn hàng
      </Link>
      {q.isPending ? (
        <Loading />
      ) : q.isError ? (
        <ErrorMessage error={q.error} />
      ) : (
        <>
          <OrderDetails order={q.data} />
          {q.data.payment.paymentMethod === 'DemoQR' &&
            ['Pending', 'Failed'].includes(q.data.payment.paymentStatus) &&
            ['Pending', 'Confirmed'].includes(q.data.orderStatus) && <DemoPaymentQr orderId={id} />}
          <div className="row no-print">
            <button className="button outline" onClick={() => window.print()}>
              In hóa đơn
            </button>
            {q.data.orderStatus === 'Pending' && (
              <button
                className="text-button danger"
                disabled={cancel.isPending}
                onClick={() => {
                  if (
                    window.confirm(
                      'Bạn muốn hủy đơn hàng này và hoàn lại số lượng đã giữ trong kho?',
                    )
                  )
                    cancel.mutate();
                }}
              >
                Hủy đơn hàng
              </button>
            )}
          </div>
          <ErrorMessage error={cancel.error} />
        </>
      )}
    </section>
  );
}
