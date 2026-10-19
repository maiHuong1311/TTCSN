import { useRef, useState } from 'react';
import { uploadProductImage } from '../../api/admin';
import { ErrorMessage } from '../ui/Feedback';

export function ImagePicker({
  images,
  onChange,
  label,
  multiple = false,
  onBusyChange,
}: {
  images: string[];
  onChange: (images: string[]) => void;
  label: string;
  multiple?: boolean;
  onBusyChange: (busy: boolean) => void;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const input = useRef<HTMLInputElement>(null);
  async function upload(files: File[]) {
    setError(null);
    if (!files.length) return;
    if (multiple && images.length + files.length > 20) {
      setError(new Error('Mỗi sản phẩm được chọn tối đa 20 ảnh.'));
      return;
    }
    if (
      files.some((f) => !['image/jpeg', 'image/png'].includes(f.type) || f.size > 5 * 1024 * 1024)
    ) {
      setError(new Error('Vui lòng chọn ảnh JPG hoặc PNG, tối đa 5 MB mỗi ảnh.'));
      return;
    }
    setBusy(true);
    onBusyChange(true);
    try {
      const uploaded: string[] = [];
      for (const file of files) uploaded.push((await uploadProductImage(file)).imageUrl);
      onChange(multiple ? [...images, ...uploaded] : uploaded);
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
      onBusyChange(false);
    }
  }
  return (
    <div className="image-picker" aria-busy={busy}>
      <span className="image-picker-label">{label}</span>
      <button
        type="button"
        className="button secondary"
        disabled={busy}
        onClick={() => input.current?.click()}
      >
        {busy ? 'Đang tải ảnh…' : images.length && !multiple ? 'Đổi ảnh từ máy' : 'Chọn ảnh từ máy'}
      </button>
      <input
        ref={input}
        hidden
        type="file"
        accept="image/jpeg,image/png"
        multiple={multiple}
        aria-label={label}
        disabled={busy}
        onChange={(e) => {
          void upload(Array.from(e.target.files || []));
          e.target.value = '';
        }}
      />
      <small className="subtle">
        JPG, PNG · Tối đa 5 MB/ảnh{multiple ? ' · Ảnh đầu tiên là ảnh đại diện' : ''}
      </small>
      {busy && <p role="status">Đang tải ảnh lên…</p>}
      <div className="image-picker-previews">
        {images.map((url, i) => (
          <div key={url + i}>
            <img src={url} alt={`${label} ${i + 1}`} />
            <button
              type="button"
              className="text-button"
              disabled={busy}
              onClick={() => onChange(images.filter((_, n) => n !== i))}
            >
              Xóa ảnh {i + 1}
            </button>
          </div>
        ))}
      </div>
      <ErrorMessage error={error} />
    </div>
  );
}
