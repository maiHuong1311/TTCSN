export interface DemoPayment {
  orderId: number;
  amount: number;
  paymentStatus: string;
  orderStatus: string;
  expiresAt: string;
  token: string | null;
}
export interface ReviewEligibility {
  canReview: boolean;
  deliveredPurchase: boolean;
  alreadyReviewed: boolean;
}
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface User {
  userId: number;
  email: string;
  fullName: string;
  username: string;
  phoneNumber: string | null;
  role: 'Customer' | 'Admin';
  status: 'Active' | 'Blocked';
  createdAt: string;
  heightCm: number | null;
  weightKg: number | null;
  preferredStyle: string | null;
}
export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}
export interface RegisterRequest {
  verificationCode: string;
  email: string;
  password: string;
  fullName: string;
  username: string;
  phoneNumber: string;
}
export interface EmailCodeResponse {
  message: string;
  expiresAt: string;
  resendAt: string;
}
export interface AuthMessageResponse {
  message: string;
}
export interface ResetPasswordRequest {
  email: string;
  token: string;
  password: string;
}
export interface VerifyPasswordOtpRequest {
  email: string;
  otp: string;
}
export interface PasswordOtpVerificationResponse {
  token: string;
  expiresInSeconds: number;
}
export interface ProfileRequest {
  fullName: string;
  phoneNumber: string;
  heightCm: number | null;
  weightKg: number | null;
  preferredStyle: string | null;
}
export interface Category {
  categoryId: number;
  categoryName: string;
  parentId: number | null;
  description: string;
}
export interface CategoryRequest {
  categoryName: string;
  parentId: number | null;
  description: string;
}
export interface Variant {
  imageUrl?: string | null;
  variantId: number;
  size: string;
  color: string;
  stockQuantity: number;
  sku: string;
}
export interface Product {
  sizeChartCode?: string | null;
  productId: number;
  categoryId: number;
  productName: string;
  description: string;
  brand: string;
  material: string;
  gender: string;
  price: number;
  salePrice: number | null;
  status: string;
  createdAt: string;
  updatedAt: string;
  variants: Variant[];
  images: { imageId: number; imageUrl: string; isPrimary: boolean }[];
}
export interface ProductRequest {
  sizeChartCode?: string | null;
  categoryId: number;
  productName: string;
  description: string;
  brand: string;
  material: string;
  gender: string;
  price: number;
  salePrice: number | null;
  status: string;
  variants: ({ variantId?: number } & Omit<Variant, 'variantId'>)[];
  images: string[];
}
export interface ProductFilters {
  q?: string;
  categoryId?: string;
  gender?: string;
  page?: number;
  size?: number;
}
export interface AddressRequest {
  receiverName: string;
  phoneNumber: string;
  province: string;
  detailAddress: string;
  isDefault: boolean;
}
export interface Address extends AddressRequest {
  addressId: number;
}
export interface CartItem {
  cartItemId: number;
  variantId: number;
  productId: number;
  productName: string;
  size: string;
  color: string;
  quantity: number;
  stockQuantity: number;
  unitPrice: number;
  imageUrl: string;
}
export interface Cart {
  cartId: number;
  items: CartItem[];
  subtotal: number;
}
export interface Quote {
  subtotal: number;
  shippingFee: number;
  discountAmount: number;
  totalAmount: number;
}
export interface CheckoutRequest {
  addressId: number;
  paymentMethod: string;
  couponCode: string;
  note: string;
}
export interface Order {
  orderId: number;
  userId: number;
  receiverName: string;
  receiverPhone: string;
  shippingAddress: string;
  totalAmount: number;
  shippingFee: number;
  discountAmount: number;
  orderStatus: string;
  note: string;
  createdAt: string;
  items: {
    orderItemId: number;
    variantId: number;
    productId: number;
    productName: string;
    size: string;
    color: string;
    quantity: number;
    unitPrice: number;
  }[];
  payment: {
    paymentId: number;
    paymentMethod: string;
    paymentStatus: string;
    amount: number;
    transactionId: string | null;
    paidAt: string | null;
  };
}
export interface CouponRequest {
  couponCode: string;
  discountType: string;
  discountValue: number;
  minOrderValue: number;
  usageLimit: number;
  startDate: string;
  endDate: string;
}
export interface Coupon extends CouponRequest {
  couponId: number;
}
export interface ReviewRequest {
  rating: number;
  comment: string;
  imageUrl: string | null;
}
export interface Review extends ReviewRequest {
  reviewId: number;
  userId: number;
  fullName: string;
  productId: number;
  createdAt: string;
}
export interface Report {
  orders: number;
  revenue: number;
  pendingOrders: number;
  bestSellers: {
    productId: number;
    productName: string;
    quantity: number;
    revenue: number;
  }[];
}
export interface Behavior {
  productId: number;
  productName: string;
  views: number;
}
export interface ChatRequest {
  message: string;
  orderId: number | null;
  heightCm: number | null;
  weightKg: number | null;
  productId?: number | null;
  sizeChartCode?: string | null;
  bustCm?: number | null;
  waistCm?: number | null;
  hipCm?: number | null;
  shoulderCm?: number | null;
  bodyShape?: string | null;
  allowExternalAi?: boolean;
}
export interface ChatResponse {
  sizeAdvice?: { chartCode: string; chartName: string | null; size: string | null; explanation: string } | null;
  mode: string;
  answer: string;
  suggestedSize: string | null;
  products: Product[];
}

export interface SizeChart {
  id: number; code: string; name: string; appliesTo: string; measurements: string[];
  rows: { size: string; ranges: Record<string, { min: number; max: number }> }[];
}
export interface StoreInfo {
  contact: { hotline: string; email: string; address: string; supportHours: string; timezone: string };
  policies: { id: string; title: string; paragraphs: string[] }[];
}
