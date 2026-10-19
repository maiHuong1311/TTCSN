import { useEffect, useRef } from 'react';
import { Icon } from './Icon';
export function Dialog({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    ref.current?.showModal();
  }, []);
  return (
    <dialog ref={ref} onCancel={onClose} className="dialog">
      <div className="dialog-head">
        <h2>{title}</h2>
        <button aria-label="Đóng hộp thoại" className="icon-button" onClick={onClose}>
          <Icon name="close" />
        </button>
      </div>
      {children}
    </dialog>
  );
}
