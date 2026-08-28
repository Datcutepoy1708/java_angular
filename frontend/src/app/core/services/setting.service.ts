import { inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';
import {
  DEFAULT_PUBLIC_SETTINGS,
  GroupedSettings,
  PublicSettings,
  SettingItem,
  UpdateSettingsPayload
} from '../models/setting.model';

@Injectable({
  providedIn: 'root'
})
export class SettingService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiUrl;

  readonly publicSettings = signal<PublicSettings>(DEFAULT_PUBLIC_SETTINGS);
  readonly isLoaded = signal<boolean>(false);

  loadPublicSettings(): Observable<PublicSettings> {
    return this.http
      .get<ApiResponse<Record<string, string>>>(`${this.baseUrl}/api/v1/settings/public`)
      .pipe(
        map(res => this.parsePublicSettings(res.data || {})),
        tap(parsed => {
          this.publicSettings.set(parsed);
          this.isLoaded.set(true);
        })
      );
  }

  getAllSettings(): Observable<ApiResponse<SettingItem[]>> {
    return this.http.get<ApiResponse<SettingItem[]>>(`${this.baseUrl}/api/v1/settings`);
  }

  getGroupedSettings(): Observable<ApiResponse<GroupedSettings>> {
    return this.http.get<ApiResponse<GroupedSettings>>(`${this.baseUrl}/api/v1/settings/grouped`);
  }

  updateSettings(payload: UpdateSettingsPayload): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${this.baseUrl}/api/v1/settings`, payload).pipe(
      tap(() => {
        // Automatically refresh public settings state in memory
        this.loadPublicSettings().subscribe();
      })
    );
  }

  resetDefaults(): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${this.baseUrl}/api/v1/settings/reset-defaults`, {}).pipe(
      tap(() => {
        this.loadPublicSettings().subscribe();
      })
    );
  }

  private parsePublicSettings(raw: Record<string, string>): PublicSettings {
    const parseNumber = (val: string | undefined, fallback: number): number => {
      if (!val) return fallback;
      const num = Number(val);
      return isNaN(num) ? fallback : num;
    };

    const parseBoolean = (val: string | undefined, fallback: boolean): boolean => {
      if (val === undefined || val === null || val === '') return fallback;
      return val.trim().toLowerCase() === 'true';
    };

    return {
      storeName: raw['STORE_NAME'] || DEFAULT_PUBLIC_SETTINGS.storeName,
      storeSlogan: raw['STORE_SLOGAN'] || DEFAULT_PUBLIC_SETTINGS.storeSlogan,
      storeHotline: raw['STORE_HOTLINE'] || DEFAULT_PUBLIC_SETTINGS.storeHotline,
      storeEmail: raw['STORE_EMAIL'] || DEFAULT_PUBLIC_SETTINGS.storeEmail,
      storeAddress: raw['STORE_ADDRESS'] || DEFAULT_PUBLIC_SETTINGS.storeAddress,
      storeWorkingHours: raw['STORE_WORKING_HOURS'] || DEFAULT_PUBLIC_SETTINGS.storeWorkingHours,
      footerBrandTitle: raw['FOOTER_BRAND_TITLE'] || DEFAULT_PUBLIC_SETTINGS.footerBrandTitle,
      footerDescription: raw['FOOTER_DESCRIPTION'] || DEFAULT_PUBLIC_SETTINGS.footerDescription,
      footerHotline: raw['FOOTER_HOTLINE'] || DEFAULT_PUBLIC_SETTINGS.footerHotline,
      footerEmail: raw['FOOTER_EMAIL'] || DEFAULT_PUBLIC_SETTINGS.footerEmail,
      footerAddress: raw['FOOTER_ADDRESS'] || DEFAULT_PUBLIC_SETTINGS.footerAddress,
      footerCopyright: raw['FOOTER_COPYRIGHT'] || DEFAULT_PUBLIC_SETTINGS.footerCopyright,
      footerBusinessLicense: raw['FOOTER_BUSINESS_LICENSE'] || DEFAULT_PUBLIC_SETTINGS.footerBusinessLicense,
      footerFacebookUrl: raw['FOOTER_FACEBOOK_URL'] || DEFAULT_PUBLIC_SETTINGS.footerFacebookUrl,
      footerYoutubeUrl: raw['FOOTER_YOUTUBE_URL'] || DEFAULT_PUBLIC_SETTINGS.footerYoutubeUrl,
      footerTiktokUrl: raw['FOOTER_TIKTOK_URL'] || DEFAULT_PUBLIC_SETTINGS.footerTiktokUrl,
      freeShippingThreshold: parseNumber(raw['FREE_SHIPPING_THRESHOLD'], DEFAULT_PUBLIC_SETTINGS.freeShippingThreshold),
      defaultShippingFee: parseNumber(raw['DEFAULT_SHIPPING_FEE'], DEFAULT_PUBLIC_SETTINGS.defaultShippingFee),
      orderAutoCancelHours: parseNumber(raw['ORDER_AUTO_CANCEL_HOURS'], DEFAULT_PUBLIC_SETTINGS.orderAutoCancelHours),
      returnWindowDays: parseNumber(raw['RETURN_WINDOW_DAYS'], DEFAULT_PUBLIC_SETTINGS.returnWindowDays),
      enableCod: parseBoolean(raw['ENABLE_COD'], DEFAULT_PUBLIC_SETTINGS.enableCod),
      enableBankTransfer: parseBoolean(raw['ENABLE_BANK_TRANSFER'], DEFAULT_PUBLIC_SETTINGS.enableBankTransfer),
      metaTitle: raw['META_TITLE'] || DEFAULT_PUBLIC_SETTINGS.metaTitle,
      metaDescription: raw['META_DESCRIPTION'] || DEFAULT_PUBLIC_SETTINGS.metaDescription,
      maintenanceMode: parseBoolean(raw['MAINTENANCE_MODE'], DEFAULT_PUBLIC_SETTINGS.maintenanceMode),
      policyShoppingGuide: raw['POLICY_SHOPPING_GUIDE'] || DEFAULT_PUBLIC_SETTINGS.policyShoppingGuide,
      policyShippingDelivery: raw['POLICY_SHIPPING_DELIVERY'] || DEFAULT_PUBLIC_SETTINGS.policyShippingDelivery,
      policyWarrantyReturn: raw['POLICY_WARRANTY_RETURN'] || DEFAULT_PUBLIC_SETTINGS.policyWarrantyReturn,
      policyFaqJson: raw['POLICY_FAQ_JSON'] || DEFAULT_PUBLIC_SETTINGS.policyFaqJson
    };
  }
}
