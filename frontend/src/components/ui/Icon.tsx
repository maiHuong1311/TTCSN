const paths = {
  home: 'm3 10 9-7 9 7M5 9v12h14V9M9 21v-8h6v8',
  truck: 'M1 3h14v13H1zM15 8h5l3 4v4h-8M7 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0M21 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0',
  undo: 'M3 7h12a6 6 0 0 1 0 12H9M7 3 3 7l4 4',
  shield: 'M12 22s8-4 8-11V5l-8-3-8 3v6c0 7 8 11 8 11m-4-11 3 3 5-5',
  bag: 'M6 7h12l1 13H5L6 7zm3 0V5a3 3 0 016 0v2',
  arrow: 'M4 12h15m-6-6 6 6-6 6',
  search: 'm21 21-5-5M18 10a8 8 0 11-16 0 8 8 0 0116 0',
  user: 'M20 21v-2a7 7 0 00-14 0v2M16 7a4 4 0 11-8 0 4 4 0 018 0',
  close: 'm6 6 12 12M6 18 18 6',
  spark: 'm12 2 2.5 7.5L22 12l-7.5 2.5L12 22l-2.5-7.5L2 12l7.5-2.5L12 2',
  menu: 'M3 6h18M3 12h18M3 18h18',
};
export function Icon({ name, size = 20 }: { name: keyof typeof paths; size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={paths[name]} />
    </svg>
  );
}
