import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { completeDemoPayment } from '../api/shop';
import { useDemoPayment, shopKeys } from '../hooks/useShop';
import { ErrorMessage, Loading } from '../components/ui/Feedback';
import { money, date } from '../lib/format';
import { display } from '../lib/labels';

export function DemoPaymentPage() {
  const [token] = useState(() => window.location.hash.slice(1));
  const valid = /^[A-Za-z0-9_-]{43}$/.test(token);
  const query = useDemoPayment(token);
  const qc = useQueryClient();
  const complete = useMutation({
    mutationFn: (outcome: 'Paid' | 'Failed') => completeDemoPayment(token, outcome),
    onSuccess: (result) => {
      qc.setQueryData(shopKeys.demoPayment(token), result);
      qc.invalidateQueries({ queryKey: shopKeys.orders() });
    },
  });
  const payment = query.data;
  const open =
    payment &&
    ['Pending', 'Confirmed'].includes(payment.orderStatus) &&
    payment.paymentStatus === 'Pending' &&
    Date.parse(payment.expiresAt) > Date.now();
  return (
    <section className="section narrow demo-payment">
      <div className="eyebrow">FORME · DEMO</div>
      <h1>Thanh toán giả lập</h1>
      <p className="info-note">
        Không chuyển tiền thật. Không nhập thông tin ngân hàng hoặc ví điện tử. Các nút dưới đây chỉ
        mô phỏng kết quả thanh toán.
      </p>
      {!valid ? (
        <p role="alert">Liên kết thanh toán không hợp lệ. Vui lòng tạo mã QR từ đơn hàng.</p>
      ) : query.isPending ? (
        <Loading />
      ) : (
        <>
          <ErrorMessage error={query.error} />
          {payment && (
            <div className="panel">
              <h2>Đơn hàng #{payment.orderId}</h2>
              <h3>{money(payment.amount)}</h3>
              <p role="status">
                Thanh toán: {display(payment.paymentStatus)} · Đơn hàng:{' '}
                {display(payment.orderStatus)}
              </p>
              <p>Hiệu lực đến: {date(payment.expiresAt)}</p>
              {open && !query.isError ? (
                <div className="row">
                  <button
                    className="button"
                    disabled={complete.isPending}
                    onClick={() => complete.mutate('Paid')}
                  >
                    Giả lập thành công
                  </button>
                  <button
                    className="button outline"
                    disabled={complete.isPending}
                    onClick={() => complete.mutate('Failed')}
                  >
                    Giả lập thất bại
                  </button>
                </div>
              ) : (
                <p>
                  Phiên thanh toán đã kết thúc. Bạn có thể quay lại đơn hàng để xem kết quả hoặc tạo
                  mã mới nếu cần.
                </p>
              )}
              <ErrorMessage error={complete.error} />
              <Link className="text-button" to={'/orders/' + payment.orderId}>
                Về đơn hàng →
              </Link>
            </div>
          )}
        </>
      )}
    </section>
  );
}
