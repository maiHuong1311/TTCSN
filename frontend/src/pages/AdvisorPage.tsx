import { useMutation } from '@tanstack/react-query';
import { useAiStatus, useProfile } from '../hooks/useShop';
import { createChat } from '../api/ai';
import { ProductCard } from '../components/catalog/ProductCard';
import { ErrorMessage } from '../components/ui/Feedback';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { form, str, numberOrNull } from '../lib/format';
export function AdvisorPage() {
  const status = useAiStatus();
  const profile = useProfile();
  const chat = useMutation({
    mutationFn: (f: FormData) =>
      createChat({
        message: str(f, 'message'),
        orderId: numberOrNull(f, 'orderId'),
        heightCm: numberOrNull(f, 'heightCm'),
        weightKg: numberOrNull(f, 'weightKg'),
      }),
  });
  return (
    <section className="section advisor-page">
      <div className="advisor-intro">
        <div className="advisor-seal">✧</div>
        <div className="eyebrow">TRỢ LÝ PHONG CÁCH CỦA BẠN</div>
        <h1>
          Thêm chút gợi ý.
          <br />
          <em>Đậm chất riêng bạn.</em>
        </h1>
        <p>Hỏi về cách phối đồ, kích cỡ hoặc tình trạng đơn hàng của bạn.</p>
        <Badge>{status.data?.configured ? 'Đã kết nối AI' : 'AI chưa được kết nối'}</Badge>
        <p className="subtle">
          Thông tin chính sách và tra cứu đơn hàng hoạt động ngay trên máy.
          <br />
          Tư vấn phối đồ và kích cỡ bằng AI sẽ khả dụng sau khi cấu hình API.
        </p>
        <ErrorMessage error={status.error} />
      </div>
      <div className="advisor-workspace">
        <form className="panel" onSubmit={(e) => chat.mutate(form(e))}>
          <label>
            Bạn cần tư vấn điều gì?
            <textarea
              name="message"
              placeholder="Tôi nên mặc gì khi dự tiệc cưới ngoài trời?"
              required
              maxLength={2000}
            />
          </label>
          <div className="form-grid">
            <label>
              Chiều cao (cm)
              <input
                key={'h' + profile.data?.heightCm}
                name="heightCm"
                type="number"
                min={50}
                max={250}
                step="0.1"
                defaultValue={profile.data?.heightCm ?? ''}
              />
            </label>
            <label>
              Cân nặng (kg)
              <input
                key={'w' + profile.data?.weightKg}
                name="weightKg"
                type="number"
                min={20}
                max={350}
                step="0.1"
                defaultValue={profile.data?.weightKg ?? ''}
              />
            </label>
          </div>
          <label>
            Mã đơn hàng (để tra cứu)
            <input name="orderId" type="number" min={1} placeholder="Mã đơn hàng của bạn" />
          </label>
          <Button disabled={chat.isPending}>
            {chat.isPending ? 'Đang xử lý…' : 'Gửi câu hỏi ↗'}
          </Button>
          <ErrorMessage error={chat.error} />
        </form>
        {chat.data && (
          <div className="advisor-response" aria-live="polite">
            <Badge>{chat.data.mode}</Badge>
            <p>{chat.data.answer}</p>
            {chat.data.suggestedSize && (
              <p>
                Kích cỡ gợi ý: <strong>{chat.data.suggestedSize}</strong>. Độ vừa vặn có thể khác
                nhau tùy sản phẩm.
              </p>
            )}
            <div className="product-grid">
              {chat.data.products.map((p) => (
                <ProductCard key={p.productId} product={p} />
              ))}
            </div>
          </div>
        )}
      </div>
    </section>
  );
}
