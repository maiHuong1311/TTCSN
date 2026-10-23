import { Link } from 'react-router-dom';
import { useSizeCharts } from '../hooks/useStore';
import { SizeChartTable } from '../components/catalog/SizeChartTable';
import { Loading, ErrorMessage } from '../components/ui/Feedback';
export function SizeGuidePage() {
  const charts = useSizeCharts();
  return (
    <section className="section size-guide-page">
      <div className="eyebrow">CHỌN CỠ VỪA VẶN</div>
      <h1>Bảng size FORME</h1>
      <p>
        Đo bằng thước dây, giữ thước ngang và không siết chặt. Độ vừa vặn còn
        tùy phom, chất liệu và sở thích mặc.
      </p>
      <p>
        Các bảng dưới đây áp dụng cho trang phục nữ được cửa hàng gắn bảng size
        tương ứng. Sản phẩm khác cần tư vấn riêng.
      </p>
      {charts.isPending && <Loading />}
      <ErrorMessage error={charts.error} />
      {charts.data?.map((chart) => (
        <section
          id={chart.code}
          className="size-chart-section"
          key={chart.code}
        >
          <h2>{chart.name}</h2>
          <p>{chart.appliesTo}</p>
          <SizeChartTable chart={chart} />
          <Link
            className="text-button"
            to={'/advisor?sizeChartCode=' + chart.code}
          >
            Tư vấn theo số đo →
          </Link>
        </section>
      ))}
    </section>
  );
}
