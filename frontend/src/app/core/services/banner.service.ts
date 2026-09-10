import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, switchMap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';
import { Banner, BannerPosition, BannerRequest } from '../models/banner.model';

@Injectable({
  providedIn: 'root'
})
export class BannerService {
  setStatus(id: number, status: 'active' | 'inactive'): Observable<ApiResponse<Banner>> {
    return this.getBannerById(id).pipe(switchMap(response => {
      if (!response.success || !response.data) throw new Error(response.message || 'Không tìm thấy banner');
      const { title, imageUrl, linkUrl, position, sortOrder, startDate, endDate } = response.data;
      return this.updateBanner(id, { title, imageUrl, linkUrl, position, sortOrder, startDate, endDate, status });
    }));
  }
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  getPublicBanners(position?: BannerPosition): Observable<ApiResponse<Banner[]>> {
    let params = new HttpParams();
    if (position) {
      params = params.set('position', position);
    }
    return this.http.get<ApiResponse<Banner[]>>(`${this.baseUrl}/api/v1/banners/public`, { params });
  }

  getAdminBanners(position?: BannerPosition): Observable<ApiResponse<Banner[]>> {
    let params = new HttpParams();
    if (position) {
      params = params.set('position', position);
    }
    return this.http.get<ApiResponse<Banner[]>>(`${this.baseUrl}/api/v1/admin/banners`, { params });
  }

  getBannerById(bannerId: number): Observable<ApiResponse<Banner>> {
    return this.http.get<ApiResponse<Banner>>(`${this.baseUrl}/api/v1/admin/banners/${bannerId}`);
  }

  createBanner(request: BannerRequest): Observable<ApiResponse<Banner>> {
    return this.http.post<ApiResponse<Banner>>(`${this.baseUrl}/api/v1/admin/banners`, request);
  }

  updateBanner(bannerId: number, request: BannerRequest): Observable<ApiResponse<Banner>> {
    return this.http.put<ApiResponse<Banner>>(`${this.baseUrl}/api/v1/admin/banners/${bannerId}`, request);
  }

  deleteBanner(bannerId: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${this.baseUrl}/api/v1/admin/banners/${bannerId}`);
  }
}
