import { useAuthStore } from '../stores/authStore';
export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public errors: Record<string, string> = {},
  ) {
    super(message);
    this.name = 'ApiError';
  }
}
export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = useAuthStore.getState().token;
  const response = await fetch('/api' + path, {
    ...options,
    headers: {
      ...(options.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  if (!response.ok) {
    if (response.status === 401) useAuthStore.getState().logout();
    const problem = await response.json().catch(() => ({}));
    throw new ApiError(
      response.status,
      problem.detail || 'Không thể thực hiện yêu cầu. Vui lòng thử lại.',
      problem.errors || {},
    );
  }
  if (response.status === 204 || response.headers.get('content-length') === '0')
    return undefined as T;
  const text = await response.text();
  return text ? JSON.parse(text) : (undefined as T);
}
export const body = (value: unknown) => JSON.stringify(value);
