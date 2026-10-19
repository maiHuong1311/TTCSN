import { Icon } from '../components/ui/Icon';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { AvailableCoupons } from '../components/catalog/AvailableCoupons';
import { useProducts, useRecommendations } from '../hooks/useCatalog';
import { ProductCard } from '../components/catalog/ProductCard';
import { CollectionBanner } from '../components/catalog/CollectionBanner';
import { ErrorMessage, Loading } from '../components/ui/Feedback';
import { lookbook } from '../lib/lookbook';
export function HomePage() {
  const token = useAuthStore((s) => s.token);
  return (
    <div className="storefront-home">
      <CollectionBanner />
      <section className="store-benefits" aria-label="Quyền lợi mua sắm">
        <div>
          <span>
            <Icon name="truck" size={28} />
          </span>
          <p>
            <strong>MIỄN PHÍ VẬN CHUYỂN</strong>
            <small>Cho đơn hàng từ 1.000.000₫</small>
          </p>
        </div>
        <div>
          <span>
            <Icon name="undo" size={28} />
          </span>
          <p>
            <strong>HỖ TRỢ ĐỔI TRẢ 7 NGÀY</strong>
            <small>Theo chính sách của cửa hàng</small>
          </p>
        </div>
        <div>
          <span>
            <Icon name="shield" size={28} />
          </span>
          <p>
            <strong>THANH TOÁN LINH HOẠT</strong>
            <small>Thuận tiện khi nhận hàng</small>
          </p>
        </div>
        <div>
          <span>
            <Icon name="spark" size={28} />
          </span>
          <p>
            <strong>TƯ VẤN PHONG CÁCH</strong>
            <small>Đồng hành cùng lựa chọn của bạn</small>
          </p>
        </div>
      </section>
      <div className="store-container" id="ma-giam-gia">
        <AvailableCoupons />
      </div>
      <section className="store-container category-discovery">
        <div className="store-section-heading">
          <h2>BẠN ĐANG TÌM GÌ?</h2>
          <p>Thêm cảm hứng cho tủ đồ mỗi ngày</p>
        </div>
        <div className="category-circles">
          {[
            { name: 'Áo nữ', image: 'product-1.jpg', to: '/shop?categoryId=1' },
            { name: 'Quần nữ', image: 'product-5.jpg', to: '/shop?categoryId=2' },
            { name: 'Váy & đầm', image: 'product-3.jpg', to: '/shop?categoryId=3' },
            { name: 'Đồ công sở', image: 'product-2.jpg', to: '/shop?categoryId=2' },
            { name: 'Áo thu đông', image: 'product-7.jpg', to: '/shop?categoryId=1' },
            { name: 'Phụ kiện', image: 'product-6.png', to: '/shop?categoryId=4' },
          ].map((c) => (
            <Link key={c.name} to={c.to}>
              <div>
                <img src={'/assets/photos/' + c.image} alt={c.name} loading="lazy" />
              </div>
              <h3>{c.name}</h3>
            </Link>
          ))}
        </div>
      </section>
      <section className="store-container home-products">
        <div className="store-section-heading">
          <span>THIẾT KẾ MỚI, CẢM HỨNG MỚI</span>
          <h2>HÀNG MỚI VỀ</h2>
          <p>Sẵn sàng làm mới phong cách của bạn</p>
        </div>
        <div className="home-collection-tabs">
          <Link className="active" to="/shop">
            Tất cả sản phẩm
          </Link>
          <Link to="/shop?categoryId=1">Áo nữ</Link>
          <Link to="/shop?categoryId=3">Váy & đầm</Link>
          <Link to="/shop?categoryId=2">Quần nữ</Link>
        </div>
        {token ? <LiveProducts /> : <PreviewProducts />}
        <div className="view-collection">
          <Link className="button outline" to="/shop">
            XEM TẤT CẢ SẢN PHẨM <span>→</span>
          </Link>
        </div>
      </section>
      <section className="store-container editorial-grid">
        <Link to="/shop?categoryId=3" className="editorial-card">
          <img
            src="/assets/photos/product-3.jpg"
            alt="Bộ sưu tập đầm sơ mi thanh lịch"
            loading="lazy"
          />
          <div>
            <small>VÁY & ĐẦM</small>
            <h2>
              Nét duyên
              <br />
              rất riêng.
            </h2>
            <span>KHÁM PHÁ NGAY →</span>
          </div>
        </Link>
        <Link to="/shop?categoryId=1" className="editorial-card">
          <img
            src="/assets/photos/category-knit.jpg"
            alt="Người mẫu với thiết kế áo dệt nữ"
            loading="lazy"
          />
          <div>
            <small>CHẠM VÀO SỰ DỊU DÀNG</small>
            <h2>
              Ấm áp.
              <br />
              Vẫn thật xinh.
            </h2>
            <span>XEM BỘ SƯU TẬP →</span>
          </div>
        </Link>
      </section>
      <section className="sale-collection" id="uu-dai">
        <div className="store-container">
          <div className="store-section-heading">
            <span>LỰA CHỌN YÊU THÍCH, MỨC GIÁ DỄ YÊU</span>
            <h2>ƯU ĐÃI CHO BẠN</h2>
            <p>Những thiết kế đang có giá khuyến mãi tại FORME</p>
          </div>
          {token ? <LiveProducts sale /> : <PreviewProducts sale />}
          <div className="view-collection">
            <Link className="button" to="/shop">
              KHÁM PHÁ CỬA HÀNG →
            </Link>
          </div>
        </div>
      </section>
      {token && <Personalized />}
      <section className="style-advice store-container">
        <div>
          <span>TRỢ LÝ PHONG CÁCH FORME</span>
          <h2>
            Mặc đẹp hơn,
            <br />
            hiểu mình hơn.
          </h2>
          <p>Một gợi ý nhỏ cho bộ trang phục, một lựa chọn phù hợp với bạn.</p>
          <Link className="button outline" to="/advisor">
            GẶP TRỢ LÝ PHONG CÁCH →
          </Link>
        </div>
        <img
          src="/assets/photos/category-set.jpg"
          alt="Gợi ý trang phục nữ cho phong cách thường ngày"
          loading="lazy"
        />
      </section>
    </div>
  );
}
function LiveProducts({ sale = false }: { sale?: boolean }) {
  const q = useProducts({ size: 8 });
  if (q.isPending) return <Loading />;
  if (q.isError) return <ErrorMessage error={q.error} />;
  const products = sale ? q.data.content.filter((p) => p.salePrice !== null) : q.data.content;
  return (
    <div className={`product-grid${products.length === 3 ? ' three-products' : ''}`}>
      {products.map((p) => (
        <ProductCard key={p.productId} product={p} />
      ))}
    </div>
  );
}
function PreviewProducts({ sale = false }: { sale?: boolean }) {
  const items = sale ? lookbook.filter((p) => [1, 3, 6].includes(p.id)) : lookbook;
  return (
    <div className={`product-grid${items.length === 3 ? ' three-products' : ''}`}>
      {items.map((p) => (
        <PreviewCard key={p.id} product={p} />
      ))}
    </div>
  );
}
function PreviewCard({ product: p }: { product: (typeof lookbook)[number] }) {
  const [colorIndex, setColorIndex] = useState(0);
  const selected = p.colors[colorIndex];
  const href = '/products/' + p.id + '?color=' + encodeURIComponent(selected.color);
  return (
    <article className="product-card">
      <Link to={href}>
        <div className="product-image">
          <img src={selected.imageUrl} alt={p.name} loading="lazy" />
          <span className="product-label preview-label">KHÁM PHÁ</span>
          <span className="quick-view">
            Xem sản phẩm <span>↗</span>
          </span>
        </div>
      </Link>
      <Link to={href}>
        <div className="product-meta">
          <div>
            <small>{p.category}</small>
            <h3>{p.name}</h3>
            <p className="preview-price">
              Đăng nhập để xem giá <span>→</span>
            </p>
          </div>
        </div>
      </Link>
      <div className="card-colors" role="group" aria-label={'Màu của ' + p.name}>
        {p.colors.map((item, n) => (
          <button
            type="button"
            key={item.color}
            aria-label={'Xem màu ' + item.color}
            title={item.color}
            aria-pressed={colorIndex === n}
            onClick={() => setColorIndex(n)}
          >
            <img src={item.imageUrl} alt="" loading="lazy" />
          </button>
        ))}
      </div>
    </article>
  );
}
function Personalized() {
  const q = useRecommendations();
  return (
    <section className="store-container home-products">
      <div className="store-section-heading">
        <span>GỢI Ý TỪ DANH MỤC CỬA HÀNG</span>
        <h2>DÀNH RIÊNG CHO BẠN</h2>
      </div>
      {q.isPending ? (
        <Loading />
      ) : q.isError ? (
        <ErrorMessage error={q.error} />
      ) : (
        <div className="product-grid">
          {q.data.slice(0, 4).map((p) => (
            <ProductCard key={p.productId} product={p} />
          ))}
        </div>
      )}
    </section>
  );
}
