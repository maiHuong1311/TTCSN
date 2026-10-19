import { useAvailableCoupons } from "../../hooks/useCoupons";
import { useUIStore } from "../../stores/uiStore";
import { money, date } from "../../lib/format";
import { ErrorMessage } from "../ui/Feedback";

export function AvailableCoupons({
  subtotal,
  onApply,
  selectedCode,
}: {
  subtotal?: number;
  onApply?: (code: string) => void;
  selectedCode?: string;
}) {
  const query = useAvailableCoupons();
  const notify = useUIStore((s) => s.notify);
  async function copy(code: string) {
    try {
      await navigator.clipboard.writeText(code);
      notify("Đã sao chép mã " + code + ".");
    } catch {
      notify("Mã giảm giá của bạn: " + code);
    }
  }
  const now = Date.now();
  const coupons =
    query.data?.filter(
      (c) => Date.parse(c.startDate) <= now && Date.parse(c.endDate) > now,
    ) ?? [];
  return (
    <section className="available-coupons" aria-label="Mã giảm giá hiện có">
      <h2>Mã giảm giá hiện có</h2>
      {query.isPending && <p role="status">Đang tải mã giảm giá…</p>}
      {query.isError ? (
        <>
          <ErrorMessage error={query.error} />
          <button
            type="button"
            className="text-button"
            onClick={() => void query.refetch()}
          >
            Thử lại
          </button>
        </>
      ) : (
        !query.isPending &&
        !coupons.length && <p>Hiện chưa có mã giảm giá còn hiệu lực.</p>
      )}
      {!query.isError && (
        <div className="available-coupon-grid">
          {coupons.map((c) => {
            const shortfall =
              subtotal === undefined
                ? 0
                : Math.max(0, c.minOrderValue - subtotal);
            const selected =
              selectedCode?.toUpperCase() === c.couponCode.toUpperCase();
            return (
              <article className="available-coupon" key={c.couponId}>
                <strong>{c.couponCode}</strong>
                <p>
                  Giảm{" "}
                  {c.discountType === "Percentage"
                    ? c.discountValue + "%"
                    : money(c.discountValue)}
                </p>
                <p>Đơn từ {money(c.minOrderValue)}</p>
                <small>Hết hạn: {date(c.endDate)}</small>
                {shortfall > 0 && (
                  <small>Mua thêm {money(shortfall)} để áp dụng</small>
                )}
                <button
                  type="button"
                  className="text-button"
                  disabled={!!onApply && (shortfall > 0 || selected)}
                  onClick={() =>
                    onApply ? onApply(c.couponCode) : void copy(c.couponCode)
                  }
                >
                  {onApply
                    ? selected
                      ? "Đang áp dụng"
                      : "Áp dụng mã " + c.couponCode
                    : "Sao chép mã " + c.couponCode}
                </button>
              </article>
            );
          })}
        </div>
      )}
    </section>
  );
}
