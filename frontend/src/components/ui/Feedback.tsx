import { errorText, fieldLabel } from '../../lib/labels';
import { ApiError } from '../../api/client';
export function ErrorMessage({ error }: { error: unknown }) {
  if (!error) return null;
  return (
    <div role="alert" className="error">
      {error instanceof Error ? errorText(error.message) : 'Đã xảy ra lỗi. Vui lòng thử lại.'}
      {error instanceof ApiError && Object.keys(error.errors).length > 0 && (
        <ul>
          {Object.entries(error.errors).map(([k, v]) => (
            <li key={k}>
              {fieldLabel(k)}: {errorText(v)}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
export function Loading() {
  return (
    <div className="loading" role="status">
      <span className="spinner" /> Đang tải dữ liệu…
    </div>
  );
}
export function Empty({ title, children }: { title: string; children?: React.ReactNode }) {
  return (
    <div className="empty">
      <span className="empty-symbol">◇</span>
      <h2>{title}</h2>
      {children}
    </div>
  );
}
