import { useQuery } from '@tanstack/react-query';
import { fetchSizeCharts, fetchStoreInfo } from '../api/store';
export const storeKeys = {
  all: ['store'] as const,
  sizes: () => ['store', 'sizes'] as const,
  info: () => ['store', 'info'] as const,
};
export const useSizeCharts = () =>
  useQuery({
    queryKey: storeKeys.sizes(),
    queryFn: fetchSizeCharts,
    staleTime: 300000,
  });
export const useStoreInfo = () =>
  useQuery({
    queryKey: storeKeys.info(),
    queryFn: fetchStoreInfo,
    staleTime: 300000,
  });
