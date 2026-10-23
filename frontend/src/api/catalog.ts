import { request, body } from './client';
import type {
  Product,
  Category,
  PageResponse,
  ProductFilters,
  Review,
  ReviewRequest,
  ReviewEligibility,
} from '../types/domain';
export function fetchProducts(filters: ProductFilters) {
  const q = new URLSearchParams();
  Object.entries(filters).forEach(([k, v]) => {
    if (v !== undefined && v !== '') q.set(k, String(v));
  });
  return request<PageResponse<Product>>('/products?' + q);
}
export const fetchProduct = (id: number) => request<Product>(`/products/${id}`);
export const fetchCategories = () => request<Category[]>('/categories');
export const fetchRecommendations = (productId?: number, match = false, personalized = false) =>
  request<Product[]>(
    `/recommendations?personalized=${personalized}&match=${match}${productId ? '&productId=' + productId : ''}`,
  );
export const trackView = (id: number) => request<void>(`/products/${id}/views`, { method: 'POST' });
export const deleteInteractions = () => request<void>('/interactions', { method: 'DELETE' });
export const fetchReviews = (id: number) => request<Review[]>(`/products/${id}/reviews`);
export const fetchReviewEligibility = (id: number) =>
  request<ReviewEligibility>(`/products/${id}/reviews/eligibility`);
export const createReview = (id: number, r: ReviewRequest) =>
  request<Review>(`/products/${id}/reviews`, { method: 'POST', body: body(r) });

export const trackSearch = (query: string) => request<void>('/search-events', { method: 'POST', body: body({query, consent: true}) });
