import { afterEach, expect, it, vi } from 'vitest';
import { createChat } from './ai';
import { ApiError } from './client';
afterEach(() => vi.restoreAllMocks());
const request = {
  message: 'đơn hàng',
  orderId: null,
  heightCm: null,
  weightKg: null,
};
function stream(text: string) {
  return new Response(
    new ReadableStream({
      start(controller) {
        controller.enqueue(new TextEncoder().encode(text));
        controller.close();
      },
    }),
    { status: 200, headers: { 'Content-Type': 'text/event-stream' } },
  );
}
it('reads progress and the final structured result from SSE', async () => {
  const result = {
    mode: 'Local',
    answer: 'Xin chào',
    products: [],
    suggestedSize: null,
  };
  vi.spyOn(globalThis, 'fetch').mockResolvedValue(
    stream(
      'event:status\ndata:Đang tìm sản phẩm\n\nevent:result\ndata:' +
        JSON.stringify(result) +
        '\n\n',
    ),
  );
  const progress = vi.fn();
  expect(await createChat(request, progress)).toEqual(result);
  expect(progress).toHaveBeenCalledWith('Đang tìm sản phẩm');
});
it('keeps Vietnamese order authorization errors received through SSE', async () => {
  vi.spyOn(globalThis, 'fetch').mockResolvedValue(
    stream(
      'event:error\ndata:{"status":404,"detail":"Không tìm thấy đơn hàng."}\n\n',
    ),
  );
  await expect(createChat(request)).rejects.toEqual(
    new ApiError(404, 'Không tìm thấy đơn hàng.'),
  );
});
