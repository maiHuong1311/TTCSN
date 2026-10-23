import { useStoreInfo } from '../hooks/useStore';
import { Loading, ErrorMessage } from '../components/ui/Feedback';
export function PoliciesPage() {
  const info = useStoreInfo();
  return (
    <section className="section policies-page">
      <div className="eyebrow">ĐỒNG HÀNH CÙNG BẠN</div>
      <h1>Chính sách cửa hàng</h1>
      {info.isPending && <Loading />}
      <ErrorMessage error={info.error} />
      {info.data?.policies.map((policy) => (
        <section className="policy-section" key={policy.id} id={policy.id}>
          <h2>{policy.title}</h2>
          {policy.paragraphs.map((paragraph) => (
            <p key={paragraph}>{paragraph}</p>
          ))}
        </section>
      ))}
      {info.data && (
        <section id="contact" className="policy-section">
          <h2>Thông tin liên hệ</h2>
          <p>
            Hotline:{' '}
            <a href={'tel:' + info.data.contact.hotline}>
              {info.data.contact.hotline}
            </a>
          </p>
          <p>
            Email:{' '}
            <a href={'mailto:' + info.data.contact.email}>
              {info.data.contact.email}
            </a>
          </p>
          <p>Địa chỉ: {info.data.contact.address}</p>
          <p>Thời gian hỗ trợ: {info.data.contact.supportHours}</p>
        </section>
      )}
    </section>
  );
}
