import { display } from '../../lib/labels';
export function Badge({ children }: { children: React.ReactNode }) {
  return (
    <span className="badge">{typeof children === 'string' ? display(children) : children}</span>
  );
}
