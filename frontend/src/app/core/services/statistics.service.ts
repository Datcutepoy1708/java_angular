import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';
import {
  CategoryRevenueShare,
  DashboardOverview,
  RevenueChartDataPoint,
  TopSellingProduct
} from '../models/statistics.model';

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/admin/statistics`;

  getOverview(startDate?: string, endDate?: string): Observable<ApiResponse<DashboardOverview>> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<DashboardOverview>>(`${this.baseUrl}/overview`, { params });
  }

  getRevenueTrend(period: string = 'day', startDate?: string, endDate?: string): Observable<ApiResponse<RevenueChartDataPoint[]>> {
    let params = new HttpParams().set('period', period);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<RevenueChartDataPoint[]>>(`${this.baseUrl}/revenue-trend`, { params });
  }

  getTopSelling(limit: number = 10, startDate?: string, endDate?: string): Observable<ApiResponse<TopSellingProduct[]>> {
    let params = new HttpParams().set('limit', limit.toString());
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<TopSellingProduct[]>>(`${this.baseUrl}/top-selling`, { params });
  }

  getOrderStatusDistribution(startDate?: string, endDate?: string): Observable<ApiResponse<Record<string, number>>> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<Record<string, number>>>(`${this.baseUrl}/order-status`, { params });
  }

  getCategoryShare(startDate?: string, endDate?: string): Observable<ApiResponse<CategoryRevenueShare[]>> {
    let params = new HttpParams();
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);
    return this.http.get<ApiResponse<CategoryRevenueShare[]>>(`${this.baseUrl}/category-share`, { params });
  }

  refreshCache(): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/refresh-cache`, {});
  }
}
