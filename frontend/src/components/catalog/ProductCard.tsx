import { Link } from 'react-router-dom';
import { useState } from 'react';
import type { Product } from '../../types/domain';
import { money } from '../../lib/format';
import { productImageUrl } from '../../lib/images';
export function ProductCard({ product: p }: { product: Product }) {
  const stock = p.variants.reduce((n, v) => n + v.stockQuantity, 0);
  const [color, setColor] = useState<string>();
  const colors = [...new Set(p.variants.map((v) => v.color))];
  const selectedColor = color || colors[0];
  const variant = p.variants.find((v) => v.color === selectedColor);
  const href = '/products/' + p.productId + '?color=' + encodeURIComponent(selectedColor || '');
  const photo =
    variant?.imageUrl || p.images.find((i) => i.isPrimary)?.imageUrl || p.images[0]?.imageUrl;
  return (
    <article className="product-card">
      <Link to={href}>
        <div className="product-image">
          <img loading="lazy" src={productImageUrl(photo, p.productId)} alt={p.productName} />
          {p.salePrice !== null && (
            <span className="product-label">−{Math.round((1 - p.salePrice / p.price) * 100)}%</span>
          )}
          <span className="quick-view">
            Xem sản phẩm <span>↗</span>
          </span>
        </div>
      </Link>
      <Link to={href}>
        <div className="product-meta">
          <div>
            <small>{p.material}</small>
            <h3>{p.productName}</h3>
            <p>
              {money(p.salePrice ?? p.price)} {p.salePrice !== null && <del>{money(p.price)}</del>}
            </p>
          </div>
          <span className="size-count">{new Set(p.variants.map((v) => v.size)).size} kích cỡ</span>
        </div>
      </Link>
      {colors.length > 1 && (
        <div className="card-colors" role="group" aria-label={'Màu của ' + p.productName}>
          {colors.map((next) => (
            <button
              key={next}
              type="button"
              aria-label={'Xem màu ' + next}
              title={next}
              aria-pressed={selectedColor === next}
              onClick={() => setColor(next)}
            >
              <img
                src={productImageUrl(
                  p.variants.find((v) => v.color === next)?.imageUrl || p.images[0]?.imageUrl,
                  p.productId,
                )}
                alt=""
              />
            </button>
          ))}
        </div>
      )}
      {!stock && <small>Hiện đã hết hàng</small>}
    </article>
  );
}
