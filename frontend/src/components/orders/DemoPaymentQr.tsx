import { useMutation, useQueryClient } from '@tanstack/react-query';
import QRCode from 'qrcode';
import { createDemoPayment } from '../../api/shop';
import { shopKeys } from '../../hooks/useShop';
import { ErrorMessage } from '../ui/Feedback';
import { money, date } from '../../lib/format';

export function DemoPaymentQr({ orderId }: { orderId: number }) {
  const qc = useQueryClient();
  const session = useMutation({
    mutationFn: async () => {
      const payment = await createDemoPayment(orderId);
      const url = new URL('/demo-payment', window.location.origin);
      url.hash = payment.token!;
      const qr = await QRCode.toDataURL(url.href, {
        width: 300,
        margin: 4,
        errorCorrectionLevel: 'M',
      });
      return { ...payment, url: url.href, qr };
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: shopKeys.order(orderId) }),
  });
  return (
    <aside className="panel demo-payment no-print">
      <div className="eyebrow">TRẢI NGHIỆM THANH TOÁN</div>
      <h2>Thanh toán bằng QR giả lập</h2>
      <p className="info-note">
        Thanh toán giả lập — không chuyển tiền thật. Mã QR mở trang demo để bạn chọn kết quả.
      </p>
      {session.data && (
        <div className="demo-qr-content">
          <img
            src={session.data.qr}
            width="300"
            height="300"
            alt={`Mã QR thanh toán giả lập cho đơn hàng ${orderId}`}
          />
          <div>
            <h3>{money(session.data.amount)}</h3>
            <p>Mã có hiệu lực đến {date(session.data.expiresAt)}. Tạo mã mới sẽ vô hiệu mã cũ.</p>
            <a className="button" href={session.data.url} target="_blank" rel="noreferrer">
              Mở trang thanh toán demo ↗
            </a>
            <p className="subtle">
              Để quét bằng điện thoại, mở website bằng địa chỉ IP máy tính trong cùng Wi-Fi rồi tạo
              mã QR.
            </p>
          </div>
        </div>
      )}
      <button
        className="button outline"
        type="button"
        disabled={session.isPending}
        onClick={() => session.mutate()}
      >
        {session.isPending
          ? 'Đang tạo mã QR…'
          : session.data
            ? 'Tạo mã QR mới'
            : 'Tạo mã QR thanh toán'}
      </button>
      <ErrorMessage error={session.error} />
    </aside>
  );
}
