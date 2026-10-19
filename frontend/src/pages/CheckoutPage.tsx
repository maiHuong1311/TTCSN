import { couponKeys } from '../hooks/useCoupons';
import { AvailableCoupons } from '../components/catalog/AvailableCoupons';
import { display } from '../lib/labels';
import { useState } from 'react';
import { productImageUrl } from '../lib/images';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useCart, useAddresses, shopKeys } from '../hooks/useShop';
import { createQuote, createOrder } from '../api/shop';
import type { CheckoutRequest } from '../types/domain';
import { AddressForm } from '../components/account/AddressForm';
import { Dialog } from '../components/ui/Dialog';
import { ErrorMessage, Loading, Empty } from '../components/ui/Feedback';
import { Button } from '../components/ui/Button';
import { form, str, money } from '../lib/format';
export function CheckoutPage() {
  const cart = useCart();
  const addresses = useAddresses();
  const [showAddress, setShowAddress] = useState(false);
  const [coupon, setCoupon] = useState('');
  const [couponDraft, setCouponDraft] = useState('');
  const [addressId, setAddressId] = useState<number>();
  const [method, setMethod] = useState('COD');
  const quote = useQuery({
    queryKey: [...shopKeys.cart(), 'quote', coupon, cart.data?.subtotal],
    queryFn: () => createQuote(coupon),
    enabled: !!cart.data?.items.length,
    retry: false,
  });
  const qc = useQueryClient();
  const navigate = useNavigate();
  const order = useMutation({
    mutationFn: (r: CheckoutRequest) => createOrder(r),
    onSuccess: (r) => {
      qc.invalidateQueries({ queryKey: couponKeys.all });
      qc.invalidateQueries({ queryKey: shopKeys.all });
      navigate('/orders/' + r.orderId);
    },
  });
  if (cart.isPending || addresses.isPending) return <Loading />;
  if (cart.isError || addresses.isError)
    return (
      <section className="section">
        <ErrorMessage error={cart.error || addresses.error} />
      </section>
    );
  if (!cart.data.items.length)
    return (
      <Empty title="Giỏ hàng đang trống">
        <Link to="/shop">Khám phá bộ sưu tập</Link>
      </Empty>
    );
  const selected =
    addressId ?? addresses.data.find((a) => a.isDefault)?.addressId ?? addresses.data[0]?.addressId;
  return (
    <section className="section">
      <div className="eyebrow">HOÀN TẤT ĐƠN HÀNG</div>
      <h1>Đặt hàng của bạn.</h1>
      <div className="checkout-layout">
        <form
          onSubmit={(e) => {
            const f = form(e);
            if (selected)
              order.mutate({
                addressId: selected,
                paymentMethod: method,
                couponCode: coupon,
                note: str(f, 'note'),
              });
          }}
        >
          <div className="step-head">
            <h2>
              <span>01</span> Địa chỉ giao hàng
            </h2>
            <button type="button" className="text-button" onClick={() => setShowAddress(true)}>
              + Thêm địa chỉ
            </button>
          </div>
          {addresses.data.length ? (
            addresses.data.map((a) => (
              <label
                className={'address-option ' + (selected === a.addressId ? 'selected' : '')}
                key={a.addressId}
              >
                <input
                  type="radio"
                  name="addressId"
                  checked={selected === a.addressId}
                  onChange={() => setAddressId(a.addressId)}
                />
                <span>
                  <strong>{a.receiverName}</strong> · {a.phoneNumber}
                  <small>
                    {a.detailAddress}, {a.province}
                  </small>
                </span>
                {a.isDefault && <small>Mặc định</small>}
              </label>
            ))
          ) : (
            <p className="subtle">Vui lòng thêm địa chỉ giao hàng để tiếp tục.</p>
          )}
          <div className="step-head">
            <h2>
              <span>02</span> Phương thức thanh toán
            </h2>
          </div>
          <div className="payment-options">
            {[
              ['COD', 'Thanh toán khi nhận hàng', 'Thanh toán trực tiếp khi nhận được hàng.'],
              ['DemoQR', 'QR giả lập', 'Quét mã QR để thử thanh toán — không chuyển tiền thật.'],
              ['VietQR', 'VietQR', 'Chờ xác nhận chuyển khoản từ cửa hàng.'],
              ['EWallet', 'Ví điện tử', 'Chờ xác nhận thanh toán từ ví điện tử.'],
            ].map(([value, label, detail]) => (
              <label className={method === value ? 'selected' : ''} key={value}>
                <input
                  type="radio"
                  name="paymentMethod"
                  value={value}
                  checked={method === value}
                  onChange={() => setMethod(value)}
                />
                <span>
                  <strong>{label}</strong>
                  <small>{detail}</small>
                </span>
              </label>
            ))}
          </div>
          {method === 'DemoQR' && (
            <p className="info-note">
              Thanh toán giả lập — không chuyển tiền thật. Sau khi đặt hàng, bạn có thể tạo mã QR và
              chọn kết quả trên trang demo.
            </p>
          )}
          {method !== 'COD' && method !== 'DemoQR' && (
            <p className="info-note">
              Phiên bản chạy cục bộ ghi nhận thanh toán ở trạng thái chờ xác nhận. Vui lòng liên hệ
              cửa hàng để được hướng dẫn; cổng thanh toán chưa được kết nối.
            </p>
          )}
          <label>
            Ghi chú cho cửa hàng
            <textarea
              name="note"
              placeholder="Ghi chú giao hàng, yêu cầu đặc biệt…"
              maxLength={2000}
            />
          </label>
          <ErrorMessage error={order.error} />
          <Button
            disabled={
              !selected || !quote.data || quote.isError || quote.isFetching || order.isPending
            }
          >
            {order.isPending
              ? 'Đang đặt hàng…'
              : 'Đặt hàng · ' + money(quote.data?.totalAmount || 0) + ' ↗'}
          </Button>
        </form>
        <aside className="panel order-summary">
          <h2>Sản phẩm đã chọn.</h2>
          {cart.data.items.map((i) => (
            <div className="summary-item" key={i.cartItemId}>
              <img src={productImageUrl(i.imageUrl, i.productId)} alt="" />
              <div>
                <strong>{i.productName}</strong>
                <small>
                  {i.size} / {display(i.color)} × {i.quantity}
                </small>
              </div>
              <span>{money(i.unitPrice * i.quantity)}</span>
            </div>
          ))}
          <div className="coupon-field">
            <input
              aria-label="Mã giảm giá"
              placeholder="Nhập mã giảm giá"
              value={couponDraft}
              onChange={(e) => setCouponDraft(e.target.value)}
            />
            <button
              className="text-button"
              type="button"
              onClick={() => setCoupon(couponDraft.trim())}
            >
              Áp dụng
            </button>
          </div>
          {coupon && (
            <button
              type="button"
              className="text-button"
              onClick={() => {
                setCoupon('');
                setCouponDraft('');
              }}
            >
              Xóa {coupon} ×
            </button>
          )}
          <AvailableCoupons
            subtotal={cart.data.subtotal}
            selectedCode={quote.isSuccess && !quote.isFetching ? coupon : undefined}
            onApply={(code) => { setCouponDraft(code); setCoupon(code); }}
          />
          <ErrorMessage error={quote.error} />
          {quote.data && (
            <>
              <div className="summary-row">
                <span>Tạm tính</span>
                <span>{money(quote.data.subtotal)}</span>
              </div>
              <div className="summary-row">
                <span>Phí vận chuyển</span>
                <span>{money(quote.data.shippingFee)}</span>
              </div>
              <div className="summary-row">
                <span>Giảm giá</span>
                <span>−{money(quote.data.discountAmount)}</span>
              </div>
              <div className="summary-row total">
                <strong>Tổng cộng</strong>
                <strong>{money(quote.data.totalAmount)}</strong>
              </div>
            </>
          )}
        </aside>
      </div>
      {showAddress && (
        <Dialog title="Thêm địa chỉ giao hàng" onClose={() => setShowAddress(false)}>
          <AddressForm onSaved={() => setShowAddress(false)} />
        </Dialog>
      )}
    </section>
  );
}
