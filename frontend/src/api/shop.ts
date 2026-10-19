import { request, body } from './client';
import type {
  Address,
  AddressRequest,
  Cart,
  Quote,
  CheckoutRequest,
  Order,
  DemoPayment,
} from '../types/domain';
export const createDemoPayment = (id: number) =>
  request<DemoPayment>(`/orders/${id}/demo-payment`, { method: 'POST' });
export const fetchDemoPayment = (token: string) =>
  request<DemoPayment>('/demo-payments/view', {
    method: 'POST',
    body: body({ token }),
    headers: { Authorization: '' },
  });
export const completeDemoPayment = (token: string, outcome: 'Paid' | 'Failed') =>
  request<DemoPayment>('/demo-payments/complete', {
    method: 'POST',
    body: body({ token, outcome }),
    headers: { Authorization: '' },
  });
export const fetchCart = () => request<Cart>('/carts/current');
export const addCartItem = (variantId: number, quantity: number) =>
  request<Cart>('/carts/current/items', {
    method: 'POST',
    body: body({ variantId, quantity }),
  });
export const updateCartItem = (variantId: number, quantity: number) =>
  request<Cart>('/carts/current/items', {
    method: 'PUT',
    body: body({ variantId, quantity }),
  });
export const deleteCartItem = (id: number) =>
  request<Cart>(`/carts/current/items/${id}`, { method: 'DELETE' });
export const fetchAddresses = () => request<Address[]>('/addresses');
export const saveAddress = (r: AddressRequest, id?: number) =>
  request<Address>(id ? `/addresses/${id}` : '/addresses', {
    method: id ? 'PUT' : 'POST',
    body: body(r),
  });
export const deleteAddress = (id: number) =>
  request<void>(`/addresses/${id}`, { method: 'DELETE' });
export const createQuote = (couponCode: string) =>
  request<Quote>('/orders/quote', {
    method: 'POST',
    body: body({ couponCode }),
  });
export const createOrder = (r: CheckoutRequest) =>
  request<Order>('/orders', { method: 'POST', body: body(r) });
export const fetchOrders = () => request<Order[]>('/orders');
export const fetchOrder = (id: number) => request<Order>(`/orders/${id}`);
export const cancelOrder = (id: number) =>
  request<Order>(`/orders/${id}/cancel`, { method: 'POST' });
