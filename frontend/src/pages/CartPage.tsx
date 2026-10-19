import { display } from '../lib/labels';
import { Link } from 'react-router-dom';
import { productImageUrl } from '../lib/images';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useCart, shopKeys } from '../hooks/useShop';
import * as api from '../api/shop';
import { money } from '../lib/format';
import { ErrorMessage, Loading, Empty } from '../components/ui/Feedback';
export function CartPage() {
  const q = useCart();
  const qc = useQueryClient();
  const update = useMutation({
    mutationFn: ({ id, quantity }: { id: number; quantity: number }) =>
      api.updateCartItem(id, quantity),
    onSuccess: (r) => qc.setQueryData(shopKeys.cart(), r),
  });
  const remove = useMutation({
    mutationFn: api.deleteCartItem,
    onSuccess: (r) => qc.setQueryData(shopKeys.cart(), r),
  });
  if (q.isPending) return <Loading />;
  if (q.isError)
    return (
      <section className="section">
        <ErrorMessage error={q.error} />
      </section>
    );
  return (
    <section className="section">
      <div className="eyebrow">NHỮNG LỰA CHỌN CỦA BẠN</div>
      <h1>Giỏ hàng của bạn.</h1>
      {!q.data.items.length ? (
        <Empty title="Sẵn sàng cho lựa chọn mới.">
          <p>Giỏ hàng của bạn chưa có sản phẩm nào.</p>
          <Link className="button" to="/shop">
            Khám phá bộ sưu tập ↗
          </Link>
        </Empty>
      ) : (
        <div className="checkout-layout">
          <div>
            <ErrorMessage error={update.error || remove.error} />
            {q.data.items.map((i) => (
              <article className="cart-item" key={i.cartItemId}>
                <Link to={'/products/' + i.productId}>
                  <img src={productImageUrl(i.imageUrl,i.productId)} alt={i.productName} />
                </Link>
                <div>
                  <Link to={'/products/' + i.productId}>
                    <h3>{i.productName}</h3>
                  </Link>
                  <p className="subtle">
                    {display(i.color)} / {i.size}
                  </p>
                  <strong>{money(i.unitPrice)}</strong>
                  <div className="quantity">
                    <button
                      aria-label={'Giảm số lượng ' + i.productName}
                      disabled={i.quantity <= 1 || update.isPending}
                      onClick={() => update.mutate({ id: i.variantId, quantity: i.quantity - 1 })}
                    >
                      −
                    </button>
                    <span>{i.quantity}</span>
                    <button
                      aria-label={'Tăng số lượng ' + i.productName}
                      disabled={i.quantity >= Math.min(i.stockQuantity, 100) || update.isPending}
                      onClick={() => update.mutate({ id: i.variantId, quantity: i.quantity + 1 })}
                    >
                      +
                    </button>
                    <button
                      className="text-button"
                      disabled={remove.isPending}
                      onClick={() => remove.mutate(i.cartItemId)}
                    >
                      Xóa
                    </button>
                  </div>
                  {i.quantity > i.stockQuantity && (
                    <p className="error">
                      Tồn kho đã thay đổi. Vui lòng giảm số lượng hoặc xóa sản phẩm này.
                    </p>
                  )}
                </div>
                <strong className="line-total">{money(i.unitPrice * i.quantity)}</strong>
              </article>
            ))}
            <Link className="text-button" to="/shop">
              ← Tiếp tục mua sắm
            </Link>
          </div>
          <aside className="panel order-summary">
            <h2>Thông tin giỏ hàng.</h2>
            <div className="summary-row">
              <span>Tạm tính</span>
              <strong>{money(q.data.subtotal)}</strong>
            </div>
            <p className="subtle">Phí vận chuyển và giảm giá sẽ được tính khi thanh toán.</p>
            <Link className="button full" to="/checkout">
              Tiến hành thanh toán ↗
            </Link>
            <p className="summary-note">
              Miễn phí vận chuyển cho đơn từ 1.000.000₫
              <br />
              Đơn dưới mức này có phí vận chuyển 30.000₫.
            </p>
          </aside>
        </div>
      )}
    </section>
  );
}
