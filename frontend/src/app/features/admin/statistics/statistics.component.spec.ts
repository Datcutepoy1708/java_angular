import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatisticsComponent } from './statistics.component';
import { StatisticsService } from '../../../core/services/statistics.service';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { DashboardOverview, RevenueChartDataPoint, TopSellingProduct } from '../../../core/models/statistics.model';

describe('StatisticsComponent', () => {
  let component: StatisticsComponent;
  let fixture: ComponentFixture<StatisticsComponent>;
  let statisticsServiceMock: Partial<StatisticsService>;

  const mockOverview: DashboardOverview = {
    totalRevenue: 200000000,
    revenueGrowthPercent: 12.0,
    totalOrders: 60,
    ordersGrowthPercent: 5.0,
    averageOrderValue: 3333333,
    newCustomers: 15,
    customerGrowthPercent: 10.0,
    lowStockCount: 2
  };

  const mockTrend: RevenueChartDataPoint[] = [
    { dateLabel: '2026-08-20', revenue: 10000000, orderCount: 2 },
    { dateLabel: '2026-08-21', revenue: 25000000, orderCount: 5 }
  ];

  const mockTopSellers: TopSellingProduct[] = [
    {
      productId: 1,
      productName: 'RTX 4070 Super',
      productSlug: 'rtx-4070-super',
      categoryName: 'VGA',
      thumbnailImage: null,
      totalQuantitySold: 12,
      totalRevenueGenerated: 216000000
    }
  ];

  beforeEach(async () => {
    statisticsServiceMock = {
      getOverview: vi.fn().mockReturnValue(of({ success: true, data: mockOverview })),
      getRevenueTrend: vi.fn().mockReturnValue(of({ success: true, data: mockTrend })),
      getTopSelling: vi.fn().mockReturnValue(of({ success: true, data: mockTopSellers })),
      getOrderStatusDistribution: vi.fn().mockReturnValue(of({ success: true, data: { COMPLETED: 50, PENDING: 10 } })),
      getCategoryShare: vi.fn().mockReturnValue(of({ success: true, data: [{ categoryId: 1, categoryName: 'VGA', revenue: 200000000, percentageShare: 100 }] })),
      refreshCache: vi.fn().mockReturnValue(of({ success: true, data: null }))
    };

    await TestBed.configureTestingModule({
      imports: [StatisticsComponent],
      providers: [
        { provide: StatisticsService, useValue: statisticsServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(StatisticsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize and load statistics for 30days default', () => {
    expect(component).toBeTruthy();
    expect(component.activePeriod()).toBe('30days');
    expect(component.overview()?.totalRevenue).toBe(200000000);
    expect(component.revenueTrend().length).toBe(2);
    expect(component.topSellers().length).toBe(1);
  });

  it('should switch period filter and reload data', () => {
    component.applyPeriod('7days');
    expect(component.activePeriod()).toBe('7days');
    expect(statisticsServiceMock.getOverview).toHaveBeenCalled();
  });

  it('should calculate SVG points and paths without crashing', () => {
    const points = component.getSvgPoints();
    expect(points.length).toBe(2);
    const path = component.getSvgPath();
    expect(path).toContain('M');
  });
});
