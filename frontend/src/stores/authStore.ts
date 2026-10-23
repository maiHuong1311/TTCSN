import { useUIStore } from './uiStore';
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
    (set, get) => ({
      token: null,
      user: null,
      expiresAt: null,
      login: (r) => { if (get().user?.userId !== r.user.userId) useUIStore.getState().setTracking(false); set({ token: r.accessToken, user: r.user, expiresAt: r.expiresAt }); },
      logout: () => { useUIStore.getState().setTracking(false); set({ token: null, user: null, expiresAt: null }); },
    }),
    { name: 'forme-auth' },
  ),
);
