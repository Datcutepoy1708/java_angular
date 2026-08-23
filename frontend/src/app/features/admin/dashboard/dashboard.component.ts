import { ChangeDetectionStrategy, Component, signal } from '@angular/core';

interface StatCard {
  label: string;
  value: string;
  delta: string;
  positive: boolean;
}

interface RecentOrder {
  orderId: string;
  customer: string;
  status: 'Processing' | 'Completed' | 'Pending';
}

interface BestSeller {
  rank: number;
  name: string;
  sold: number;
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent {
  readonly stats = signal<StatCard[]>([
    { label: 'Doanh thu hôm nay', value: '$4,250.00', delta: '+12% so với hôm qua', positive: true },
    { label: 'Đơn hàng hôm nay', value: '48', delta: '+5% so với hôm qua', positive: true },
    { label: 'Khách hàng mới', value: '12', delta: '-2% so với hôm qua', positive: false },
    { label: 'Cảnh báo tồn kho thấp', value: '5', delta: 'Cần xử lý', positive: false, },
  ]);

  readonly bestSellers = signal<BestSeller[]>([
    { rank: 1, name: 'Intel Core i7-14700K', sold: 124 },
    { rank: 2, name: 'NVIDIA RTX 4070 SUPER', sold: 89 },
    { rank: 3, name: 'Corsair Dominator RAM', sold: 76 },
  ]);

  readonly recentOrders = signal<RecentOrder[]>([
    { orderId: '#CX-8842', customer: 'Nguyễn Văn An', status: 'Processing' },
    { orderId: '#CX-8841', customer: 'Trần Thị Bình', status: 'Completed' },
    { orderId: '#CX-8840', customer: 'Lê Minh Cường', status: 'Pending' },
  ]);

  // SVG path for sparkline chart (decorative, matches mockup curve shape)
  readonly chartPath = 'M 30 160 C 60 150, 80 130, 110 120 C 140 110, 160 80, 190 70 C 220 60, 240 90, 270 85 C 300 80, 330 30, 360 20 C 390 10, 410 40, 440 35';

  getStatusClass(status: string): string {
    return status.toLowerCase();
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      Processing: 'Đang xử lý',
      Completed: 'Hoàn thành',
      Pending: 'Chờ xử lý',
    };
    return map[status] ?? status;
  }
}
