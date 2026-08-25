import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { StatisticsService } from '../../../core/services/statistics.service';
import { OrderService } from '../../../core/services/order.service';
import { DashboardOverview, RevenueChartDataPoint, TopSellingProduct } from '../../../core/models/statistics.model';
import { Order } from '../../../core/models/order.model';

interface StatCard {
  label: string;
  value: string;
  delta: string;
  positive: boolean;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DashboardComponent implements OnInit {
  private readonly statisticsService = inject(StatisticsService);
  private readonly orderService = inject(OrderService);

  readonly isLoading = signal<boolean>(true);
  readonly overview = signal<DashboardOverview | null>(null);
  readonly topSellers = signal<TopSellingProduct[]>([]);
  readonly recentOrders = signal<Order[]>([]);
  readonly revenueTrend = signal<RevenueChartDataPoint[]>([]);

  readonly chartPath = signal<string>('M 30 160 L 440 160');

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.isLoading.set(true);

    const today = new Date().toISOString().split('T')[0];
    const thirtyDaysAgo = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

    // 1. Overview KPIs
    this.statisticsService.getOverview(today, today).subscribe({
      next: (res) => {
        if (res.data) {
          this.overview.set(res.data);
        }
      },
      error: (err) => console.error('Failed to load overview', err)
    });

    // 2. Revenue Trend (30 days)
    this.statisticsService.getRevenueTrend('day', thirtyDaysAgo, today).subscribe({
      next: (res) => {
        if (res.data && res.data.length > 0) {
          this.revenueTrend.set(res.data);
          this.generateChartPath(res.data);
        }
      },
      error: (err) => console.error('Failed to load trend', err)
    });

    // 3. Top Selling
    this.statisticsService.getTopSelling(5, thirtyDaysAgo, today).subscribe({
      next: (res) => {
        if (res.data) {
          this.topSellers.set(res.data);
        }
      },
      error: (err) => console.error('Failed to load top sellers', err)
    });

    // 4. Recent Orders
    this.orderService.getAdminOrders({ page: 0, size: 5 }).subscribe({
      next: (res) => {
        if (res.data && res.data.content) {
          this.recentOrders.set(res.data.content);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load recent orders', err);
        this.isLoading.set(false);
      }
    });
  }

  private generateChartPath(data: RevenueChartDataPoint[]): void {
    if (!data || data.length === 0) return;

    const width = 440;
    const height = 140;
    const paddingX = 30;
    const paddingY = 20;

    const maxRev = Math.max(...data.map(d => Number(d.revenue) || 0), 1000000);
    const stepX = (width - paddingX) / Math.max(data.length - 1, 1);

    const points = data.map((d, i) => {
      const x = paddingX + i * stepX;
      const normalizedY = (Number(d.revenue) || 0) / maxRev;
      const y = (height + paddingY) - (normalizedY * height);
      return { x, y };
    });

    if (points.length === 1) {
      this.chartPath.set(`M ${points[0].x} ${points[0].y} L 440 ${points[0].y}`);
      return;
    }

    let d = `M ${points[0].x.toFixed(1)} ${points[0].y.toFixed(1)}`;
    for (let i = 0; i < points.length - 1; i++) {
      const p0 = points[i];
      const p1 = points[i + 1];
      const cpX = (p0.x + p1.x) / 2;
      d += ` C ${cpX.toFixed(1)} ${p0.y.toFixed(1)}, ${cpX.toFixed(1)} ${p1.y.toFixed(1)}, ${p1.x.toFixed(1)} ${p1.y.toFixed(1)}`;
    }

    this.chartPath.set(d);
  }

  formatCurrency(value: number | undefined): string {
    if (value === undefined || value === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
  }

  getStatusClass(status: string): string {
    return status ? status.toLowerCase() : 'pending';
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'Chờ xử lý',
      CONFIRMED: 'Đã xác nhận',
      PROCESSING: 'Đang chuẩn bị',
      SHIPPING: 'Đang giao hàng',
      DELIVERED: 'Đã giao hàng',
      COMPLETED: 'Hoàn thành',
      CANCELLED: 'Đã hủy',
      REFUNDED: 'Đã hoàn tiền'
    };
    return map[status] || status;
  }
}
