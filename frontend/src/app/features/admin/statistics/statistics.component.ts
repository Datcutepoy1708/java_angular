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

export interface SvgPoint {
  x: number;
  y: number;
  data: RevenueChartDataPoint;
}

export interface SvgBar {
  x: number;
  y: number;
  width: number;
  height: number;
  data: RevenueChartDataPoint;
}

export interface DonutSlice {
  category: CategoryRevenueShare;
  strokeDasharray: string;
  strokeDashoffset: number;
  color: string;
  percentage: number;
}

export interface YAxisTick {
  y: number;
  label: string;
  value: number;
}

export interface XAxisTick {
  x: number;
  label: string;
}

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

  // Chart type: 'area' (Smooth Wave), 'bar' (Modern Rounded Bar), 'combo' (Dual Axis)
  readonly chartType = signal<'area' | 'bar' | 'combo'>('area');

  // Chart interactivity
  readonly hoveredPoint = signal<RevenueChartDataPoint | null>(null);
  readonly tooltipPos = signal<{ x: number; y: number }>({ x: 0, y: 0 });
  readonly activeSlice = signal<CategoryRevenueShare | null>(null);

  // SVG Chart Dimensions
  readonly chartWidth = 840;
  readonly chartHeight = 280;
  readonly paddingLeft = 75;
  readonly paddingRight = 30;
  readonly paddingTop = 30;
  readonly paddingBottom = 45;

  // Modern vibrant color palette for Donut & Categories
  readonly colorPalette = [
    '#3b82f6', // Electric Blue
    '#10b981', // Emerald
    '#f59e0b', // Amber
    '#8b5cf6', // Violet
    '#ec4899', // Pink
    '#06b6d4', // Cyan
    '#f97316', // Orange
    '#6366f1'  // Indigo
  ];

  // Computed total order status count
  readonly totalStatusOrders = computed(() => {
    const dist = this.orderStatusDist();
    return Object.values(dist).reduce((sum, count) => sum + count, 0);
  });

  // Computed max revenue for Y-scale
  readonly maxRevenue = computed(() => {
    const data = this.revenueTrend();
    if (!data || data.length === 0) return 10000000;
    const maxVal = Math.max(...data.map(d => Number(d.revenue) || 0));
    return maxVal > 0 ? maxVal * 1.15 : 10000000;
  });

  // Computed max orders for combo Y2-scale
  readonly maxOrders = computed(() => {
    const data = this.revenueTrend();
    if (!data || data.length === 0) return 10;
    const maxVal = Math.max(...data.map(d => Number(d.orderCount) || 0));
    return maxVal > 0 ? Math.ceil(maxVal * 1.25) : 10;
  });

  // Total Category Revenue
  readonly totalCategoryRevenue = computed(() => {
    return this.categoryShares().reduce((sum, c) => sum + (Number(c.revenue) || 0), 0);
  });

  // Y-Axis Ticks (4 levels: 0, 33%, 66%, 100%)
  readonly yAxisTicks = computed<YAxisTick[]>(() => {
    const max = this.maxRevenue();
    const plotH = this.chartHeight - this.paddingTop - this.paddingBottom;
    const bottomY = this.chartHeight - this.paddingBottom;

    const ticks: YAxisTick[] = [];
    const steps = 4;
    for (let i = 0; i <= steps; i++) {
      const val = (max / steps) * i;
      const y = bottomY - (val / max) * plotH;
      ticks.push({
        y,
        value: val,
        label: this.formatShortCurrency(val)
      });
    }
    return ticks;
  });

  // X-Axis Ticks (smart sampling 5 to 7 dates)
  readonly xAxisTicks = computed<XAxisTick[]>(() => {
    const data = this.revenueTrend();
    if (!data || data.length === 0) return [];

    const plotW = this.chartWidth - this.paddingLeft - this.paddingRight;
    const count = data.length;

    if (count <= 7) {
      return data.map((d, i) => ({
        x: this.paddingLeft + (count === 1 ? plotW / 2 : (i / (count - 1)) * plotW),
        label: this.formatDateLabel(d.dateLabel)
      }));
    }

    const stepIndex = Math.ceil(count / 6);
    const sampled: XAxisTick[] = [];
    for (let i = 0; i < count; i += stepIndex) {
      sampled.push({
        x: this.paddingLeft + (i / (count - 1)) * plotW,
        label: this.formatDateLabel(data[i].dateLabel)
      });
    }
    // Ensure the last date is included
    const lastX = this.paddingLeft + plotW;
    const lastLabel = this.formatDateLabel(data[count - 1].dateLabel);
    if (sampled.length > 0 && Math.abs(sampled[sampled.length - 1].x - lastX) > 40) {
      sampled.push({ x: lastX, label: lastLabel });
    }
    return sampled;
  });

  // Donut slices computation (Circumference = 2 * PI * 80 ≈ 502.65)
  readonly donutCircumference = 502.65;
  readonly donutRadius = 80;
  readonly donutSlices = computed<DonutSlice[]>(() => {
    const categories = this.categoryShares();
    if (!categories || categories.length === 0) return [];

    let accumulatedPercentage = 0;
    return categories.map((cat, idx) => {
      const pct = Math.max(0, cat.percentageShare || 0);
      const strokeDashoffset = -((accumulatedPercentage / 100) * this.donutCircumference);
      accumulatedPercentage += pct;

      // Slices with tiny gap for modern look
      const arcLength = Math.max(0, (pct / 100) * this.donutCircumference - 2);
      const strokeDasharray = `${arcLength} ${this.donutCircumference - arcLength}`;

      return {
        category: cat,
        strokeDasharray,
        strokeDashoffset,
        color: this.colorPalette[idx % this.colorPalette.length],
        percentage: pct
      };
    });
  });

  ngOnInit(): void {
    this.applyPeriod('30days');
  }

  setChartType(type: 'area' | 'bar' | 'combo'): void {
    this.chartType.set(type);
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

    this.statisticsService.getOverview(startDate, endDate).subscribe({
      next: (res) => { if (res.data) this.overview.set(res.data); },
      error: (err) => console.error('Overview error', err)
    });

    this.statisticsService.getRevenueTrend(period, startDate, endDate).subscribe({
      next: (res) => { if (res.data) this.revenueTrend.set(res.data); },
      error: (err) => console.error('Trend error', err)
    });

    this.statisticsService.getTopSelling(10, startDate, endDate).subscribe({
      next: (res) => { if (res.data) this.topSellers.set(res.data); },
      error: (err) => console.error('Top selling error', err)
    });

    this.statisticsService.getOrderStatusDistribution(startDate, endDate).subscribe({
      next: (res) => { if (res.data) this.orderStatusDist.set(res.data); },
      error: (err) => console.error('Status error', err)
    });

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

  // SVG Chart Helpers (Line / Area)
  getSvgPoints(): SvgPoint[] {
    const data = this.revenueTrend();
    if (!data || data.length === 0) return [];

    const width = this.chartWidth;
    const height = this.chartHeight;
    const pL = this.paddingLeft;
    const pR = this.paddingRight;
    const pT = this.paddingTop;
    const pB = this.paddingBottom;

    const plotW = width - pL - pR;
    const plotH = height - pT - pB;
    const maxRev = this.maxRevenue();
    const count = data.length;

    return data.map((d, i) => {
      const x = pL + (count === 1 ? plotW / 2 : (i / (count - 1)) * plotW);
      const rev = Number(d.revenue) || 0;
      const y = (height - pB) - (rev / maxRev) * plotH;
      return { x, y, data: d };
    });
  }

  getSvgPath(): string {
    const points = this.getSvgPoints();
    if (points.length === 0) return '';
    if (points.length === 1) return `M ${points[0].x} ${points[0].y} L ${this.chartWidth - this.paddingRight} ${points[0].y}`;

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
    const bottomY = this.chartHeight - this.paddingBottom;
    return `${linePath} L ${lastX} ${bottomY} L ${firstX} ${bottomY} Z`;
  }

  // SVG Bar Chart Helpers
  getSvgBars(): SvgBar[] {
    const data = this.revenueTrend();
    if (!data || data.length === 0) return [];

    const plotW = this.chartWidth - this.paddingLeft - this.paddingRight;
    const plotH = this.chartHeight - this.paddingTop - this.paddingBottom;
    const bottomY = this.chartHeight - this.paddingBottom;
    const maxRev = this.maxRevenue();
    const count = data.length;

    const availableSlot = plotW / Math.max(count, 1);
    const barWidth = Math.max(8, Math.min(32, availableSlot * 0.65));

    return data.map((d, i) => {
      const centerX = this.paddingLeft + (i + 0.5) * availableSlot;
      const rev = Number(d.revenue) || 0;
      const barH = Math.max(3, (rev / maxRev) * plotH);
      const x = centerX - barWidth / 2;
      const y = bottomY - barH;
      return { x, y, width: barWidth, height: barH, data: d };
    });
  }

  // SVG Combo Order Bars (Secondary metric in combo view)
  getComboOrderBars(): SvgBar[] {
    const data = this.revenueTrend();
    if (!data || data.length === 0) return [];

    const plotW = this.chartWidth - this.paddingLeft - this.paddingRight;
    const plotH = this.chartHeight - this.paddingTop - this.paddingBottom;
    const bottomY = this.chartHeight - this.paddingBottom;
    const maxOrders = this.maxOrders();
    const count = data.length;

    const availableSlot = plotW / Math.max(count, 1);
    const barWidth = Math.max(6, Math.min(24, availableSlot * 0.5));

    return data.map((d, i) => {
      const centerX = this.paddingLeft + (i + 0.5) * availableSlot;
      const orders = Number(d.orderCount) || 0;
      const barH = Math.max(2, (orders / maxOrders) * (plotH * 0.7)); // Scale to max 70% height
      const x = centerX - barWidth / 2;
      const y = bottomY - barH;
      return { x, y, width: barWidth, height: barH, data: d };
    });
  }

  onPointHover(pt: SvgPoint): void {
    this.hoveredPoint.set(pt.data);
    this.tooltipPos.set({ x: pt.x, y: pt.y });
  }

  onBarHover(bar: SvgBar): void {
    this.hoveredPoint.set(bar.data);
    this.tooltipPos.set({ x: bar.x + bar.width / 2, y: bar.y });
  }

  onPointLeave(): void {
    this.hoveredPoint.set(null);
  }

  onSliceHover(category: CategoryRevenueShare): void {
    this.activeSlice.set(category);
  }

  onSliceLeave(): void {
    this.activeSlice.set(null);
  }

  formatCurrency(value: number | undefined): string {
    if (value === undefined || value === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
  }

  formatShortCurrency(val: number): string {
    if (val >= 1_000_000_000) return (val / 1_000_000_000).toFixed(1).replace(/\.0$/, '') + ' Tỷ';
    if (val >= 1_000_000) return (val / 1_000_000).toFixed(1).replace(/\.0$/, '') + ' Tr';
    if (val >= 1_000) return (val / 1_000).toFixed(0) + ' K';
    return val.toFixed(0) + ' ₫';
  }

  formatDateLabel(dateStr: string): string {
    if (!dateStr) return '';
    // Format YYYY-MM-DD or YYYY-MM to DD/MM or T.MM
    const parts = dateStr.split('-');
    if (parts.length === 3) return `${parts[2]}/${parts[1]}`;
    if (parts.length === 2) return `T${parts[1]}`;
    return dateStr;
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

  getStatusColor(status: string): string {
    const map: Record<string, string> = {
      PENDING: '#f59e0b',
      CONFIRMED: '#3b82f6',
      PROCESSING: '#8b5cf6',
      SHIPPING: '#06b6d4',
      DELIVERED: '#10b981',
      COMPLETED: '#059669',
      CANCELLED: '#ef4444',
      REFUNDED: '#64748b'
    };
    return map[status] || '#94a3b8';
  }
}

