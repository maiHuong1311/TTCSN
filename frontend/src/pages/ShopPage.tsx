import { useEffect } from 'react';
import { trackSearch } from '../api/catalog';
import { useUIStore } from '../stores/uiStore';
import { useSearchParams } from 'react-router-dom';
import { useProducts, useCategories } from '../hooks/useCatalog';
import { ProductCard } from '../components/catalog/ProductCard';
import { ErrorMessage, Loading, Empty } from '../components/ui/Feedback';
import { Icon } from '../components/ui/Icon';
export function ShopPage() {
  const [params, setParams] = useSearchParams();
  const filters = {
    q: params.get('q') || '',
    categoryId: params.get('categoryId') || '',
    gender: params.get('gender') || '',
    page: Math.max(0, Number(params.get('page')) || 0),
    size: 12,
  };
  const tracking = useUIStore((s) => s.tracking);
  useEffect(() => {
    if (!tracking || filters.q.trim().length < 2 || filters.q.length > 100) return;
    const timer = setTimeout(() => { void trackSearch(filters.q).catch(() => {}); }, 800);
    return () => clearTimeout(timer);
  }, [filters.q, tracking]);
  const products = useProducts(filters);
  const categories = useCategories();
  function change(k: string, v: string) {
    setParams((p) => {
      if (v) p.set(k, v);
      else p.delete(k);
      if (k !== 'page') p.delete('page');
      return p;
    });
  }
  return (
    <section className="section shop-section">
      <div className="collection-heading">
        <div>
          <div className="eyebrow">BỘ SƯU TẬP FORME</div>
          <h1>
            Mỗi ngày, <em>đều tinh tế.</em>
          </h1>
          <p className="subtle">Tủ đồ phù hợp với bạn. Bắt đầu từ những gì khiến bạn thoải mái.</p>
        </div>
      </div>
      <div className="filter-bar">
        <div className="category-tabs">
          <button
            className={!filters.categoryId ? 'active' : ''}
            onClick={() => change('categoryId', '')}
          >
            Tất cả
          </button>
          {categories.data?.map((c) => (
            <button
              key={c.categoryId}
              className={filters.categoryId === String(c.categoryId) ? 'active' : ''}
              onClick={() => change('categoryId', String(c.categoryId))}
            >
              {c.categoryName}
            </button>
          ))}
        </div>
        <label className="search-field">
          <Icon name="search" />
          <input
            aria-label="Tìm sản phẩm"
            placeholder="Tìm sản phẩm bạn yêu thích"
            value={filters.q}
            onChange={(e) => change('q', e.target.value)}
          />
        </label>
        <select
          aria-label="Lọc theo đối tượng"
          value={filters.gender}
          onChange={(e) => change('gender', e.target.value)}
        >
          <option value="">Tất cả đối tượng</option>
          <option value="Female">Nữ</option>
          <option value="Male">Nam</option>
          <option value="Unisex">Nam và nữ</option>
        </select>
      </div>
      <ErrorMessage error={categories.error} />
      {products.isPending ? (
        <Loading />
      ) : products.isError ? (
        <ErrorMessage error={products.error} />
      ) : products.data.content.length ? (
        <>
          <div className="product-grid">
            {products.data.content.map((p) => (
              <ProductCard key={p.productId} product={p} />
            ))}
          </div>
          <div className="pagination">
            <button
              disabled={!filters.page}
              onClick={() => change('page', String(filters.page - 1))}
            >
              ← Trước
            </button>
            <span>
              {products.data.page + 1} / {Math.max(1, products.data.totalPages)}
            </span>
            <button
              disabled={filters.page + 1 >= products.data.totalPages}
              onClick={() => change('page', String(filters.page + 1))}
            >
              Sau →
            </button>
          </div>
        </>
      ) : (
        <Empty title="Không tìm thấy sản phẩm">
          <p>Hãy thử từ khóa hoặc danh mục khác.</p>
          <button className="text-button" onClick={() => setParams({})}>
            Xóa bộ lọc ↗
          </button>
        </Empty>
      )}
    </section>
  );
}
