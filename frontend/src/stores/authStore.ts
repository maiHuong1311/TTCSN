import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { AuthResponse, User } from '../types/domain';
interface AuthState {
  token: string | null;
  user: User | null;
  expiresAt: string | null;
  login: (r: AuthResponse) => void;
  logout: () => void;
}
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      expiresAt: null,
      login: (r) => set({ token: r.accessToken, user: r.user, expiresAt: r.expiresAt }),
      logout: () => set({ token: null, user: null, expiresAt: null }),
    }),
    { name: 'forme-auth' },
  ),
);
