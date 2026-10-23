import { readFile } from 'node:fs/promises';

// Read literal dotenv values. Never print the key or provider error bodies.
const values = {};
try {
  for (const line of (await readFile(new URL('../../backend/.env', import.meta.url), 'utf8')).replace(/^\uFEFF/, '').split(/\r?\n/)) {
    const match = line.match(/^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$/);
    if (match) values[match[1]] = match[2].trim().replace(/^(['"])(.*)\1$/, '$2');
  }
} catch (error) {
  if (error.code !== 'ENOENT') throw error;
}
const key = process.env.GEMINI_API_KEY || values.GEMINI_API_KEY;
const model = process.env.GEMINI_MODEL || values.GEMINI_MODEL || 'gemini-3.5-flash-lite';
if (!key || key.startsWith('THAY_')) {
  console.error('Chưa điền GEMINI_API_KEY trong backend/.env.');
  process.exit(1);
}
if (!/^[A-Za-z0-9._-]+$/.test(model)) {
  console.error('GEMINI_MODEL phải là mã model, không có tiền tố models/.');
  process.exit(1);
}
async function request(path, body) {
  const response = await fetch('https://generativelanguage.googleapis.com/v1beta/' + path, {
    method: body ? 'POST' : 'GET',
    headers: { 'x-goog-api-key': key, ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(35000),
  });
  if (!response.ok) {
    const hints = { 400: 'Kiểm tra model và cấu trúc yêu cầu.', 401: 'Khóa không hợp lệ.', 403: 'Kiểm tra quyền của khóa hoặc khu vực sử dụng.', 404: 'Model không tồn tại hoặc tài khoản chưa được truy cập.', 429: 'Hết hạn mức hoặc vượt tốc độ yêu cầu.' };
    throw new Error(`Gemini HTTP ${response.status}. ${hints[response.status] || 'Dịch vụ chưa phản hồi thành công.'}`);
  }
  return response.json();
}
try {
  if (process.argv.includes('--list')) {
    let page = '';
    do {
      const data = await request('models?pageSize=1000' + (page ? '&pageToken=' + encodeURIComponent(page) : ''));
      for (const item of data.models || []) {
        if (item.supportedGenerationMethods?.includes('generateContent')) console.log(item.name.replace(/^models\//, ''));
      }
      page = data.nextPageToken;
    } while (page);
  } else {
    const metadata = await request('models/' + encodeURIComponent(model));
    if (!metadata.supportedGenerationMethods?.includes('generateContent')) throw new Error('Model không hỗ trợ generateContent.');
    console.log('Khóa truy cập được model: ' + model);
    if (process.argv.includes('--generate')) {
      const start = performance.now();
      const data = await request('models/' + encodeURIComponent(model) + ':generateContent', {
        contents: [{ role: 'user', parts: [{ text: 'Trả lời bằng tiếng Việt một câu chào khách của cửa hàng thời trang FORME; productIds là mảng rỗng.' }] }],
        generationConfig: {
          temperature: 0.2, maxOutputTokens: 1024, responseMimeType: 'application/json',
          responseSchema: { type: 'OBJECT', properties: { answer: { type: 'STRING' }, productIds: { type: 'ARRAY', items: { type: 'INTEGER' } } }, required: ['answer', 'productIds'] },
        },
      });
      const parts = data.candidates?.[0]?.content?.parts || [];
      const result = JSON.parse(parts.filter(p => !p.thought).map(p => p.text || '').join(''));
      if (typeof result.answer !== 'string' || !result.answer.trim() || !Array.isArray(result.productIds)) throw new Error('Phản hồi không đúng cấu trúc hỏi đáp.');
      console.log(JSON.stringify({ status: 'PASS', model, ms: Math.round(performance.now() - start), answer: result.answer, productIds: result.productIds }));
    }
  }
} catch (error) {
  console.error(error.name === 'TimeoutError' ? 'Gemini quá thời gian chờ.' : error.message === 'fetch failed' ? 'Không kết nối được tới Google từ môi trường hiện tại.' : error.message);
  process.exitCode = 1;
}
