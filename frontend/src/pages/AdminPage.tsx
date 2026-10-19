import { couponKeys } from '../hooks/useCoupons';
import { display } from '../lib/labels';
import { productImageUrl } from '../lib/images';
import { ImagePicker } from '../components/catalog/ImagePicker';
import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import * as api from '../api/admin';
import {
  adminKeys,
  useAdminProducts,
  useAdminOrders,
  useReport,
  useCoupons,
  useUsers,
  useBehavior,
} from '../hooks/useAdmin';
import { catalogKeys, useCategories } from '../hooks/useCatalog';
import { shopKeys } from '../hooks/useShop';
import { useAuthStore } from '../stores/authStore';
import { useUIStore } from '../stores/uiStore';
import type { Product, ProductRequest, Category, Coupon, Order } from '../types/domain';
import { Button } from '../components/ui/Button';
import { Dialog } from '../components/ui/Dialog';
import { Badge } from '../components/ui/Badge';
import { ErrorMessage, Loading } from '../components/ui/Feedback';
import { OrderDetails } from '../components/orders/OrderDetails';
import { form, str, numberOrNull, money, date } from '../lib/format';
const tabs = [
  'Tổng quan',
  'Sản phẩm',
  'Danh mục',
  'Đơn hàng',
  'Mã giảm giá',
  'Khách hàng',
] as const;
export function AdminPage() {
  const [tab, setTab] = useState<(typeof tabs)[number]>('Tổng quan');
  return (
    <section className="section admin-page">
      <div className="eyebrow">FORME / TRANG QUẢN TRỊ</div>
      <div className="section-head">
        <h1>Quản lý cửa hàng.</h1>
        <Badge>Quản trị viên</Badge>
      </div>
      <div className="admin-tabs" role="tablist">
        {tabs.map((t) => (
          <button
            role="tab"
            aria-selected={tab === t}
            className={tab === t ? 'active' : ''}
            key={t}
            onClick={() => setTab(t)}
          >
            {t}
          </button>
        ))}
      </div>
      {tab === 'Tổng quan' ? (
        <Overview />
      ) : tab === 'Sản phẩm' ? (
        <Products />
      ) : tab === 'Danh mục' ? (
        <Categories />
      ) : tab === 'Đơn hàng' ? (
        <Orders />
      ) : tab === 'Mã giảm giá' ? (
        <Coupons />
      ) : (
        <Customers />
      )}
    </section>
  );
}
function useRefresh() {
  const qc = useQueryClient();
  const notify = useUIStore((s) => s.notify);
  return () => {
    qc.invalidateQueries({ queryKey: couponKeys.all });
    qc.invalidateQueries({ queryKey: adminKeys.all });
    qc.invalidateQueries({ queryKey: catalogKeys.all });
    qc.invalidateQueries({ queryKey: shopKeys.all });
    notify('Đã lưu thay đổi.');
  };
}
function Overview() {
  const report = useReport();
  const behavior = useBehavior();
  if (report.isPending) return <Loading />;
  if (report.isError) return <ErrorMessage error={report.error} />;
  return (
    <>
      <div className="metric-grid">
        <div className="panel">
          <small>DOANH THU ĐƠN ĐÃ GIAO</small>
          <strong>{money(report.data.revenue)}</strong>
          <p>Đã tính phí vận chuyển và giảm giá</p>
          <small>Số liệu local bao gồm cả đơn thanh toán giả lập.</small>
        </div>
        <div className="panel">
          <small>TỔNG SỐ ĐƠN HÀNG</small>
          <strong>{report.data.orders}</strong>
          <p>Bao gồm tất cả trạng thái</p>
        </div>
        <div className="panel">
          <small>ĐƠN CHỜ XÁC NHẬN</small>
          <strong>{report.data.pendingOrders}</strong>
          <p>Đơn hàng cần được xử lý</p>
        </div>
      </div>
      <div className="account-layout">
        <div className="panel">
          <h2>Sản phẩm bán chạy.</h2>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Số lượng đã giao</th>
                  <th>Doanh thu sản phẩm</th>
                </tr>
              </thead>
              <tbody>
                {report.data.bestSellers.map((s) => (
                  <tr key={s.productId}>
                    <td>{s.productName}</td>
                    <td>{s.quantity}</td>
                    <td>{money(s.revenue)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {!report.data.bestSellers.length && (
            <p className="subtle">Thống kê bán chạy sẽ hiển thị khi có đơn hàng đã giao.</p>
          )}
        </div>
        <div className="panel">
          <h2>Sản phẩm được quan tâm.</h2>
          <p className="subtle">Tổng hợp lượt xem của những khách hàng đã cho phép theo dõi.</p>
          <ErrorMessage error={behavior.error} />
          {behavior.data?.map((b) => (
            <div className="summary-row" key={b.productId}>
              <span>{b.productName}</span>
              <strong>{b.views} lượt xem</strong>
            </div>
          ))}
          {behavior.data?.length === 0 && (
            <p className="subtle">Chưa có lượt xem được phép ghi nhận.</p>
          )}
        </div>
      </div>
    </>
  );
}
function Products() {
  const [page, setPage] = useState(0);
  const q = useAdminProducts(page);
  const [edit, setEdit] = useState<Product | 'new' | null>(null);
  const refresh = useRefresh();
  const deactivate = useMutation({ mutationFn: api.deleteProduct, onSuccess: refresh });
  return (
    <>
      <div className="section-head">
        <h2>Danh sách sản phẩm.</h2>
        <Button onClick={() => setEdit('new')}>+ Thêm sản phẩm</Button>
      </div>
      <ErrorMessage error={q.error || deactivate.error} />
      {q.isPending ? (
        <Loading />
      ) : (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Giá bán</th>
                  <th>Tồn kho</th>
                  <th>Trạng thái</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {q.data?.content.map((p) => (
                  <tr key={p.productId}>
                    <td>
                      <div className="table-product">
                        <img src={productImageUrl(p.images[0]?.imageUrl, p.productId)} alt="" />
                        <span>
                          <strong>{p.productName}</strong>
                          <small>
                            {p.brand} · {p.variants.length} biến thể
                          </small>
                        </span>
                      </div>
                    </td>
                    <td>{money(p.salePrice ?? p.price)}</td>
                    <td>{p.variants.reduce((n, v) => n + v.stockQuantity, 0)}</td>
                    <td>
                      <Badge>{p.status}</Badge>
                    </td>
                    <td>
                      <div className="row">
                        <button className="text-button" onClick={() => setEdit(p)}>
                          Chỉnh sửa
                        </button>
                        <button
                          className="text-button danger"
                          disabled={deactivate.isPending || p.status === 'Inactive'}
                          onClick={() => deactivate.mutate(p.productId)}
                        >
                          Ngừng bán
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="pagination">
            <button disabled={page === 0} onClick={() => setPage(page - 1)}>
              ← Trước
            </button>
            <span>Trang {page + 1}</span>
            <button
              disabled={!q.data || page + 1 >= q.data.totalPages}
              onClick={() => setPage(page + 1)}
            >
              Sau →
            </button>
          </div>
        </>
      )}
      {edit && (
        <Dialog
          title={edit === 'new' ? 'Thêm sản phẩm' : 'Chỉnh sửa sản phẩm'}
          onClose={() => setEdit(null)}
        >
          <ProductForm
            product={edit === 'new' ? undefined : edit}
            onSaved={() => {
              setEdit(null);
              refresh();
            }}
          />
        </Dialog>
      )}
    </>
  );
}
function ProductForm({ product: p, onSaved }: { product?: Product; onSaved: () => void }) {
  const categories = useCategories();
  const [images, setImages] = useState<string[]>(p?.images.map((i) => i.imageUrl) || []);
  const [uploads, setUploads] = useState(0);
  const uploadBusy = (busy: boolean) => setUploads((n) => n + (busy ? 1 : -1));
  const [variants, setVariants] = useState<ProductRequest['variants']>(
    p?.variants.map((v) => ({ ...v })) || [
      { size: '', color: 'Màu cát', stockQuantity: 20, sku: '' },
    ],
  );
  const save = useMutation({
    mutationFn: (f: FormData) =>
      api.saveProduct(
        {
          categoryId: Number(f.get('categoryId')),
          productName: str(f, 'productName'),
          description: str(f, 'description'),
          brand: str(f, 'brand'),
          material: str(f, 'material'),
          gender: str(f, 'gender'),
          price: Number(f.get('price')),
          salePrice: numberOrNull(f, 'salePrice'),
          status: str(f, 'status'),
          variants: variants.map((v) => ({ ...v, imageUrl: v.imageUrl?.trim() || null })),
          images,
        },
        p?.productId,
      ),
    onSuccess: onSaved,
  });
  function variant(index: number, key: string, value: string | number) {
    setVariants((v) => v.map((x, i) => (i === index ? { ...x, [key]: value } : x)));
  }
  return (
    <form
      onSubmit={(e) => {
        const data = form(e);
        if (!uploads && !save.isPending) save.mutate(data);
      }}
    >
      <fieldset className="product-form-fields" disabled={uploads > 0 || save.isPending}>
        <div className="form-grid">
          <label>
            Tên sản phẩm
            <input name="productName" defaultValue={p?.productName} required maxLength={255} />
          </label>
          <label>
            Danh mục
            <select name="categoryId" defaultValue={p?.categoryId} required>
              <option value="">Chọn danh mục</option>
              {categories.data?.map((c) => (
                <option key={c.categoryId} value={c.categoryId}>
                  {c.categoryName}
                </option>
              ))}
            </select>
          </label>
          <label>
            Thương hiệu
            <input name="brand" defaultValue={p?.brand || 'FORME'} maxLength={100} />
          </label>
          <label>
            Chất liệu
            <input name="material" defaultValue={p?.material} maxLength={100} />
          </label>
          <label>
            Đối tượng
            <select name="gender" defaultValue={p?.gender || 'Unisex'}>
              <option value="Unisex">Nam và nữ</option>
              <option value="Female">Nữ</option>
              <option value="Male">Nam</option>
            </select>
          </label>
          <label>
            Trạng thái
            <select name="status" defaultValue={p?.status || 'Active'}>
              <option value="Active">Đang hoạt động</option>
              <option value="Inactive">Ngừng bán</option>
            </select>
          </label>
          <label>
            Giá gốc (VNĐ)
            <input
              name="price"
              type="number"
              min="0.01"
              step="0.01"
              required
              defaultValue={p?.price}
            />
          </label>
          <label>
            Giá khuyến mãi (không bắt buộc)
            <input
              name="salePrice"
              type="number"
              min="0"
              step="0.01"
              defaultValue={p?.salePrice ?? ''}
            />
          </label>
        </div>
        <label>
          Mô tả
          <textarea name="description" defaultValue={p?.description} maxLength={20000} />
        </label>
        <ImagePicker
          label="Ảnh sản phẩm"
          images={images}
          onChange={setImages}
          multiple
          onBusyChange={uploadBusy}
        />
        <h3>Kích cỡ, màu sắc và tồn kho</h3>
        <p className="subtle">Đặt tồn kho bằng 0 để ngừng bán một biến thể hiện có.</p>
        <p className="subtle">
          Kích cỡ không bắt buộc. Để trống nếu sản phẩm không phân kích cỡ; hệ thống sẽ lưu là
          Freesize.
        </p>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Kích cỡ</th>
                <th>Màu sắc</th>
                <th>Tồn kho</th>
                <th>SKU</th>
                <th>Ảnh theo màu</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {variants.map((v, n) => (
                <tr key={n}>
                  <td>
                    <input
                      aria-label={'Kích cỡ ' + (n + 1)}
                      value={v.size}
                      onChange={(e) => variant(n, 'size', e.target.value)}
                      placeholder="Freesize"
                      maxLength={10}
                    />
                  </td>
                  <td>
                    <input
                      aria-label={'Màu sắc ' + (n + 1)}
                      value={v.color}
                      onChange={(e) => variant(n, 'color', e.target.value)}
                      required
                      maxLength={50}
                    />
                  </td>
                  <td>
                    <input
                      aria-label={'Tồn kho ' + (n + 1)}
                      type="number"
                      min={0}
                      value={v.stockQuantity}
                      onChange={(e) => variant(n, 'stockQuantity', Number(e.target.value))}
                      required
                    />
                  </td>
                  <td>
                    <input
                      aria-label={'SKU ' + (n + 1)}
                      value={v.sku}
                      onChange={(e) => variant(n, 'sku', e.target.value)}
                      required
                      maxLength={100}
                    />
                  </td>
                  <td>
                    <ImagePicker
                      label={'Ảnh biến thể ' + (n + 1)}
                      images={v.imageUrl ? [v.imageUrl] : []}
                      onBusyChange={uploadBusy}
                      onChange={(urls) =>
                        setVariants((all) =>
                          all.map((item, i) =>
                            i === n ||
                            (v.color.trim() &&
                              item.color.trim().toLowerCase() === v.color.trim().toLowerCase())
                              ? { ...item, imageUrl: urls[0] || null }
                              : item,
                          ),
                        )
                      }
                    />
                  </td>
                  <td>
                    {!v.variantId && variants.length > 1 && (
                      <button
                        type="button"
                        className="text-button"
                        onClick={() => setVariants((all) => all.filter((_, i) => i !== n))}
                      >
                        ×
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <button
          type="button"
          className="text-button"
          onClick={() =>
            setVariants((v) => [...v, { size: '', color: '', stockQuantity: 0, sku: '' }])
          }
        >
          + Thêm biến thể
        </button>
        <ErrorMessage error={save.error || categories.error} />
        <div className="product-form-actions">
          <Button disabled={save.isPending || uploads > 0}>
            {uploads > 0 ? 'Đang tải ảnh…' : save.isPending ? 'Đang lưu…' : 'Lưu sản phẩm'}
          </Button>
        </div>
      </fieldset>
    </form>
  );
}
function Categories() {
  const q = useCategories();
  const [edit, setEdit] = useState<Category | 'new' | null>(null);
  const refresh = useRefresh();
  const remove = useMutation({ mutationFn: api.deleteCategory, onSuccess: refresh });
  return (
    <>
      <div className="section-head">
        <h2>Quản lý danh mục.</h2>
        <Button onClick={() => setEdit('new')}>+ Thêm danh mục</Button>
      </div>
      <ErrorMessage error={q.error || remove.error} />
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Danh mục</th>
              <th>Danh mục cha</th>
              <th>Mô tả</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            {q.data?.map((c) => (
              <tr key={c.categoryId}>
                <td>{c.categoryName}</td>
                <td>{q.data.find((x) => x.categoryId === c.parentId)?.categoryName || '—'}</td>
                <td>{c.description}</td>
                <td>
                  <div className="row">
                    <button className="text-button" onClick={() => setEdit(c)}>
                      Chỉnh sửa
                    </button>
                    <button
                      className="text-button danger"
                      disabled={remove.isPending}
                      onClick={() => remove.mutate(c.categoryId)}
                    >
                      Xóa
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {edit && (
        <Dialog title="Thông tin danh mục" onClose={() => setEdit(null)}>
          <CategoryForm
            category={edit === 'new' ? undefined : edit}
            categories={q.data || []}
            onSaved={() => {
              setEdit(null);
              refresh();
            }}
          />
        </Dialog>
      )}
    </>
  );
}
function CategoryForm({
  category: c,
  categories,
  onSaved,
}: {
  category?: Category;
  categories: Category[];
  onSaved: () => void;
}) {
  const save = useMutation({
    mutationFn: (f: FormData) =>
      api.saveCategory(
        {
          categoryName: str(f, 'categoryName'),
          parentId: numberOrNull(f, 'parentId'),
          description: str(f, 'description'),
        },
        c?.categoryId,
      ),
    onSuccess: onSaved,
  });
  return (
    <form onSubmit={(e) => save.mutate(form(e))}>
      <label>
        Tên danh mục
        <input name="categoryName" defaultValue={c?.categoryName} required maxLength={100} />
      </label>
      <label>
        Danh mục cha
        <select name="parentId" defaultValue={c?.parentId ?? ''}>
          <option value="">Không có danh mục cha</option>
          {categories
            .filter((x) => x.categoryId !== c?.categoryId)
            .map((x) => (
              <option key={x.categoryId} value={x.categoryId}>
                {x.categoryName}
              </option>
            ))}
        </select>
      </label>
      <label>
        Mô tả
        <textarea name="description" defaultValue={c?.description} maxLength={5000} />
      </label>
      <ErrorMessage error={save.error} />
      <Button disabled={save.isPending}>Lưu danh mục</Button>
    </form>
  );
}
function Orders() {
  const q = useAdminOrders();
  const [edit, setEdit] = useState<Order | null>(null);
  return (
    <>
      <h2>Quản lý và theo dõi đơn hàng.</h2>
      <ErrorMessage error={q.error} />
      {q.isPending ? (
        <Loading />
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Đơn hàng</th>
                <th>Khách hàng</th>
                <th>Ngày đặt</th>
                <th>Tổng cộng</th>
                <th>Trạng thái</th>
                <th>Thanh toán</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {q.data?.map((o) => (
                <tr key={o.orderId}>
                  <td>#{o.orderId}</td>
                  <td>{o.receiverName}</td>
                  <td>{date(o.createdAt)}</td>
                  <td>{money(o.totalAmount)}</td>
                  <td>
                    <Badge>{o.orderStatus}</Badge>
                  </td>
                  <td>
                    {display(o.payment.paymentMethod)} / {display(o.payment.paymentStatus)}
                  </td>
                  <td>
                    <button className="text-button" onClick={() => setEdit(o)}>
                      Xử lý ↗
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {edit && (
        <Dialog title={'Xử lý đơn hàng #' + edit.orderId} onClose={() => setEdit(null)}>
          <ManageOrder order={q.data?.find((o) => o.orderId === edit.orderId) || edit} />
        </Dialog>
      )}
    </>
  );
}
function ManageOrder({ order: o }: { order: Order }) {
  const refresh = useRefresh();
  const status = useMutation({
    mutationFn: (s: string) => api.updateOrderStatus(o.orderId, s),
    onSuccess: refresh,
  });
  const payment = useMutation({
    mutationFn: (f: FormData) => api.confirmPayment(o.orderId, str(f, 'transactionId')),
    onSuccess: refresh,
  });
  const allowed: Record<string, string[]> = {
    Pending: ['Confirmed', 'Cancelled'],
    Confirmed: ['Shipping', 'Cancelled'],
    Shipping: ['Delivered'],
  };
  return (
    <>
      <OrderDetails order={o} productLinks={false} />
      <div className="row no-print">
        {allowed[o.orderStatus]?.map((s) => (
          <button
            key={display(s)}
            className="button outline"
            disabled={status.isPending}
            onClick={() => {
              if (
                s !== 'Cancelled' ||
                window.confirm('Bạn muốn hủy đơn hàng này và hoàn lại tồn kho?')
              )
                status.mutate(s);
            }}
          >
            Chuyển sang {display(s)}
          </button>
        ))}
        <button className="text-button" onClick={() => window.print()}>
          In hóa đơn
        </button>
      </div>
      <ErrorMessage error={status.error} />
      {o.payment.paymentStatus === 'Pending' &&
        o.payment.paymentMethod !== 'COD' &&
        o.payment.paymentMethod !== 'DemoQR' &&
        o.orderStatus !== 'Cancelled' && (
          <form className="no-print" onSubmit={(e) => payment.mutate(form(e))}>
            <h3>Xác nhận khoản thanh toán đã nhận</h3>
            <p className="subtle">Vui lòng đối soát khoản tiền thực nhận trước khi xác nhận.</p>
            <label>
              Mã giao dịch
              <input name="transactionId" required maxLength={255} />
            </label>
            <ErrorMessage error={payment.error} />
            <Button disabled={payment.isPending}>Xác nhận đã nhận thanh toán</Button>
          </form>
        )}
    </>
  );
}
function Coupons() {
  const q = useCoupons();
  const [edit, setEdit] = useState<Coupon | 'new' | null>(null);
  const refresh = useRefresh();
  return (
    <>
      <div className="section-head">
        <h2>Ưu đãi dành cho khách hàng.</h2>
        <Button onClick={() => setEdit('new')}>+ Thêm mã giảm giá</Button>
      </div>
      <ErrorMessage error={q.error} />
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Mã</th>
              <th>Giảm giá</th>
              <th>Đơn tối thiểu</th>
              <th>Giới hạn</th>
              <th>Hết hạn</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {q.data?.map((c) => (
              <tr key={c.couponId}>
                <td>
                  <strong>{c.couponCode}</strong>
                </td>
                <td>
                  {c.discountType === 'Percentage' ? c.discountValue + '%' : money(c.discountValue)}
                </td>
                <td>{money(c.minOrderValue)}</td>
                <td>{c.usageLimit}</td>
                <td>{date(c.endDate)}</td>
                <td>
                  <button className="text-button" onClick={() => setEdit(c)}>
                    Chỉnh sửa
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {edit && (
        <Dialog title="Thông tin mã giảm giá" onClose={() => setEdit(null)}>
          <CouponForm
            coupon={edit === 'new' ? undefined : edit}
            onSaved={() => {
              setEdit(null);
              refresh();
            }}
          />
        </Dialog>
      )}
    </>
  );
}
function CouponForm({ coupon: c, onSaved }: { coupon?: Coupon; onSaved: () => void }) {
  const local = (v: string) => {
    const d = new Date(v);
    return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
  };
  const save = useMutation({
    mutationFn: (f: FormData) =>
      api.saveCoupon(
        {
          couponCode: str(f, 'couponCode'),
          discountType: str(f, 'discountType'),
          discountValue: Number(f.get('discountValue')),
          minOrderValue: Number(f.get('minOrderValue')),
          usageLimit: Number(f.get('usageLimit')),
          startDate: new Date(str(f, 'startDate')).toISOString(),
          endDate: new Date(str(f, 'endDate')).toISOString(),
        },
        c?.couponId,
      ),
    onSuccess: onSaved,
  });
  return (
    <form onSubmit={(e) => save.mutate(form(e))}>
      <label>
        Mã giảm giá
        <input
          name="couponCode"
          required
          pattern="[A-Za-z0-9_-]{2,50}"
          defaultValue={c?.couponCode}
        />
      </label>
      <div className="form-grid">
        <label>
          Loại giảm giá
          <select name="discountType" defaultValue={c?.discountType || 'Percentage'}>
            <option value="Percentage">Theo phần trăm</option>
            <option value="Fixed">Số tiền cố định</option>
          </select>
        </label>
        <label>
          Giá trị
          <input
            name="discountValue"
            type="number"
            step="0.01"
            min="0.01"
            required
            defaultValue={c?.discountValue}
          />
        </label>
        <label>
          Đơn hàng tối thiểu (VNĐ)
          <input
            name="minOrderValue"
            type="number"
            min={0}
            step="0.01"
            required
            defaultValue={c?.minOrderValue ?? 0}
          />
        </label>
        <label>
          Số lượt sử dụng tối đa
          <input
            name="usageLimit"
            type="number"
            min={0}
            required
            defaultValue={c?.usageLimit ?? 100}
          />
        </label>
        <label>
          Ngày bắt đầu
          <input
            type="datetime-local"
            name="startDate"
            required
            defaultValue={c ? local(c.startDate) : undefined}
          />
        </label>
        <label>
          Ngày kết thúc
          <input
            type="datetime-local"
            name="endDate"
            required
            defaultValue={c ? local(c.endDate) : undefined}
          />
        </label>
      </div>
      <ErrorMessage error={save.error} />
      <Button disabled={save.isPending}>Lưu mã giảm giá</Button>
    </form>
  );
}
function Customers() {
  const q = useUsers();
  const actor = useAuthStore((s) => s.user?.userId);
  const refresh = useRefresh();
  const update = useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      api.updateUserStatus(id, status),
    onSuccess: refresh,
  });
  return (
    <>
      <h2>Quản lý khách hàng.</h2>
      <ErrorMessage error={q.error || update.error} />
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Họ và tên</th>
              <th>Thông tin liên hệ</th>
              <th>Vai trò</th>
              <th>Trạng thái</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {q.data?.map((u) => (
              <tr key={u.userId}>
                <td>
                  {u.fullName}
                  <small>@{u.username}</small>
                </td>
                <td>
                  {u.email}
                  <small>{u.phoneNumber}</small>
                </td>
                <td>{display(u.role)}</td>
                <td>
                  <Badge>{u.status}</Badge>
                </td>
                <td>
                  <button
                    className="text-button"
                    disabled={actor === u.userId || update.isPending}
                    onClick={() =>
                      update.mutate({
                        id: u.userId,
                        status: u.status === 'Active' ? 'Blocked' : 'Active',
                      })
                    }
                  >
                    {u.status === 'Active' ? 'Khóa tài khoản' : 'Mở khóa'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
