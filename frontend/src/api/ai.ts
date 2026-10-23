import { request } from './client';
import { useAuthStore } from '../stores/authStore';
import { ApiError } from './client';
import type { ChatRequest, ChatResponse } from '../types/domain';
export const fetchAiStatus = () => request<{ configured: boolean; mode: string }>('/ai/status');
export async function createChat(r: ChatRequest, onStatus?: (message: string) => void): Promise<ChatResponse> {
  const response = await fetch('/api/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + useAuthStore.getState().token,
    },
    body: JSON.stringify(r),
  });
  if (!response.ok) {
    if (response.status === 401) useAuthStore.getState().logout();
    const p = await response.json();
    throw new ApiError(response.status, p.detail || 'Trợ lý hiện không khả dụng.');
  }
  const reader = response.body!.getReader();
  const decoder = new TextDecoder();
  let pending = '';
  for (;;) {
    const { value, done } = await reader.read();
    pending += decoder.decode(value, { stream: !done });
    const normalized = pending.replaceAll('\r\n', '\n');
    const frames = normalized.split('\n\n');
    pending = frames.pop() || '';
    for (const frame of frames) {
      if (frame.includes('event:error')) {
        const data = frame.split('\n').filter((line) => line.startsWith('data:')).map((line) => line.slice(5)).join('\n');
        const problem = JSON.parse(data);
        await reader.cancel();
        throw new ApiError(problem.status || 503, problem.detail || 'Trợ lý hiện không khả dụng.');
      }
      if (frame.includes('event:status')) {
        const status = frame.split('\n').filter((line) => line.startsWith('data:')).map((line) => line.slice(5)).join('\n');
        onStatus?.(status);
      }
      if (frame.includes('event:result')) {
        const data = frame
          .split('\n')
          .filter((x) => x.startsWith('data:'))
          .map((x) => x.slice(5))
          .join('\n');
        const result = JSON.parse(data);
        await reader.cancel();
        return result;
      }
    }
    if (done) break;
  }
  throw new Error('Kết nối với trợ lý đã kết thúc trước khi có phản hồi. Vui lòng thử lại.');
}
