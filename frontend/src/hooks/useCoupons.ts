import { useQuery } from "@tanstack/react-query";
import { fetchAvailableCoupons } from "../api/coupons";
import { useAuthStore } from "../stores/authStore";

export const couponKeys = {
  all: ["available-coupons"] as const,
  customer: (userId: number | null) => [...couponKeys.all, userId],
};
export const useAvailableCoupons = () => {
  const userId = useAuthStore((s) => s.token ? s.user?.userId ?? null : null);
  return useQuery({
    queryKey: couponKeys.customer(userId),
    queryFn: fetchAvailableCoupons,
    refetchInterval: 30000,
    staleTime: 0,
  });
};
