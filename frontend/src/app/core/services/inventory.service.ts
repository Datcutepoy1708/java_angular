import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  InventoryFilterParams,
  InventoryItem,
  InventoryLog,
  InventoryLogFilterParams,
  InventoryMetrics,
  StockAdjustmentRequest,
  StockImportRequest,
  StockTransferRequest,
  VariantStockSummary,
  Warehouse,
} from '../models/inventory.model';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  pageSize: number;
  pageNumber: number;
  last: boolean;
}

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/api/v1/inventory`;
  private readonly warehouseBase = `${environment.apiUrl}/api/v1/warehouses`;

  getWarehouses(): Observable<ApiResponse<Warehouse[]>> {
    return this.http.get<ApiResponse<Warehouse[]>>(this.warehouseBase);
  }

  getWarehouseById(id: number): Observable<ApiResponse<Warehouse>> {
    return this.http.get<ApiResponse<Warehouse>>(`${this.warehouseBase}/${id}`);
  }

  getInventory(params?: InventoryFilterParams): Observable<ApiResponse<PageResponse<InventoryItem>>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.warehouseId) httpParams = httpParams.set('warehouseId', params.warehouseId);
      if (params.keyword?.trim()) httpParams = httpParams.set('keyword', params.keyword.trim());
      if (params.stockStatus && params.stockStatus !== 'ALL') httpParams = httpParams.set('stockStatus', params.stockStatus);
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
      if (params.sortBy) httpParams = httpParams.set('sortBy', params.sortBy);
      if (params.sortDir) httpParams = httpParams.set('sortDir', params.sortDir);
    }
    return this.http.get<ApiResponse<PageResponse<InventoryItem>>>(this.base, { params: httpParams });
  }

  getMetrics(): Observable<ApiResponse<InventoryMetrics>> {
    return this.http.get<ApiResponse<InventoryMetrics>>(`${this.base}/metrics`);
  }

  getVariantStockSummary(variantId: number): Observable<ApiResponse<VariantStockSummary>> {
    return this.http.get<ApiResponse<VariantStockSummary>>(`${this.base}/variants/${variantId}/summary`);
  }

  getProductStockSummary(productId: number): Observable<ApiResponse<VariantStockSummary[]>> {
    return this.http.get<ApiResponse<VariantStockSummary[]>>(`${this.base}/products/${productId}/stock`);
  }

  adjustStock(request: StockAdjustmentRequest): Observable<ApiResponse<InventoryItem>> {
    return this.http.post<ApiResponse<InventoryItem>>(`${this.base}/adjust`, request);
  }

  importStock(request: StockImportRequest): Observable<ApiResponse<InventoryItem[]>> {
    return this.http.post<ApiResponse<InventoryItem[]>>(`${this.base}/import`, request);
  }

  transferStock(request: StockTransferRequest): Observable<ApiResponse<InventoryItem[]>> {
    return this.http.post<ApiResponse<InventoryItem[]>>(`${this.base}/transfer`, request);
  }

  getLowStockAlerts(threshold = 10, page = 0, size = 20): Observable<ApiResponse<PageResponse<InventoryItem>>> {
    const params = new HttpParams()
      .set('threshold', threshold)
      .set('page', page)
      .set('size', size);
    return this.http.get<ApiResponse<PageResponse<InventoryItem>>>(`${this.base}/low-stock`, { params });
  }

  getLogs(params?: InventoryLogFilterParams): Observable<ApiResponse<PageResponse<InventoryLog>>> {
    let httpParams = new HttpParams();
    if (params) {
      if (params.variantId) httpParams = httpParams.set('variantId', params.variantId);
      if (params.warehouseId) httpParams = httpParams.set('warehouseId', params.warehouseId);
      if (params.keyword?.trim()) httpParams = httpParams.set('keyword', params.keyword.trim());
      if (params.page !== undefined) httpParams = httpParams.set('page', params.page);
      if (params.size !== undefined) httpParams = httpParams.set('size', params.size);
    }
    return this.http.get<ApiResponse<PageResponse<InventoryLog>>>(`${this.base}/logs`, { params: httpParams });
  }
}
