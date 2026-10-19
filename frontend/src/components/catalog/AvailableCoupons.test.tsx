import { afterEach, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import * as api from "../../api/coupons";
import { AvailableCoupons } from "./AvailableCoupons";
import type { Coupon } from "../../types/domain";

const coupon: Coupon = {
  couponId: 1,
  couponCode: "SAVE10",
  discountType: "Percentage",
  discountValue: 10,
  minOrderValue: 300000,
  usageLimit: 10,
  startDate: "2020-01-01T00:00:00Z",
  endDate: "2099-01-01T00:00:00Z",
};
function mount(props: React.ComponentProps<typeof AvailableCoupons> = {}) {
  return render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { queries: { retry: false } } })
      }
    >
      <AvailableCoupons {...props} />
    </QueryClientProvider>,
  );
}
afterEach(() => vi.restoreAllMocks());
it("copies a live server coupon and hides expired entries", async () => {
  const user = userEvent.setup();
  const copy = vi.spyOn(navigator.clipboard, "writeText").mockResolvedValue();
  vi.spyOn(api, "fetchAvailableCoupons").mockResolvedValue([
    coupon,
    {
      ...coupon,
      couponId: 2,
      couponCode: "OLD",
      endDate: "2021-01-01T00:00:00Z",
    },
  ]);
  mount();
  await user.click(
    await screen.findByRole("button", { name: "Sao chép mã SAVE10" }),
  );
  expect(copy).toHaveBeenCalledWith("SAVE10");
  expect(screen.queryByText("OLD")).not.toBeInTheDocument();
});
it("disables coupons below the minimum and applies an eligible coupon", async () => {
  vi.spyOn(api, "fetchAvailableCoupons").mockResolvedValue([
    coupon,
    { ...coupon, couponId: 2, couponCode: "BIG", minOrderValue: 900000 },
  ]);
  const apply = vi.fn();
  mount({ subtotal: 390000, onApply: apply });
  expect(
    await screen.findByRole("button", { name: "Áp dụng mã BIG" }),
  ).toBeDisabled();
  await userEvent
    .setup()
    .click(screen.getByRole("button", { name: "Áp dụng mã SAVE10" }));
  expect(apply).toHaveBeenCalledWith("SAVE10");
});
it("shows an empty state when no coupons are available", async () => {
  vi.spyOn(api, "fetchAvailableCoupons").mockResolvedValue([]);
  mount();
  expect(
    await screen.findByText("Hiện chưa có mã giảm giá còn hiệu lực."),
  ).toBeInTheDocument();
});
it("retries a failed request without advertising stale coupon codes", async () => {
  const fetch = vi
    .spyOn(api, "fetchAvailableCoupons")
    .mockRejectedValueOnce(new Error("Không tải được mã"))
    .mockResolvedValue([]);
  mount();
  await userEvent
    .setup()
    .click(await screen.findByRole("button", { name: "Thử lại" }));
  await waitFor(() => expect(fetch).toHaveBeenCalledTimes(2));
  expect(
    await screen.findByText("Hiện chưa có mã giảm giá còn hiệu lực."),
  ).toBeInTheDocument();
});
