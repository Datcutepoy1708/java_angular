import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
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

export interface DashboardChartPoint {
  x: number;
  y: number;
  data: RevenueChartDataPoint;
}

export interface DashboardChartBar {
  x: number;
  y: number;
  width: number;
  height: number;
  data: RevenueChartDataPoint;
}

export interface YAxisGuide {
  y: number;
  label: string;
}

export interface XAxisGuide {
  x: number;
  label: string;
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
  readonly isTrendLoading = signal<boolean>(false);
  readonly overview = signal<DashboardOverview | null>(null);
  readonly topSellers = signal<TopSellingProduct[]>([]);
  readonly recentOrders = signal<Order[]>([]);
  readonly revenueTrend = signal<RevenueChartDataPoint[]>([]);

  // Chart configuration & state
  readonly selectedPeriod = signal<'7d' | '30d'>('30d');
  readonly chartType = signal<'area' | 'bar'>('area');
  readonly hoveredPoint = signal<RevenueChartDataPoint | null>(null);
  readonly activeChartPoint = signal<DashboardChartPoint | null>(null);

  readonly chartWidth = 640;
  readonly chartHeight = 220;
  readonly paddingLeft = 55;
  readonly paddingRight = 20;
  readonly paddingTop = 25;
  readonly paddingBottom = 35;

  readonly chartPath = signal<string>('M 55 185 L 620 185');
  readonly chartAreaPath = signal<string>('M 55 185 L 620 185 Z');
  readonly chartPoints = signal<DashboardChartPoint[]>([]);

  // Summary metrics in chart card
  readonly totalPeriodRevenue = computed(() => {
    return this.revenueTrend().reduce((sum, d) => sum + (Number(d.revenue) || 0), 0);
  });

  readonly totalPeriodOrders = computed(() => {
    return this.revenueTrend().reduce((sum, d) => sum + (Number(d.orderCount) || 0), 0);
  });

  readonly dailyAverageRevenue = computed(() => {
    const list = this.revenueTrend();
    if (!list || list.length === 0) return 0;
    return Math.round(this.totalPeriodRevenue() / list.length);
  });

  readonly peakPoint = computed(() => {
    const list = this.revenueTrend();
    if (!list || list.length === 0) return null;
    return list.reduce((max, curr) => (Number(curr.revenue) > Number(max.revenue) ? curr : max), list[0]);
  });

  readonly peakRevenue = computed(() => {
    return Number(this.peakPoint()?.revenue) || 0;
  });

  // Y-Axis grid lines with formatted labels
  readonly yGridLines = computed<YAxisGuide[]>(() => {
    const list = this.revenueTrend();
    const maxRev = Math.max(...list.map(d => Number(d.revenue) || 0), 1000000);
    const usableHeight = this.chartHeight - this.paddingTop - this.paddingBottom;
    const steps = 3;
    const lines: YAxisGuide[] = [];

    for (let i = 0; i <= steps; i++) {
      const val = (maxRev / steps) * (steps - i);
      const y = this.paddingTop + (i / steps) * usableHeight;
      lines.push({ y, label: this.formatShortCurrency(val) });
    }
    return lines;
  });

  // X-Axis sample labels
  readonly xGridLabels = computed<XAxisGuide[]>(() => {
    const pts = this.chartPoints();
    if (pts.length === 0) return [];
    if (pts.length <= 6) {
      return pts.map(p => ({ x: p.x, label: this.formatDateLabel(p.data.dateLabel) }));
    }
    const count = 5;
    const step = (pts.length - 1) / (count - 1);
    const result: XAxisGuide[] = [];
    for (let i = 0; i < count; i++) {
      const idx = Math.min(Math.round(i * step), pts.length - 1);
      result.push({ x: pts[idx].x, label: this.formatDateLabel(pts[idx].data.dateLabel) });
    }
    return result;
  });

  // Bar representation for Bar chart mode
  readonly chartBars = computed<DashboardChartBar[]>(() => {
    const data = this.revenueTrend();
    if (!data || data.length === 0) return [];

    const usableWidth = this.chartWidth - this.paddingLeft - this.paddingRight;
    const usableHeight = this.chartHeight - this.paddingTop - this.paddingBottom;
    const maxRev = Math.max(...data.map(d => Number(d.revenue) || 0), 1000000);
    const stepX = usableWidth / data.length;
    const barWidth = Math.max(Math.min(stepX * 0.65, 26), 6);

    return data.map((d, i) => {
      const rev = Number(d.revenue) || 0;
      const h = Math.max((rev / maxRev) * usableHeight, rev > 0 ? 4 : 2);
      const x = this.paddingLeft + i * stepX + (stepX - barWidth) / 2;
      const y = this.chartHeight - this.paddingBottom - h;
      return { x, y, width: barWidth, height: h, data: d };
    });
  });

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

    // 2. Revenue Trend
    this.loadRevenueTrend();

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

  loadRevenueTrend(): void {
    this.isTrendLoading.set(true);
    const today = new Date().toISOString().split('T')[0];
    const days = this.selectedPeriod() === '7d' ? 7 : 30;
    const startDate = new Date(Date.now() - (days - 1) * 24 * 60 * 60 * 1000).toISOString().split('T')[0];

    this.statisticsService.getRevenueTrend('day', startDate, today).subscribe({
      next: (res) => {
        if (res.data && res.data.length > 0) {
          this.revenueTrend.set(res.data);
          this.generateChartPath(res.data);
        } else {
          this.revenueTrend.set([]);
          this.chartPoints.set([]);
        }
        this.isTrendLoading.set(false);
      },
      error: (err) => {
        console.error('Failed to load trend', err);
        this.isTrendLoading.set(false);
      }
    });
  }

  setPeriod(period: '7d' | '30d'): void {
    if (this.selectedPeriod() === period) return;
    this.selectedPeriod.set(period);
    this.loadRevenueTrend();
  }

  setChartType(type: 'area' | 'bar'): void {
    this.chartType.set(type);
  }

  private generateChartPath(data: RevenueChartDataPoint[]): void {
    if (!data || data.length === 0) return;

    const usableWidth = this.chartWidth - this.paddingLeft - this.paddingRight;
    const usableHeight = this.chartHeight - this.paddingTop - this.paddingBottom;
    const baseFloorY = this.chartHeight - this.paddingBottom;

    const maxRev = Math.max(...data.map(d => Number(d.revenue) || 0), 1000000);
    const stepX = usableWidth / Math.max(data.length - 1, 1);

    const points: DashboardChartPoint[] = data.map((d, i) => {
      const x = this.paddingLeft + i * stepX;
      const normalizedY = (Number(d.revenue) || 0) / maxRev;
      const y = baseFloorY - (normalizedY * usableHeight);
      return { x, y, data: d };
    });

    this.chartPoints.set(points);

    if (points.length === 1) {
      this.chartPath.set(`M ${points[0].x} ${points[0].y} L ${this.chartWidth - this.paddingRight} ${points[0].y}`);
      this.chartAreaPath.set(`M ${points[0].x} ${points[0].y} L ${this.chartWidth - this.paddingRight} ${points[0].y} L ${this.chartWidth - this.paddingRight} ${baseFloorY} L ${points[0].x} ${baseFloorY} Z`);
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
    const lastX = points[points.length - 1].x.toFixed(1);
    const firstX = points[0].x.toFixed(1);
    this.chartAreaPath.set(`${d} L ${lastX} ${baseFloorY} L ${firstX} ${baseFloorY} Z`);
  }

  onPointHover(pt: DashboardChartPoint): void {
    this.activeChartPoint.set(pt);
    this.hoveredPoint.set(pt.data);
  }

  onBarHover(bar: DashboardChartBar): void {
    const pt: DashboardChartPoint = {
      x: bar.x + bar.width / 2,
      y: bar.y,
      data: bar.data
    };
    this.activeChartPoint.set(pt);
    this.hoveredPoint.set(bar.data);
  }

  onPointLeave(): void {
    this.hoveredPoint.set(null);
    this.activeChartPoint.set(null);
  }

  formatShortCurrency(val: number): string {
    if (val >= 1_000_000_000) return (val / 1_000_000_000).toFixed(1).replace(/\.0$/, '') + ' tỷ';
    if (val >= 1_000_000) return (val / 1_000_000).toFixed(1).replace(/\.0$/, '') + ' tr';
    if (val >= 1_000) return (val / 1_000).toFixed(0) + ' k';
    return val.toString() + ' ₫';
  }

  formatDateLabel(dateStr: string): string {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      return `${parts[2]}/${parts[1]}`;
    }
    return dateStr;
  }

  getTooltipTop(y: number): number {
    return Math.max(y - 65, 10);
  }

  getTooltipLeft(x: number): number {
    return Math.max(Math.min((x / this.chartWidth) * 100, 90), 10);
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
