import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { BannerService } from './banner.service';
import { environment } from '../../../environments/environment';
import { Banner, BannerRequest } from '../models/banner.model';

describe('BannerService', () => {
  let service: BannerService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        BannerService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(BannerService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch public banners by position', () => {
    const mockBanners: Banner[] = [{
      bannerId: 1,
      title: 'Hero Banner',
      imageUrl: '/uploads/hero.jpg',
      linkUrl: '/products',
      position: 'homepage_slider',
      sortOrder: 1,
      status: 'active',
      createdAt: '2026-08-25'
    }];

    service.getPublicBanners('homepage_slider').subscribe(res => {
      expect(res.data.length).toBe(1);
      expect(res.data[0].title).toBe('Hero Banner');
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/banners/public?position=homepage_slider`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: mockBanners });
  });

  it('should create banner for admin', () => {
    const request: BannerRequest = {
      title: 'New Banner',
      imageUrl: '/uploads/new.jpg',
      position: 'homepage_slider',
      status: 'active'
    };

    service.createBanner(request).subscribe(res => {
      expect(res.data.bannerId).toBe(2);
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/admin/banners`);
    expect(req.request.method).toBe('POST');
    req.flush({ success: true, message: 'OK', data: { bannerId: 2, ...request, sortOrder: 0, createdAt: '2026-08-25' } });
  });

  it('should delete banner for admin', () => {
    service.deleteBanner(2).subscribe(res => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/api/v1/admin/banners/2`);
    expect(req.request.method).toBe('DELETE');
    req.flush({ success: true, message: 'OK', data: null });
  });
});
