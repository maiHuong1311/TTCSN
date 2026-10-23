import { useUIStore } from '../stores/uiStore';
import { useQuery } from '@tanstack/react-query';
import * as api from '../api/catalog';
import type { ProductFilters } from '../types/domain';
export const catalogKeys = {
  all: ['catalog'] as const,
  list: (p: ProductFilters) => [...catalogKeys.all, 'list', p],
  detail: (id: number) => [...catalogKeys.all, 'detail', id],
  categories: () => [...catalogKeys.all, 'categories'],
  recommendations: (id?: number, match = false) => [
    ...catalogKeys.all,
    'recommendations',
    id,
    match,
  ],
  reviews: (id: number) => [...catalogKeys.all, 'reviews', id],
  reviewEligibility: (id: number) => [...catalogKeys.all, 'review-eligibility', id],
};
export const useProducts = (p: ProductFilters) =>
  useQuery({ queryKey: catalogKeys.list(p), queryFn: () => api.fetchProducts(p) });
export const useProduct = (id: number) =>
  useQuery({ queryKey: catalogKeys.detail(id), queryFn: () => api.fetchProduct(id) });
export const useCategories = () =>
  useQuery({ queryKey: catalogKeys.categories(), queryFn: api.fetchCategories });
export const useRecommendations = (id?: number, match = false) => {
  const tracking = useUIStore((s) => s.tracking);
  return useQuery({
    queryKey: [...catalogKeys.recommendations(id, match), tracking],
    queryFn: () => api.fetchRecommendations(id, match, tracking),
  });
};
export const useReviews = (id: number) =>
  useQuery({ queryKey: catalogKeys.reviews(id), queryFn: () => api.fetchReviews(id) });
export const useReviewEligibility = (id: number) =>
  useQuery({
    queryKey: catalogKeys.reviewEligibility(id),
    queryFn: () => api.fetchReviewEligibility(id),
  });
