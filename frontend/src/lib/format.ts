export const money = (n: number) =>
  new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND',
    maximumFractionDigits: 0,
  }).format(n);
export const date = (s: string) =>
  new Intl.DateTimeFormat('vi-VN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(s));
export const form = (e: React.FormEvent<HTMLFormElement>) => {
  e.preventDefault();
  return new FormData(e.currentTarget);
};
export const str = (f: FormData, k: string) => String(f.get(k) || '');
export const numberOrNull = (f: FormData, k: string) => (str(f, k) ? Number(f.get(k)) : null);
