import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ReportExportService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/admin/reports`;

  exportRevenueExcel(startDate?: string, endDate?: string, period: string = 'day'): Observable<Blob> {
    let params = new HttpParams().set('period', period);
    if (startDate) params = params.set('startDate', startDate);
    if (endDate) params = params.set('endDate', endDate);

    return this.http.get(`${this.baseUrl}/revenue/excel`, {
      params,
      responseType: 'blob'
    });
  }

  exportOrdersExcel(filter?: {
    keyword?: string;
    status?: string;
    paymentStatus?: string;
    startDate?: string;
    endDate?: string;
  }): Observable<Blob> {
    let params = new HttpParams();
    if (filter?.keyword) params = params.set('keyword', filter.keyword);
    if (filter?.status) params = params.set('status', filter.status);
    if (filter?.paymentStatus) params = params.set('paymentStatus', filter.paymentStatus);
    if (filter?.startDate) params = params.set('startDate', filter.startDate);
    if (filter?.endDate) params = params.set('endDate', filter.endDate);

    return this.http.get(`${this.baseUrl}/orders/excel`, {
      params,
      responseType: 'blob'
    });
  }

  exportInventoryExcel(
    warehouseId?: number,
    keyword?: string,
    stockStatus: string = 'ALL'
  ): Observable<Blob> {
    let params = new HttpParams().set('stockStatus', stockStatus);
    if (warehouseId) params = params.set('warehouseId', warehouseId.toString());
    if (keyword) params = params.set('keyword', keyword);

    return this.http.get(`${this.baseUrl}/inventory/excel`, {
      params,
      responseType: 'blob'
    });
  }

  downloadBlob(blob: Blob, defaultFilename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = defaultFilename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }
}
