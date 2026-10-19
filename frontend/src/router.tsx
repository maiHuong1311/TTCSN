import { useEffect } from 'react';
import {
  createBrowserRouter,
  Navigate,
  Outlet,
  useLocation,
  type RouteObject,
} from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from './stores/authStore';
import { Layout } from './components/Layout';
import { AdminLayout } from './components/AdminLayout';
import { HomePage } from './pages/HomePage';
import { AuthPage } from './pages/AuthPage';
import { PasswordResetPage } from './pages/PasswordResetPage';
import { DemoPaymentPage } from './pages/DemoPaymentPage';
import { ShopPage } from './pages/ShopPage';
import { ProductPage } from './pages/ProductPage';
import { CartPage } from './pages/CartPage';
import { CheckoutPage } from './pages/CheckoutPage';
import { OrdersPage, OrderPage } from './pages/OrdersPage';
import { AccountPage } from './pages/AccountPage';
import { AdvisorPage } from './pages/AdvisorPage';
import { AdminPage } from './pages/AdminPage';
function Protected({ admin = false }: { admin?: boolean }) {
  const { token, user, expiresAt, logout } = useAuthStore();
  const location = useLocation();
  const qc = useQueryClient();
  const expired = !!expiresAt && Date.parse(expiresAt) <= Date.now();
  useEffect(() => {
    if (expired) {
      logout();
      qc.clear();
    }
  }, [expired, logout, qc]);
  if (!token || expired)
    return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />;
  if (admin && user?.role !== 'Admin') return <Navigate to="/shop" replace />;
  return <Outlet />;
}
export const appRoutes: RouteObject[] = [
  {
    element: <Protected admin />,
    children: [
      { element: <AdminLayout />, children: [{ path: '/admin', element: <AdminPage /> }] },
    ],
  },
  {
    element: <Layout />,
    children: [
      { path: '/', element: <HomePage /> },
      { path: '/login', element: <AuthPage /> },
      { path: '/demo-payment', element: <DemoPaymentPage /> },
      { path: '/forgot-password', element: <PasswordResetPage key="forgot" /> },
      { path: '/reset-password', element: <PasswordResetPage key="reset" /> },
      {
        element: <Protected />,
        children: [
          { path: '/shop', element: <ShopPage /> },
          { path: '/products/:id', element: <ProductPage /> },
          { path: '/cart', element: <CartPage /> },
          { path: '/checkout', element: <CheckoutPage /> },
          { path: '/orders', element: <OrdersPage /> },
          { path: '/orders/:id', element: <OrderPage /> },
          { path: '/account', element: <AccountPage /> },
          { path: '/advisor', element: <AdvisorPage /> },
        ],
      },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
];
export const router = createBrowserRouter(appRoutes);
