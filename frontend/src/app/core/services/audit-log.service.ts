import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuditLogFilter, AuditLogItem, AuditLogPageResponse } from '../models/audit-log.model';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {
  private readonly baseUrl = `${environment.apiUrl}/api/v1/admin/audit-logs`;

  constructor(private readonly http: HttpClient) {}

  getAuditLogs(filter: AuditLogFilter = {}): Observable<ApiResponse<AuditLogPageResponse>> {
    let params = new HttpParams();

    if (filter.keyword) params = params.set('keyword', filter.keyword);
    if (filter.module) params = params.set('module', filter.module);
    if (filter.actionType) params = params.set('actionType', filter.actionType);
    if (filter.userId) params = params.set('userId', filter.userId.toString());
    if (filter.status) params = params.set('status', filter.status);
    if (filter.fromDate) params = params.set('fromDate', filter.fromDate);
    if (filter.toDate) params = params.set('toDate', filter.toDate);
    if (filter.page !== undefined) params = params.set('page', filter.page.toString());
    if (filter.size !== undefined) params = params.set('size', filter.size.toString());
    if (filter.sortBy) params = params.set('sortBy', filter.sortBy);
    if (filter.sortDirection) params = params.set('sortDirection', filter.sortDirection);

    return this.http.get<ApiResponse<AuditLogPageResponse>>(this.baseUrl, { params });
  }

  getAuditLogById(logId: number): Observable<ApiResponse<AuditLogItem>> {
    return this.http.get<ApiResponse<AuditLogItem>>(`${this.baseUrl}/${logId}`);
  }

  exportAuditLogsToCsv(filter: AuditLogFilter = {}): Observable<Blob> {
    let params = new HttpParams();

    if (filter.keyword) params = params.set('keyword', filter.keyword);
    if (filter.module) params = params.set('module', filter.module);
    if (filter.actionType) params = params.set('actionType', filter.actionType);
    if (filter.userId) params = params.set('userId', filter.userId.toString());
    if (filter.status) params = params.set('status', filter.status);
    if (filter.fromDate) params = params.set('fromDate', filter.fromDate);
    if (filter.toDate) params = params.set('toDate', filter.toDate);

    return this.http.get(`${this.baseUrl}/export`, {
      params,
      responseType: 'blob'
    });
  }
}
