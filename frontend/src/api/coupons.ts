import { request } from "./client";
import type { Coupon } from "../types/domain";

export const fetchAvailableCoupons = () => request<Coupon[]>("/coupons");
