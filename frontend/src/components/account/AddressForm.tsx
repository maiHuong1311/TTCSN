import { useMutation, useQueryClient } from '@tanstack/react-query';
import { saveAddress } from '../../api/shop';
import { shopKeys } from '../../hooks/useShop';
import type { Address } from '../../types/domain';
import { form, str } from '../../lib/format';
import { Button } from '../ui/Button';
import { ErrorMessage } from '../ui/Feedback';
export function AddressForm({ address, onSaved }: { address?: Address; onSaved: () => void }) {
  const qc = useQueryClient();
  const save = useMutation({
    mutationFn: (f: FormData) =>
      saveAddress(
        {
          receiverName: str(f, 'receiverName'),
          phoneNumber: str(f, 'phoneNumber'),
          province: str(f, 'province'),
          detailAddress: str(f, 'detailAddress'),
          isDefault: f.get('isDefault') === 'on',
        },
        address?.addressId,
      ),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: shopKeys.addresses() });
      onSaved();
    },
  });
  return (
    <form onSubmit={(e) => save.mutate(form(e))}>
      <label>
        Tên người nhận
        <input name="receiverName" defaultValue={address?.receiverName} required maxLength={100} />
      </label>
      <label>
        Số điện thoại
        <input
          name="phoneNumber"
          defaultValue={address?.phoneNumber}
          type="tel"
          pattern="(0|[+]84)[35789][0-9]{8}"
          required
        />
      </label>
      <label>
        Tỉnh / thành phố
        <input name="province" defaultValue={address?.province} required maxLength={100} />
      </label>
      <label>
        Địa chỉ chi tiết
        <textarea
          name="detailAddress"
          defaultValue={address?.detailAddress}
          required
          maxLength={255}
        />
      </label>
      <label className="checkbox">
        <input name="isDefault" type="checkbox" defaultChecked={address?.isDefault} /> Đặt làm địa
        chỉ mặc định
      </label>
      <ErrorMessage error={save.error} />
      <Button disabled={save.isPending}>{save.isPending ? 'Đang lưu…' : 'Lưu địa chỉ'}</Button>
    </form>
  );
}
