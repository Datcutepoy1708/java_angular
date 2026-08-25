import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { StatisticsService } from '../../../core/services/statistics.service';
import {
  CategoryRevenueShare,
  DashboardOverview,
  PeriodFilter,
  RevenueChartDataPoint,
  TopSellingProduct
} from '../../../core/models/statistics.model';

@Component({
  selector: 'app-statistics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './statistics.component.html',
  styleUrl: './statistics.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatisticsComponent implements OnInit {
  private readonly statisticsService = inject(StatisticsService);

  readonly activePeriod = signal<PeriodFilter>('30days');
  readonly customStartDate = signal<string>('');
  readonly customEndDate = signal<string>('');

  readonly isLoading = signal<boolean>(false);
  readonly isRefreshing = signal<boolean>(false);
  readonly refreshMessage = signal<string | null>(null);

  readonly overview = signal<DashboardOverview | null>(null);
  readonly revenueTrend = signal<RevenueChartDataPoint[]>([]);
  readonly topSellers = signal<TopSellingProduct[]>([]);
  readonly orderStatusDist = signal<Record<string, number>>({});
  readonly categoryShares = signal<CategoryRevenueShare[]>([]);

  // Chart interactivity
  readonly hoveredPoint = signal<RevenueChartDataPoint | null>(null);
  readonly tooltipPos = signal<{ x: number; y: number }>({ x: 0, y: 0 });

  // Computed total order status count
  readonly totalStatusOrders = computed(() => {
    const dist = this.orderStatusDist();
    return Object.values(dist).reduce((sum, count) => sum + count, 0);
  });

  // SVG Chart Dimensions
  readonly chartWidth = 720;
  readonly chartHeight = 220;
  readonly paddingX = 40;
  readonly paddingY = 30;

  ngOnInit(): void {
    this.applyPeriod('30days');
  }

  applyPeriod(period: PeriodFilter): void {
    this.activePeriod.set(period);
    const { start, end } = this.calculateDateRange(period);
    this.loadAllStatistics(start, end, period === 'year' ? 'month' : 'day');
  }

  applyCustomRange(): void {
    if (this.customStartDate() && this.customEndDate()) {
      this.activePeriod.set('custom');
      this.loadAllStatistics(this.customStartDate(), this.customEndDate(), 'day');
    }
  }

  refreshCache(): void {
    this.isRefreshing.set(true);
    this.statisticsService.refreshCache().subscribe({
      next: () => {
        this.refreshMessage.set('Đã làm mới bộ nhớ đệm và tính toán lại số liệu');
        setTimeout(() => this.refreshMessage.set(null), 3000);
        this.isRefreshing.set(false);
        const { start, end } = this.calculateDateRange(this.activePeriod());
        this.loadAllStatistics(start, end, this.activePeriod() === 'year' ? 'month' : 'day');
      },
      error: () => {
        this.isRefreshing.set(false);
      }
    });
  }

  private loadAllStatistics(startDate?: string, endDate?: string, period: string = 'day'): void {
    this.isLoading.set(true);

    // 1. Overview
    this.statisticsService.getOverview(startDate, endDate).subscribe({
      next: (res) => { if (res.data) this.overview.set(res.data); },
      error: (err) => console.error('Overview error', err)
    });

    // 2. Trend
    this.statisticsService.getRevenueTrend(period, startDate, endDate).subscribe({
      next: (res) => { if (res.data) this.revenueTrend.set(res.data); },
      error: (err) => console.error('Trend error', err)
    });

    // 3. Top selling
    this.statisticsService.getTopSelling(10, startDate, endDate).subscribe({
      next: (res) => { if (res.data) this.topSellers.set(res.data); },
      error: (err) => console.error('Top selling error', err)
    });

    // 4. Status distribution
    this.statisticsService.getOrderStatusDistribution(startDate, endDate).subscribe({
      next: (res) => { if (res.data) this.orderStatusDist.set(res.data); },
      error: (err) => console.error('Status error', err)
    });

    // 5. Category share
    this.statisticsService.getCategoryShare(startDate, endDate).subscribe({
      next: (res) => {
        if (res.data) this.categoryShares.set(res.data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Category share error', err);
        this.isLoading.set(false);
      }
    });
  }

  private calculateDateRange(period: PeriodFilter): { start?: string; end?: string } {
    const today = new Date();
    const end = today.toISOString().split('T')[0];

    switch (period) {
      case 'today':
        return { start: end, end };
      case '7days': {
        const d = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000);
        return { start: d.toISOString().split('T')[0], end };
      }
      case '30days': {
        const d = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000);
        return { start: d.toISOString().split('T')[0], end };
      }
      case 'month': {
        const d = new Date(today.getFullYear(), today.getMonth(), 1);
        return { start: d.toISOString().split('T')[0], end };
      }
      case 'year': {
        const d = new Date(today.getFullYear(), 0, 1);
        return { start: d.toISOString().split('T')[0], end };
      }
      case 'custom':
        return { start: this.customStartDate() || undefined, end: this.customEndDate() || undefined };
    }
  }

  // SVG Chart Helpers
  getSvgPoints(): { x: number; y: number; data: RevenueChartDataPoint }[] {
    const data = this.revenueTrend();
    if (!data || data.length === 0) return [];

    const width = this.chartWidth;
    const height = this.chartHeight;
    const pX = this.paddingX;
    const pY = this.paddingY;

    const maxRev = Math.max(...data.map(d => Number(d.revenue) || 0), 1000000);
    const stepX = (width - pX * 2) / Math.max(data.length - 1, 1);

    return data.map((d, i) => {
      const x = pX + i * stepX;
      const normalizedY = (Number(d.revenue) || 0) / maxRev;
      const y = (height - pY) - (normalizedY * (height - pY * 2));
      return { x, y, data: d };
    });
  }

  getSvgPath(): string {
    const points = this.getSvgPoints();
    if (points.length === 0) return '';
    if (points.length === 1) return `M ${points[0].x} ${points[0].y} L ${this.chartWidth - this.paddingX} ${points[0].y}`;

    let d = `M ${points[0].x.toFixed(1)} ${points[0].y.toFixed(1)}`;
    for (let i = 0; i < points.length - 1; i++) {
      const p0 = points[i];
      const p1 = points[i + 1];
      const cpX = (p0.x + p1.x) / 2;
      d += ` C ${cpX.toFixed(1)} ${p0.y.toFixed(1)}, ${cpX.toFixed(1)} ${p1.y.toFixed(1)}, ${p1.x.toFixed(1)} ${p1.y.toFixed(1)}`;
    }
    return d;
  }

  getSvgAreaPath(): string {
    const points = this.getSvgPoints();
    if (points.length === 0) return '';
    const linePath = this.getSvgPath();
    const lastX = points[points.length - 1].x;
    const firstX = points[0].x;
    const bottomY = this.chartHeight - this.paddingY;
    return `${linePath} L ${lastX} ${bottomY} L ${firstX} ${bottomY} Z`;
  }

  onPointHover(pt: { x: number; y: number; data: RevenueChartDataPoint }): void {
    this.hoveredPoint.set(pt.data);
    this.tooltipPos.set({ x: pt.x, y: pt.y });
  }

  onPointLeave(): void {
    this.hoveredPoint.set(null);
  }

  formatCurrency(value: number | undefined): string {
    if (value === undefined || value === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
  }

  getStatusPercent(count: number): number {
    const total = this.totalStatusOrders();
    if (total === 0) return 0;
    return Math.round((count / total) * 100);
  }

  getStatusLabel(status: string): string {
    const map: Record<string, string> = {
      PENDING: 'Chờ duyệt',
      CONFIRMED: 'Đã xác nhận',
      PROCESSING: 'Đang chuẩn bị',
      SHIPPING: 'Đang giao hàng',
      DELIVERED: 'Đã nhận hàng',
      COMPLETED: 'Hoàn thành',
      CANCELLED: 'Đã hủy',
      REFUNDED: 'Đã hoàn tiền'
    };
    return map[status] || status;
  }
}
