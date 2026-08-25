import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { StatisticsService } from '../../../core/services/statistics.service';
import { OrderService } from '../../../core/services/order.service';
import { of } from 'rxjs';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { provideRouter } from '@angular/router';
import { DashboardOverview } from '../../../core/models/statistics.model';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;

  const mockOverview: DashboardOverview = {
    totalRevenue: 50000000,
    revenueGrowthPercent: 10.0,
    totalOrders: 20,
    ordersGrowthPercent: 5.0,
    averageOrderValue: 2500000,
    newCustomers: 8,
    customerGrowthPercent: 12.0,
    lowStockCount: 1
  };

  beforeEach(async () => {
    const statisticsServiceMock = {
      getOverview: vi.fn().mockReturnValue(of({ success: true, data: mockOverview })),
      getRevenueTrend: vi.fn().mockReturnValue(of({ success: true, data: [] })),
      getTopSelling: vi.fn().mockReturnValue(of({ success: true, data: [] }))
    };

    const orderServiceMock = {
      getAdminOrders: vi.fn().mockReturnValue(of({ success: true, data: { content: [], totalElements: 0 } }))
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: StatisticsService, useValue: statisticsServiceMock },
        { provide: OrderService, useValue: orderServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize and load dashboard overview', () => {
    expect(component).toBeTruthy();
    expect(component.overview()?.totalRevenue).toBe(50000000);
  });
});
