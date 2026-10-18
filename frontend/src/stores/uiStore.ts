import { create } from 'zustand';
import { persist } from 'zustand/middleware';
interface UIState {
  tracking: boolean;
  setTracking: (b: boolean) => void;
  notice: string;
  notify: (s: string) => void;
}
export const useUIStore = create<UIState>()(
  persist(
    (set) => ({
      tracking: false,
      setTracking: (tracking) => set({ tracking }),
      notice: '',
      notify: (notice) => set({ notice }),
    }),
    { name: 'forme-ui', partialize: (s) => ({ tracking: s.tracking }) },
  ),
);
