import { useState } from 'react';
import { Link } from 'react-router-dom';
const slides = [
  {
    eyebrow: 'BỘ SƯU TẬP MỚI · FORME',
    title: 'Thanh lịch',
    accent: 'theo cách riêng.',
    description: 'Những thiết kế nhẹ nhàng, dễ mặc và luôn là bạn.',
    image: '/assets/photos/product-1.jpg',
    alt: 'Người mẫu mặc áo cổ nhún xanh pastel',
    to: '/shop',
    button: 'KHÁM PHÁ NGAY',
    tag: 'BỘ SƯU TẬP MỚI',
  },
  {
    eyebrow: 'CẢM HỨNG CHO MỖI NGÀY',
    title: 'Một chút mới.',
    accent: 'Thật nhiều yêu.',
    description: 'Từ công sở đến dạo phố, tự tin trong từng khoảnh khắc.',
    image: '/assets/photos/product-3.jpg',
    alt: 'Người mẫu mặc đầm sơ mi kẻ sọc thanh lịch',
    to: '/shop?categoryId=3',
    button: 'XEM VÁY & ĐẦM',
    tag: 'THANH LỊCH MỖI NGÀY',
  },
];
export function CollectionBanner() {
  const [active, setActive] = useState(0);
  const slide = slides[active];
  return (
    <section className={'collection-banner banner-slide-' + active} aria-label="Bộ sưu tập nổi bật">
      <div className="banner-copy">
        <span className="banner-kicker">{slide.eyebrow}</span>
        <h1>
          {slide.title}
          <br />
          <em>{slide.accent}</em>
        </h1>
        <p>{slide.description}</p>
        <Link className="button" to={slide.to}>
          {slide.button} <span>→</span>
        </Link>
        <span className="banner-edition">THIẾT KẾ CHO PHONG CÁCH CỦA BẠN</span>
      </div>
      <div className="banner-photograph">
        <img key={slide.image} src={slide.image} alt={slide.alt} loading="eager" />
        <span>{slide.tag}</span>
      </div>
      <button
        className="banner-arrow previous"
        aria-label="Banner trước"
        onClick={() => setActive((active + slides.length - 1) % slides.length)}
      >
        ‹
      </button>
      <button
        className="banner-arrow next"
        aria-label="Banner tiếp theo"
        onClick={() => setActive((active + 1) % slides.length)}
      >
        ›
      </button>
      <div className="banner-dots" aria-label="Chọn banner">
        {slides.map((s, n) => (
          <button
            key={s.tag}
            className={n === active ? 'active' : ''}
            aria-label={'Xem banner ' + (n + 1)}
            aria-current={n === active ? 'true' : undefined}
            onClick={() => setActive(n)}
          />
        ))}
      </div>
    </section>
  );
}
