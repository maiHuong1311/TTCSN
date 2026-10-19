import { useQuery } from '@tanstack/react-query';
import * as api from '../api/shop';
import * as auth from '../api/auth';
import { fetchAiStatus } from '../api/ai';
export const shopKeys = {
  all: ['shop'] as const,
  demoPayment: (token: string) => ['demo-payment', token] as const,
  cart: () => [...shopKeys.all, 'cart'],
  addresses: () => [...shopKeys.all, 'addresses'],
  orders: () => [...shopKeys.all, 'orders'],
  order: (id: number) => [...shopKeys.all, 'orders', id],
  profile: () => [...shopKeys.all, 'profile'],
  ai: () => [...shopKeys.all, 'ai'],
};
export const useCart = () => useQuery({ queryKey: shopKeys.cart(), queryFn: api.fetchCart });
export const useAddresses = () =>
  useQuery({ queryKey: shopKeys.addresses(), queryFn: api.fetchAddresses });
export const useOrders = () => useQuery({ queryKey: shopKeys.orders(), queryFn: api.fetchOrders });
export const useOrder = (id: number) =>
  useQuery({
    queryKey: shopKeys.order(id),
    queryFn: () => api.fetchOrder(id),
    refetchInterval: (q) =>
      q.state.data?.payment.paymentMethod === 'DemoQR' &&
      q.state.data?.payment.paymentStatus === 'Pending' &&
      q.state.data?.orderStatus !== 'Cancelled'
        ? 3000
        : false,
  });
export const useDemoPayment = (token: string) =>
  useQuery({
    queryKey: shopKeys.demoPayment(token),
    queryFn: () => api.fetchDemoPayment(token),
    enabled: /^[A-Za-z0-9_-]{43}$/.test(token),
    retry: false,
    refetchInterval: (q) =>
      q.state.data?.paymentStatus === 'Pending' &&
      q.state.data?.orderStatus !== 'Cancelled' &&
      !q.state.error
        ? 3000
        : false,
  });
export const useProfile = () =>
  useQuery({ queryKey: shopKeys.profile(), queryFn: auth.fetchProfile });
export const useAiStatus = () => useQuery({ queryKey: shopKeys.ai(), queryFn: fetchAiStatus });
