import { request } from './client';
import type { SizeChart, StoreInfo } from '../types/domain';
export const fetchSizeCharts = () => request<SizeChart[]>('/size-charts');
export const fetchStoreInfo = () => request<StoreInfo>('/store-info');
