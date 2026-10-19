import { request, body } from './client';
import type {
  PageResponse,
  Product,
  ProductRequest,
  CategoryRequest,
  Category,
  Order,
  Report,
  Coupon,
  CouponRequest,
  User,
  Behavior,
} from '../types/domain';
export const fetchAdminProducts = (page: number) =>
  request<PageResponse<Product>>('/admin/products?page=' + page);
export const saveProduct = (r: ProductRequest, id?: number) =>
  request<Product>(id ? `/admin/products/${id}` : '/admin/products', {
    method: id ? 'PUT' : 'POST',
    body: body(r),
  });
export const deleteProduct = (id: number) =>
  request<void>(`/admin/products/${id}`, { method: 'DELETE' });
export const uploadProductImage = (file: File) => {
  const data = new FormData();
  data.append('file', file);
  return request<{ imageUrl: string }>('/admin/product-images', { method: 'POST', body: data });
};
export const saveCategory = (r: CategoryRequest, id?: number) =>
  request<Category>(id ? `/admin/categories/${id}` : '/admin/categories', {
    method: id ? 'PUT' : 'POST',
    body: body(r),
  });
export const deleteCategory = (id: number) =>
  request<void>(`/admin/categories/${id}`, { method: 'DELETE' });
export const fetchAdminOrders = () => request<Order[]>('/admin/orders');
export const updateOrderStatus = (id: number, status: string) =>
  request<Order>(`/admin/orders/${id}/status`, { method: 'PATCH', body: body({ status }) });
export const confirmPayment = (id: number, transactionId: string) =>
  request<Order>(`/admin/orders/${id}/payment`, { method: 'POST', body: body({ transactionId }) });
export const fetchReport = () => request<Report>('/admin/reports');
export const fetchBehavior = () => request<Behavior[]>('/admin/behavior');
export const fetchCoupons = () => request<Coupon[]>('/admin/coupons');
export const saveCoupon = (r: CouponRequest, id?: number) =>
  request<Coupon>(id ? `/admin/coupons/${id}` : '/admin/coupons', {
    method: id ? 'PUT' : 'POST',
    body: body(r),
  });
export const fetchUsers = () => request<User[]>('/admin/users');
export const updateUserStatus = (id: number, status: string) =>
  request<User>(`/admin/users/${id}/status`, { method: 'PATCH', body: body({ status }) });
