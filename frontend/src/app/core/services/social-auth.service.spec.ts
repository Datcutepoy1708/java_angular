import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { SocialAuthService } from './social-auth.service';

describe('SocialAuthService', () => {
  let service: SocialAuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SocialAuthService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(SocialAuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should load config dynamically from backend', async () => {
    const loadPromise = service.loadConfig();

    const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/oauth2/config');
    expect(req.request.method).toBe('GET');
    req.flush({
      success: true,
      data: {
        googleClientId: 'test-google-id',
        facebookAppId: 'test-fb-id'
      }
    });

    await loadPromise;
    expect(service).toBeTruthy();
  });
});
