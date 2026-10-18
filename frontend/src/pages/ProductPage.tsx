import { display } from '../lib/labels';
import { useEffect, useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import {
  catalogKeys,
  useProduct,
  useRecommendations,
  useReviews,
  useReviewEligibility,
} from '../hooks/useCatalog';
import { shopKeys } from '../hooks/useShop';
import { addCartItem } from '../api/shop';
import { createReview, trackView } from '../api/catalog';
import { useUIStore } from '../stores/uiStore';
import { ProductCard } from '../components/catalog/ProductCard';
import { ErrorMessage, Loading } from '../components/ui/Feedback';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { money, date, form, str } from '../lib/format';
import { productImageUrl } from '../lib/images';
export function ProductPage() {
  const id = Number(useParams().id);
  const [search] = useSearchParams();
  const q = useProduct(id);
  const reviews = useReviews(id);
  const eligibility = useReviewEligibility(id);
  const similar = useRecommendations(id);
  const match = useRecommendations(id, true);
  const [variantId, setVariant] = useState<number>();
  const [image, setImage] = useState(0);
  const [color, setColor] = useState<string>();
  const [quantity, setQuantity] = useState(1);
  const qc = useQueryClient();
  const { tracking, notify } = useUIStore();
  useEffect(() => {
    setVariant(undefined);
    setImage(0);
    setColor(undefined);
    setQuantity(1);
    if (tracking)
      trackView(id)
        .then(() => qc.invalidateQueries({ queryKey: catalogKeys.recommendations() }))
        .catch(() => {});
  }, [id, tracking, qc]);
  const add = useMutation({
    mutationFn: () => addCartItem(variantId!, quantity),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: shopKeys.cart() });
      notify('Đã thêm sản phẩm vào giỏ hàng.');
    },
  });
  const review = useMutation({
    mutationFn: (f: FormData) =>
      createReview(id, {
        rating: Number(f.get('rating')),
        comment: str(f, 'comment'),
        imageUrl: str(f, 'imageUrl') || null,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: catalogKeys.reviews(id) });
      qc.setQueryData(catalogKeys.reviewEligibility(id), {
        canReview: false,
        deliveredPurchase: true,
        alreadyReviewed: true,
      });
      qc.invalidateQueries({ queryKey: catalogKeys.reviewEligibility(id) });
      notify('Cảm ơn bạn đã chia sẻ đánh giá.');
    },
  });
  if (q.isPending) return <Loading />;
  if (q.isError)
    return (
      <section className="section">
        <ErrorMessage error={q.error} />
      </section>
    );
  const p = q.data;
  const v = p.variants.find((x) => x.variantId === variantId);
  const colors = [...new Set(p.variants.map((x) => x.color))];
  const selectedColor =
    color || (colors.includes(search.get('color') || '') ? search.get('color')! : colors[0]);
  const colorVariants = p.variants.filter((x) => x.color === selectedColor);
  const colorPhotos = [
    ...new Set(colorVariants.map((x) => x.imageUrl).filter((url): url is string => !!url)),
  ];
  const gallery = colorPhotos.length ? colorPhotos : p.images.map((i) => i.imageUrl);
  function chooseColor(next: string) {
    setColor(next);
    setImage(0);
    setQuantity(1);
    const sameSize = p.variants.find(
      (x) => x.color === next && x.size === v?.size && x.stockQuantity > 0,
    );
    setVariant(sameSize?.variantId);
    add.reset();
  }
  return (
    <section className="section">
      <div className="breadcrumbs">
        <Link to="/shop">Cửa hàng</Link>
        <span>/</span>
        {p.productName}
      </div>
      <div className="product-detail">
        <div>
          <div className="detail-image">
            <img
              src={productImageUrl(gallery[image] || gallery[0], p.productId)}
              alt={p.productName + ' — ' + display(selectedColor)}
            />
          </div>
          {gallery.length > 1 && (
            <div className="thumbnails">
              {gallery.map((url, n) => (
                <button
                  key={url}
                  aria-label={'Ảnh sản phẩm ' + (n + 1)}
                  onClick={() => setImage(n)}
                  className={image === n ? 'selected' : ''}
                >
                  <img src={productImageUrl(url, p.productId)} alt="" />
                </button>
              ))}
            </div>
          )}
        </div>
        <div className="detail-copy">
          <div className="eyebrow">
            {p.brand} / {display(p.gender)}
          </div>
          <h1>{p.productName}</h1>
          <div className="detail-price">
            {money(p.salePrice ?? p.price)} {p.salePrice !== null && <del>{money(p.price)}</del>}
          </div>
          <p className="description">{p.description}</p>
          <div className="material">
            <span>Chất liệu</span>
            <strong>{p.material}</strong>
          </div>
          <div className="color-heading">
            <strong>Màu sắc: {display(selectedColor)}</strong>
          </div>
          <div className="color-options" role="group" aria-label="Chọn màu sắc">
            {colors.map((next) => {
              const variant = p.variants.find((x) => x.color === next);
              const available = p.variants.some((x) => x.color === next && x.stockQuantity > 0);
              return (
                <button
                  type="button"
                  key={next}
                  aria-pressed={selectedColor === next}
                  className={selectedColor === next ? 'selected' : ''}
                  onClick={() => chooseColor(next)}
                >
                  <img
                    src={productImageUrl(variant?.imageUrl || p.images[0]?.imageUrl, p.productId)}
                    alt=""
                  />
                  <span>
                    {display(next)}
                    {!available && <small>Hết hàng</small>}
                  </span>
                </button>
              );
            })}
          </div>
          <div className="size-heading">
            <strong>Chọn kích cỡ</strong>
            <Link to="/advisor">Tư vấn kích cỡ ↗</Link>
          </div>
          <div className="variant-options">
            {colorVariants.map((x) => (
              <button
                key={x.variantId}
                className={variantId === x.variantId ? 'selected' : ''}
                disabled={!x.stockQuantity}
                aria-pressed={variantId === x.variantId}
                onClick={() => {
                  setVariant(x.variantId);
                  setQuantity(1);
                  add.reset();
                }}
              >
                {x.size}
              </button>
            ))}
          </div>
          <div className="add-row">
            <label>
              Số lượng
              <input
                type="number"
                min={1}
                max={Math.min(v?.stockQuantity || 1, 100)}
                value={quantity}
                onChange={(e) => setQuantity(Number(e.target.value))}
              />
            </label>
            <Button
              disabled={
                !v || add.isPending || quantity < 1 || quantity > Math.min(v.stockQuantity, 100)
              }
              onClick={() => add.mutate()}
            >
              {add.isPending ? 'Đang thêm…' : 'Thêm vào giỏ ↗'}
            </Button>
          </div>
          <p className="subtle">
            {v
              ? `Còn ${v.stockQuantity} sản phẩm cho lựa chọn này.`
              : 'Vui lòng chọn kích cỡ và màu sắc còn hàng.'}
          </p>
          <ErrorMessage error={add.error} />
          <div className="detail-promises">
            <span>↗ Miễn phí vận chuyển cho đơn từ 1.000.000₫</span>
            <span>◇ Đổi trả trong 7 ngày, theo xét duyệt của cửa hàng</span>
          </div>
          <Link to="/advisor" className="advisor-link">
            ✧ Thêm gợi ý cho phong cách của bạn <span>→</span>
          </Link>
        </div>
      </div>
      <div className="reviews-layout">
        <div>
          <div className="eyebrow">TRẢI NGHIỆM VÀ CHIA SẺ</div>
          <h2>Khách hàng nói gì?</h2>
          <ErrorMessage error={reviews.error} />
          {reviews.isPending ? (
            <Loading />
          ) : reviews.data?.length ? (
            reviews.data.map((r) => (
              <article className="review" key={r.reviewId}>
                <div>
                  <strong>{r.fullName}</strong>
                  <Badge>
                    {'★'.repeat(r.rating)}
                    {'☆'.repeat(5 - r.rating)}
                  </Badge>
                </div>
                <p>{r.comment}</p>
                {r.imageUrl && (
                  <img src={r.imageUrl} alt="Ảnh đánh giá của khách hàng" loading="lazy" />
                )}
                <small>{date(r.createdAt)}</small>
              </article>
            ))
          ) : (
            <p className="subtle">Chưa có đánh giá. Hãy là người đầu tiên chia sẻ cảm nhận.</p>
          )}
        </div>
        {eligibility.isPending ? (
          <div className="panel">
            <Loading />
          </div>
        ) : eligibility.isError ? (
          <div className="panel">
            <ErrorMessage error={eligibility.error} />
            <button className="text-button" onClick={() => eligibility.refetch()}>
              Kiểm tra lại quyền đánh giá
            </button>
          </div>
        ) : !eligibility.data?.canReview ? (
          <div className="panel">
            <h3>Đánh giá sản phẩm</h3>
            <p className="subtle">
              {eligibility.data?.alreadyReviewed
                ? 'Bạn đã đánh giá sản phẩm này. Cảm ơn bạn đã chia sẻ trải nghiệm.'
                : 'Bạn chỉ có thể đánh giá sản phẩm đã mua sau khi đơn hàng được giao thành công.'}
            </p>
            {!eligibility.data?.alreadyReviewed && (
              <Link className="text-button" to="/orders">
                Theo dõi đơn hàng →
              </Link>
            )}
          </div>
        ) : (
          <form className="panel" onSubmit={(e) => review.mutate(form(e))}>
            <h3>Bạn cảm thấy thế nào?</h3>
            <p className="subtle">
              Bạn có thể đánh giá sản phẩm sau khi đơn hàng được giao thành công.
            </p>
            <label>
              Điểm đánh giá
              <select name="rating" defaultValue="5">
                {[5, 4, 3, 2, 1].map((n) => (
                  <option key={n} value={n}>
                    {n} sao
                  </option>
                ))}
              </select>
            </label>
            <label>
              Nội dung đánh giá
              <textarea name="comment" required maxLength={5000} />
            </label>
            <label>
              Liên kết ảnh (không bắt buộc)
              <input name="imageUrl" type="url" placeholder="https://…" maxLength={500} />
            </label>
            <ErrorMessage error={review.error} />
            <Button disabled={review.isPending}>
              {review.isPending ? 'Đang lưu…' : 'Gửi đánh giá'}
            </Button>
          </form>
        )}
      </div>
      {[
        [similar, 'Thêm lựa chọn cùng phong cách.', 'SẢN PHẨM TƯƠNG TỰ'],
        [match, 'Phối cùng nhau, đẹp hơn.', 'HOÀN THIỆN BỘ TRANG PHỤC'],
      ].map(([query, title, eyebrow]) => {
        const s = query as typeof similar;
        return (
          <div className="related" key={String(title)}>
            <div className="eyebrow">{String(eyebrow)} · GỢI Ý TỪ CỬA HÀNG</div>
            <h2>{String(title)}</h2>
            <ErrorMessage error={s.error} />
            <div className="product-grid">
              {s.data?.slice(0, 4).map((item) => (
                <ProductCard key={item.productId} product={item} />
              ))}
            </div>
          </div>
        );
      })}
    </section>
  );
}
