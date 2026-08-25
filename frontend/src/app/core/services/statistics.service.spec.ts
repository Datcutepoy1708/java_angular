import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { StatisticsService } from './statistics.service';
import { environment } from '../../../environments/environment';
import { DashboardOverview, RevenueChartDataPoint, TopSellingProduct } from '../models/statistics.model';

describe('StatisticsService', () => {
  let service: StatisticsService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/admin/statistics`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        StatisticsService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(StatisticsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should get overview KPIs with date params', () => {
    const mockOverview: DashboardOverview = {
      totalRevenue: 150000000,
      revenueGrowthPercent: 15.5,
      totalOrders: 45,
      ordersGrowthPercent: 8.2,
      averageOrderValue: 3333333,
      newCustomers: 12,
      customerGrowthPercent: 20.0,
      lowStockCount: 3
    };

    service.getOverview('2026-08-01', '2026-08-25').subscribe((res) => {
      expect(res.data.totalRevenue).toBe(150000000);
      expect(res.data.totalOrders).toBe(45);
      expect(res.data.revenueGrowthPercent).toBe(15.5);
    });

    const req = httpMock.expectOne((r) => r.url === `${baseUrl}/overview` && r.params.get('startDate') === '2026-08-01');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockOverview });
  });

  it('should get revenue trend data points', () => {
    const mockTrend: RevenueChartDataPoint[] = [
      { dateLabel: '2026-08-20', revenue: 20000000, orderCount: 5 },
      { dateLabel: '2026-08-21', revenue: 35000000, orderCount: 8 }
    ];

    service.getRevenueTrend('day', '2026-08-20', '2026-08-21').subscribe((res) => {
      expect(res.data.length).toBe(2);
      expect(res.data[0].revenue).toBe(20000000);
    });

    const req = httpMock.expectOne((r) => r.url === `${baseUrl}/revenue-trend` && r.params.get('period') === 'day');
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockTrend });
  });

  it('should request refresh cache via POST', () => {
    service.refreshCache().subscribe((res) => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/refresh-cache`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, message: 'Cache evicted', data: null });
  });
});
