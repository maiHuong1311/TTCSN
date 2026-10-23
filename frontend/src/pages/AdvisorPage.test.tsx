import { afterEach, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { AdvisorPage } from './AdvisorPage';
import { SizeGuidePage } from './SizeGuidePage';
import * as ai from '../api/ai';

const charts = [
  {
    id: 2,
    code: 'SIZE_QUAN_NU',
    name: 'Quần nữ',
    appliesTo: 'Quần jean, quần tây',
    measurements: ['waistCm', 'hipCm'],
    rows: [
      {
        size: 'M',
        ranges: { waistCm: { min: 66, max: 70 }, hipCm: { min: 90, max: 94 } },
      },
    ],
  },
];
vi.mock('../hooks/useShop', () => ({
  useAiStatus: () => ({ data: { configured: false } }),
  useProfile: () => ({ data: { heightCm: null, weightKg: null } }),
}));
vi.mock('../hooks/useCatalog', () => ({
  useProducts: () => ({ data: { content: [] } }),
}));
vi.mock('../hooks/useStore', () => ({
  useSizeCharts: () => ({ data: charts }),
}));
function mount(element: React.ReactNode, path = '/advisor') {
  return render(
    <QueryClientProvider
      client={
        new QueryClient({ defaultOptions: { mutations: { retry: false } } })
      }
    >
      <MemoryRouter initialEntries={[path]}>{element}</MemoryRouter>
    </QueryClientProvider>,
  );
}
afterEach(() => vi.restoreAllMocks());
it('sends body measurements with the Vòng ba label and requires opt-in for Gemini', async () => {
  const chat = vi
    .spyOn(ai, 'createChat')
    .mockResolvedValue({
      mode: 'Bảng size cửa hàng',
      answer: 'Gợi ý size M.',
      suggestedSize: 'M',
      products: [],
    });
  mount(<AdvisorPage />, '/advisor?sizeChartCode=SIZE_QUAN_NU');
  const user = userEvent.setup();
  expect(screen.queryByLabelText('Vòng mông (cm)')).not.toBeInTheDocument();
  await user.type(screen.getByLabelText('Vòng eo (cm)'), '67');
  await user.type(screen.getByLabelText('Vòng ba (cm)'), '92');
  await user.click(screen.getByRole('button', { name: 'Gửi câu hỏi ↗' }));
  await waitFor(() =>
    expect(chat).toHaveBeenCalledWith(
      expect.objectContaining({
        waistCm: 67,
        hipCm: 92,
        sizeChartCode: 'SIZE_QUAN_NU',
        allowExternalAi: false,
      }),
      expect.any(Function),
    ),
  );
  expect(await screen.findByText('Gợi ý size M.')).toBeInTheDocument();
});
it('shows the supplied hip measurement column as Vòng ba in size tables', () => {
  mount(<SizeGuidePage />, '/size-guide');
  expect(
    screen.getByRole('columnheader', { name: 'Vòng ba (cm)' }),
  ).toBeInTheDocument();
  expect(screen.getByText('90–94')).toBeInTheDocument();
});
