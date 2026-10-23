import type { SizeChart } from '../../types/domain';
export const measurementLabels: Record<string, string> = {
  bustCm: 'Vòng ngực',
  waistCm: 'Vòng eo',
  hipCm: 'Vòng ba',
  shoulderCm: 'Vai',
};
export function SizeChartTable({ chart }: { chart: SizeChart }) {
  return (
    <div className="table-wrap">
      <table>
        <caption>{chart.name} · Đơn vị: cm</caption>
        <thead>
          <tr>
            <th>Size</th>
            {chart.measurements.map((key) => (
              <th key={key}>{measurementLabels[key]} (cm)</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {chart.rows.map((row) => (
            <tr key={row.size}>
              <th scope="row">{row.size}</th>
              {chart.measurements.map((key) => (
                <td key={key}>
                  {row.ranges[key].min}–{row.ranges[key].max}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
