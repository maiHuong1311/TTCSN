import assert from 'node:assert/strict';
import { mkdir, writeFile } from 'node:fs/promises';

const base = process.env.TEST_API_URL || 'http://127.0.0.1:5173';
if (!['127.0.0.1', 'localhost'].includes(new URL(base).hostname)) throw Error('Local test servers only');
let token;
async function call(path, body, authenticated = true) {
  const start = performance.now();
  const response = await fetch(base + '/api' + path, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { 'Content-Type': 'application/json', ...(authenticated && token ? { Authorization: 'Bearer ' + token } : {}) },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(45000),
  });
  const text = await response.text();
  let data; try { data = JSON.parse(text); } catch { data = text; }
  return { status: response.status, ms: Math.round(performance.now() - start), data };
}
const login = await call('/auth/login', {
  email: process.env.TEST_ADMIN_EMAIL || 'admin@forme.local',
  password: process.env.TEST_ADMIN_PASSWORD || 'FormeLocalAdmin2026!',
}, false);
assert.equal(login.status, 200, 'Local test login failed');
token = login.data.accessToken;
const status = await call('/ai/status');
assert.equal(status.data.configured, true, 'Gemini is not configured in the running backend');
assert.match(status.data.mode, /Gemini API/);
const catalog = (await call('/products?size=100')).data.content;
const ids = new Set(catalog.map(p => p.productId));
const results = [];
async function test(name, path, body, check) {
  const response = await call(path, body);
  const pass = check(response);
  results.push({ name, pass, ...response });
  console.log(JSON.stringify({ name, pass, status: response.status, ms: response.ms, mode: response.data?.mode, answer: response.data?.answer, products: response.data?.products?.map(p => ({ id: p.productId, name: p.productName })) }));
}
const valid = r => r.status === 200 && r.data.mode === 'Gemini API' && r.data.answer?.length > 20 && r.data.products.every(p => ids.has(p.productId) && p.status === 'Active' && p.variants.some(v => v.stockQuantity > 0));
await test('General fashion Q&A', '/ai/chat', { message: 'Linen và cotton khác nhau thế nào? Nên chọn chất liệu nào khi trời nóng?', allowExternalAi: true }, valid);
await test('Budget and explicit category', '/ai/chat', { message: 'Tôi chỉ muốn mua áo nữ đi làm dưới 500 nghìn, không gợi ý quần hoặc váy.', allowExternalAi: true }, r => valid(r) && r.data.products.length > 0 && r.data.products.every(p => p.categoryId === 1 && (p.salePrice ?? p.price) <= 500000));
await test('Oversize is a styling question', '/ai/chat', { message: 'Áo oversize nên phối như thế nào cho gọn gàng?', allowExternalAi: true }, valid);
await test('No invented unavailable item', '/ai/chat', { message: 'Cửa hàng có giày thể thao Nike Air Max không? Chỉ giới thiệu nếu thực sự có trong danh mục.', allowExternalAi: true }, r => valid(r) && r.data.products.length === 0);
await test('Opt-out stays local', '/ai/chat', { message: 'Tôi cần áo đi làm', allowExternalAi: false }, r => r.status === 200 && r.data.mode === 'Gợi ý theo dữ liệu cửa hàng');
await test('Zero budget does not bypass filtering', '/ai/chat', { message: 'Tôi cần áo dưới 0đ', allowExternalAi: false }, r => r.status === 200 && r.data.products.length === 0);
await test('Local sizing unchanged', '/ai/chat', { message: 'Tư vấn size', sizeChartCode: 'SIZE_QUAN_NU', waistCm: 67, hipCm: 92, allowExternalAi: true }, r => r.status === 200 && r.data.mode === 'Bảng size cửa hàng' && r.data.suggestedSize === 'M');
await test('SSE returns a real Gemini answer', '/ai/chat/stream', { message: 'Chào bạn, tôi muốn phối đồ nữ đơn giản để đi cà phê.', allowExternalAi: true }, r => {
  if (r.status !== 200 || !r.data.includes('event:status')) return false;
  const frame = r.data.replaceAll('\r\n', '\n').split('\n\n').find(x => x.includes('event:result'));
  if (!frame) return false;
  const result = JSON.parse(frame.split('\n').filter(x => x.startsWith('data:')).map(x => x.slice(5)).join('\n'));
  return valid({ status: 200, data: result });
});
const summary = { total: results.length, passed: results.filter(r => r.pass).length, failed: results.filter(r => !r.pass).length };
await mkdir(new URL('../.local/', import.meta.url), { recursive: true });
await writeFile(new URL('../.local/gemini-live-results.json', import.meta.url), JSON.stringify({ testedAt: new Date().toISOString(), base, status: status.data, summary, results }, null, 2));
console.log(JSON.stringify(summary));
if (summary.failed) process.exitCode = 1;
