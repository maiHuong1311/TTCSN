import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useAddresses, useProfile, shopKeys } from '../hooks/useShop';
import { updateProfile } from '../api/auth';
import { deleteAddress } from '../api/shop';
import { deleteInteractions } from '../api/catalog';
import { catalogKeys } from '../hooks/useCatalog';
import { useUIStore } from '../stores/uiStore';
import type { Address } from '../types/domain';
import { AddressForm } from '../components/account/AddressForm';
import { Dialog } from '../components/ui/Dialog';
import { ErrorMessage, Loading } from '../components/ui/Feedback';
import { Button } from '../components/ui/Button';
import { form, str, numberOrNull } from '../lib/format';
export function AccountPage() {
  const profile = useProfile();
  const addresses = useAddresses();
  const [editing, setEditing] = useState<Address | 'new' | null>(null);
  const qc = useQueryClient();
  const { tracking, setTracking, notify } = useUIStore();
  const save = useMutation({
    mutationFn: (f: FormData) =>
      updateProfile({
        fullName: str(f, 'fullName'),
        phoneNumber: str(f, 'phoneNumber'),
        heightCm: numberOrNull(f, 'heightCm'),
        weightKg: numberOrNull(f, 'weightKg'),
        preferredStyle: str(f, 'preferredStyle') || null,
      }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: shopKeys.profile() });
      notify('Đã cập nhật hồ sơ của bạn.');
    },
  });
  const remove = useMutation({
    mutationFn: deleteAddress,
    onSuccess: () => qc.invalidateQueries({ queryKey: shopKeys.addresses() }),
  });
  const clear = useMutation({
    mutationFn: deleteInteractions,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: catalogKeys.all });
      notify('Đã xóa lịch sử xem sản phẩm.');
    },
  });
  if (profile.isPending) return <Loading />;
  if (profile.isError) return <ErrorMessage error={profile.error} />;
  const u = profile.data;
  return (
    <section className="section">
      <div className="eyebrow">KHÔNG GIAN CỦA BẠN</div>
      <h1>Xin chào, {u.fullName}.</h1>
      <div className="account-layout">
        <form className="panel" onSubmit={(e) => save.mutate(form(e))}>
          <h2>Hồ sơ cá nhân.</h2>
          <p className="subtle">
            {u.email} · @{u.username}
          </p>
          <label>
            Họ và tên
            <input name="fullName" defaultValue={u.fullName} required maxLength={100} />
          </label>
          <label>
            Số điện thoại
            <input
              name="phoneNumber"
              defaultValue={u.phoneNumber || ''}
              pattern="(0|[+]84)[35789][0-9]{8}"
              required
            />
          </label>
          <div className="form-grid">
            <label>
              Chiều cao (cm)
              <input
                name="heightCm"
                type="number"
                min={50}
                max={250}
                step="0.1"
                defaultValue={u.heightCm ?? ''}
              />
            </label>
            <label>
              Cân nặng (kg)
              <input
                name="weightKg"
                type="number"
                min={20}
                max={350}
                step="0.1"
                defaultValue={u.weightKg ?? ''}
              />
            </label>
          </div>
          <label>
            Phong cách yêu thích
            <input
              name="preferredStyle"
              defaultValue={u.preferredStyle || ''}
              placeholder="Tối giản, thường ngày, công sở…"
              maxLength={100}
            />
          </label>
          <ErrorMessage error={save.error} />
          <Button disabled={save.isPending}>{save.isPending ? 'Đang lưu…' : 'Lưu hồ sơ'}</Button>
          <div className="privacy">
            <h3>Quyền lựa chọn của bạn.</h3>
            <label className="checkbox">
              <input
                type="checkbox"
                checked={tracking}
                onChange={(e) => setTracking(e.target.checked)}
              />{' '}
              Cho phép dùng lịch sử xem để gợi ý sản phẩm
            </label>
            <p className="subtle">
              Mặc định tắt. Khi bật, hệ thống chỉ lưu lượt xem sản phẩm trong lúc bạn đăng nhập. Bạn
              có thể tắt bất cứ lúc nào hoặc xóa lịch sử bên dưới.
            </p>
            <button
              type="button"
              className="text-button"
              disabled={clear.isPending}
              onClick={() => clear.mutate()}
            >
              Xóa lịch sử xem
            </button>
            <ErrorMessage error={clear.error} />
          </div>
        </form>
        <div>
          <div className="section-head">
            <h2>Địa chỉ của bạn.</h2>
            <button className="text-button" onClick={() => setEditing('new')}>
              + Thêm địa chỉ
            </button>
          </div>
          <ErrorMessage error={addresses.error || remove.error} />
          {addresses.data?.map((a) => (
            <div className="panel address-card" key={a.addressId}>
              <div>
                <h3>
                  {a.receiverName} {a.isDefault && <small>MẶC ĐỊNH</small>}
                </h3>
                <p>
                  {a.phoneNumber}
                  <br />
                  {a.detailAddress}, {a.province}
                </p>
              </div>
              <div className="row">
                <button className="text-button" onClick={() => setEditing(a)}>
                  Chỉnh sửa
                </button>
                <button
                  className="text-button danger"
                  disabled={remove.isPending}
                  onClick={() => remove.mutate(a.addressId)}
                >
                  Xóa
                </button>
              </div>
            </div>
          ))}
          {addresses.data?.length === 0 && <p className="subtle">Bạn chưa lưu địa chỉ nào.</p>}
        </div>
      </div>
      {editing && (
        <Dialog
          title={editing === 'new' ? 'Thêm địa chỉ giao hàng' : 'Chỉnh sửa địa chỉ'}
          onClose={() => setEditing(null)}
        >
          <AddressForm
            address={editing === 'new' ? undefined : editing}
            onSaved={() => setEditing(null)}
          />
        </Dialog>
      )}
    </section>
  );
}
