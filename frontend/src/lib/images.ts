/** Compatibility for existing carts/records that still refer to the original demo illustrations. */
export function productImageUrl(url?: string, productId = 1) {
  if (url === '/assets/photos/product-6.jpg') return '/assets/photos/product-6.png';
  if (url && !/^\/assets\/product-[1-8]\.svg$/.test(url)) return url;
  const legacy = url?.match(/product-(\d+)\.svg$/);
  const id = legacy ? Number(legacy[1]) : productId >= 1 && productId <= 8 ? productId : 1;
  return '/assets/photos/product-' + id + (id === 6 ? '.png' : '.jpg');
}
