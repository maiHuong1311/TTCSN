import { useQuery } from '@tanstack/react-query';
import * as api from '../api/admin';
export const adminKeys = {
  all: ['admin'] as const,
  products: (page: number) => [...adminKeys.all, 'products', page],
  orders: () => [...adminKeys.all, 'orders'],
  report: () => [...adminKeys.all, 'report'],
  coupons: () => [...adminKeys.all, 'coupons'],
  users: () => [...adminKeys.all, 'users'],
  behavior: () => [...adminKeys.all, 'behavior'],
};
export const useAdminProducts = (p: number) =>
  useQuery({ queryKey: adminKeys.products(p), queryFn: () => api.fetchAdminProducts(p) });
export const useAdminOrders = () =>
  useQuery({ queryKey: adminKeys.orders(), queryFn: api.fetchAdminOrders });
export const useReport = () => useQuery({ queryKey: adminKeys.report(), queryFn: api.fetchReport });
export const useCoupons = () =>
  useQuery({ queryKey: adminKeys.coupons(), queryFn: api.fetchCoupons });
export const useUsers = () => useQuery({ queryKey: adminKeys.users(), queryFn: api.fetchUsers });
export const useBehavior = () =>
  useQuery({ queryKey: adminKeys.behavior(), queryFn: api.fetchBehavior });
