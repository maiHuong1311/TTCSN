import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { useAiStatus, useProfile } from '../hooks/useShop';
import { useProducts } from '../hooks/useCatalog';
import { useSizeCharts } from '../hooks/useStore';
import { createChat } from '../api/ai';
import { ProductCard } from '../components/catalog/ProductCard';
import {
  SizeChartTable,
  measurementLabels,
} from '../components/catalog/SizeChartTable';
import { ErrorMessage } from '../components/ui/Feedback';
import { Button } from '../components/ui/Button';
import { Badge } from '../components/ui/Badge';
import { form, str, numberOrNull } from '../lib/format';
export function AdvisorPage() {
  const [params] = useSearchParams();
  const status = useAiStatus();
  const profile = useProfile();
  const charts = useSizeCharts();
  const products = useProducts({ page: 0, size: 100 });
  const [message, setMessage] = useState(
    params.has('sizeChartCode') ? 'Tư vấn size theo số đo của tôi.' : '',
  );
  const [productId, setProductId] = useState(params.get('productId') || '');
  const [chartCode, setChartCode] = useState(params.get('sizeChartCode') || '');
  const [progress, setProgress] = useState('');
  const selectedProduct = products.data?.content.find(
    (p) => p.productId === Number(productId),
  );
  const selectedChartCode = productId
    ? selectedProduct?.sizeChartCode || ''
    : chartCode;
  const selectedChart = charts.data?.find((c) => c.code === selectedChartCode);
  const chat = useMutation({
    mutationFn: (f: FormData) => {
      setProgress('Đang xử lý yêu cầu…');
      return createChat(
        {
          message: str(f, 'message'),
          orderId: numberOrNull(f, 'orderId'),
          heightCm: numberOrNull(f, 'heightCm'),
          weightKg: numberOrNull(f, 'weightKg'),
          productId: productId ? Number(productId) : null,
          sizeChartCode: selectedChartCode || null,
          bustCm: numberOrNull(f, 'bustCm'),
          waistCm: numberOrNull(f, 'waistCm'),
          hipCm: numberOrNull(f, 'hipCm'),
          shoulderCm: numberOrNull(f, 'shoulderCm'),
          bodyShape: str(f, 'bodyShape') || null,
          allowExternalAi: f.get('allowExternalAi') === 'on',
        },
        setProgress,
      );
    },
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
        <p>Hỏi về phối đồ, chọn size, chính sách hoặc đơn hàng của bạn.</p>
        <Badge>
          {status.data?.configured
            ? 'Trợ lý tư vấn sẵn sàng'
            : 'Tư vấn số đo và hỗ trợ cửa hàng sẵn sàng'}
        </Badge>
        <p className="subtle">
          Size được đối chiếu với bảng cửa hàng; không suy đoán chỉ từ chiều cao
          và cân nặng.
        </p>
        <p>
          <Link className="text-button" to="/size-guide">
            Xem bảng size
          </Link>
          {' · '}
          <Link className="text-button" to="/policies">
            Chính sách và liên hệ
          </Link>
        </p>
        <ErrorMessage error={status.error} />
      </div>
      <div className="advisor-workspace">
        <form className="panel" onSubmit={(e) => chat.mutate(form(e))}>
          <div className="advisor-quick-questions">
            {[
              'Tư vấn size theo số đo của tôi.',
              'Tôi nên mặc gì khi dự tiệc cưới ngoài trời?',
              'Chính sách đổi trả như thế nào?',
              'Phí vận chuyển và thời gian giao hàng?',
            ].map((q) => (
              <button
                type="button"
                className="text-button"
                key={q}
                onClick={() => setMessage(q)}
              >
                {q}
              </button>
            ))}
          </div>
          <label>
            Bạn cần tư vấn điều gì?
            <textarea
              name="message"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              required
              maxLength={2000}
              placeholder="Ví dụ: Tôi cần trang phục đi làm dưới 500 nghìn"
            />
          </label>
          <label>
            Sản phẩm cần tư vấn size (không bắt buộc)
            <select
              name="productId"
              value={productId}
              onChange={(e) => setProductId(e.target.value)}
            >
              <option value="">Đối chiếu bảng size chung</option>
              {products.data?.content.map((p) => (
                <option key={p.productId} value={p.productId}>
                  {p.productName}
                </option>
              ))}
            </select>
          </label>
          <label>
            Bảng size
            <select
              value={selectedChartCode}
              disabled={!!productId}
              onChange={(e) => setChartCode(e.target.value)}
            >
              <option value="">Chọn bảng size phù hợp</option>
              {charts.data?.map((c) => (
                <option value={c.code} key={c.code}>
                  {c.name}
                </option>
              ))}
            </select>
          </label>
          {productId && !selectedChartCode && (
            <p className="subtle">
              Sản phẩm này chưa có bảng size riêng. Vui lòng liên hệ cửa hàng để
              được tư vấn.
            </p>
          )}
          <ErrorMessage error={charts.error || products.error} />
          {selectedChart && (
            <details>
              <summary>Xem thông số bảng {selectedChart.name}</summary>
              <SizeChartTable chart={selectedChart} />
            </details>
          )}
          <div className="form-grid">
            {(['bustCm', 'waistCm', 'hipCm', 'shoulderCm'] as const).map(
              (key) => (
                <label key={key}>
                  {measurementLabels[key]} (cm)
                  <input
                    name={key}
                    type="number"
                    min={key === 'shoulderCm' ? 20 : 30}
                    max={
                      key === 'shoulderCm' ? 80 : key === 'hipCm' ? 220 : 200
                    }
                    step="0.1"
                    placeholder={
                      selectedChart?.measurements.includes(key)
                        ? 'Cần cho bảng đã chọn'
                        : 'Không bắt buộc'
                    }
                  />
                </label>
              ),
            )}
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
            <label>
              Dáng người (tham khảo)
              <select name="bodyShape">
                <option value="">Không cung cấp</option>
                <option>Đồng hồ cát</option>
                <option>Quả lê</option>
                <option>Quả táo</option>
                <option>Chữ nhật</option>
              </select>
            </label>
          </div>
          <label>
            Mã đơn hàng (để tra cứu)
            <input
              name="orderId"
              type="number"
              min={1}
              placeholder="Chỉ tra cứu đơn của bạn"
            />
          </label>
          <label className="checkbox">
            <input type="checkbox" name="allowExternalAi" /> Cho phép dùng
            Gemini để trả lời câu hỏi và gợi ý trang phục.
          </label>
          <p className="subtle">
            Các ô số đo và thông tin đơn hàng được xử lý tại cửa hàng. Chính
            sách và bảng size dùng được mà không cần bật Gemini.
          </p>
          <Button disabled={chat.isPending}>
            {chat.isPending ? 'Đang xử lý…' : 'Gửi câu hỏi ↗'}
          </Button>
          {chat.isPending && <p role="status">{progress}</p>}
          <ErrorMessage error={chat.error} />
        </form>
        {chat.data && (
          <div className="advisor-response" aria-live="polite">
            <p className="advisor-answer">{chat.data.answer}</p>
            {chat.data.suggestedSize && (
              <p>
                Kích cỡ tham khảo: <strong>{chat.data.suggestedSize}</strong>.
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
