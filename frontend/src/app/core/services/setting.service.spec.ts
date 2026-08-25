import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { SettingService } from './setting.service';
import { environment } from '../../../environments/environment';
import { SettingItem } from '../models/setting.model';

describe('SettingService', () => {
  let service: SettingService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SettingService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(SettingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created with default settings in signal', () => {
    expect(service).toBeDefined();
    expect(service.publicSettings().storeName).toContain('Complexus');
    expect(service.publicSettings().freeShippingThreshold).toBe(5000000);
    expect(service.publicSettings().enableCod).toBe(true);
    expect(service.publicSettings().maintenanceMode).toBe(false);
  });

  it('should load public settings and parse numbers and booleans strongly', () => {
    const mockRaw: Record<string, string> = {
      STORE_NAME: 'Complexus Flagship Store',
      FOOTER_BRAND_TITLE: 'COMPLEXUS VIETNAM',
      FREE_SHIPPING_THRESHOLD: '10000000',
      DEFAULT_SHIPPING_FEE: '45000',
      ENABLE_COD: 'false',
      ENABLE_BANK_TRANSFER: 'true',
      MAINTENANCE_MODE: 'true',
    };

    service.loadPublicSettings().subscribe(settings => {
      expect(settings.storeName).toBe('Complexus Flagship Store');
      expect(settings.footerBrandTitle).toBe('COMPLEXUS VIETNAM');
      expect(settings.freeShippingThreshold).toBe(10000000);
      expect(settings.defaultShippingFee).toBe(45000);
      expect(settings.enableCod).toBe(false);
      expect(settings.enableBankTransfer).toBe(true);
      expect(settings.maintenanceMode).toBe(true);

      // Signal state verification
      expect(service.publicSettings().storeName).toBe('Complexus Flagship Store');
      expect(service.publicSettings().maintenanceMode).toBe(true);
      expect(service.isLoaded()).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/settings/public`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockRaw });
  });

  it('should fetch all settings for admin', () => {
    const mockItems: SettingItem[] = [
      {
        settingId: 1,
        settingKey: 'STORE_NAME',
        settingValue: 'Complexus',
        settingGroup: 'GENERAL',
        isPublic: true,
      },
    ];

    service.getAllSettings().subscribe(res => {
      expect(res.data.length).toBe(1);
      expect(res.data[0].settingKey).toBe('STORE_NAME');
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/settings`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockItems });
  });

  it('should update settings and reload public settings', () => {
    const payload = { settings: { STORE_NAME: 'New Name' } };

    service.updateSettings(payload).subscribe(res => {
      expect(res.success).toBe(true);
    });

    const putReq = httpMock.expectOne(`${baseUrl}/api/v1/settings`);
    expect(putReq.request.method).toBe('PUT');
    putReq.flush({ success: true, message: 'Updated', data: null });

    const getReq = httpMock.expectOne(`${baseUrl}/api/v1/settings/public`);
    expect(getReq.request.method).toBe('GET');
    getReq.flush({ success: true, message: 'OK', data: { STORE_NAME: 'New Name' } });
  });
});
