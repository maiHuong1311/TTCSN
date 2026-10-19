import { useQuery } from "@tanstack/react-query";
import { fetchAvailableCoupons } from "../api/coupons";

export const couponKeys = { all: ["available-coupons"] as const };
export const useAvailableCoupons = () =>
  useQuery({
    queryKey: couponKeys.all,
    queryFn: fetchAvailableCoupons,
    refetchInterval: 30000,
    staleTime: 0,
  });
