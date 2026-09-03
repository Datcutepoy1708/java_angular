import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { SocialAuthService } from './social-auth.service';

describe('SocialAuthService', () => {
  let service: SocialAuthService;
  let httpMock: HttpTestingController;

  const waitAsync = (ms: number = 25) => new Promise(resolve => setTimeout(resolve, ms));

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
    sessionStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
    vi.restoreAllMocks();
    vi.useRealTimers();
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
        facebookAppId: 'test-fb-id',
        zaloAppId: 'test-zalo-id'
      }
    });

    await loadPromise;
    expect(service).toBeTruthy();
  });

  it('should throw error when signInWithZalo called without zaloAppId configured', async () => {
    const signInPromise = service.signInWithZalo();

    const req = httpMock.expectOne('http://localhost:8080/api/v1/auth/oauth2/config');
    req.flush({
      success: true,
      data: {
        googleClientId: 'test-google-id',
        facebookAppId: 'test-fb-id',
        zaloAppId: ''
      }
    });

    await expect(signInPromise).rejects.toThrow('Zalo App ID chưa được cấu hình');
  });

  it('should lock concurrent logins so clicking twice opens only one popup', async () => {
    (service as any).zaloAppId = 'test-zalo-id';
    (service as any).configLoaded = true;

    const mockPopup = { closed: false, close: vi.fn() } as any;
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => mockPopup);

    const firstPromise = service.signInWithZalo();
    firstPromise.catch(() => {}); // Catch expected rejection when mockPopup closes

    // Call again concurrently while first login is in progress
    await expect(service.signInWithZalo()).rejects.toThrow(
      'Đang trong quá trình đăng nhập Zalo. Vui lòng chờ hoàn tất.'
    );

    // Wait for async crypto.subtle.digest to complete and window.open to be called
    await waitAsync(30);
    expect(openSpy).toHaveBeenCalledTimes(1);

    // User closes popup to complete first flow
    mockPopup.closed = true;
    await waitAsync(550);
  });

  it('should cleanup listener, interval, and sessionStorage state when popup is closed by user (using fake timers)', async () => {
    (service as any).zaloAppId = 'test-zalo-id';
    (service as any).configLoaded = true;

    const mockPopup = { closed: false, close: vi.fn() } as any;
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => mockPopup);
    const removeEventListenerSpy = vi.spyOn(window, 'removeEventListener');

    const signInPromise = service.signInWithZalo();

    // Wait for crypto.subtle.digest so popup opens
    await waitAsync(30);

    expect(openSpy).toHaveBeenCalledTimes(1);
    expect(sessionStorage.getItem('zalo_oauth_state')).toBeTruthy();

    // Now enable fake timers to control checkClosedInterval
    vi.useFakeTimers();

    // User closes the popup
    mockPopup.closed = true;

    // Advance fake timer by 500ms
    vi.advanceTimersByTime(500);

    // Promise should reject because popup closed
    await expect(signInPromise).rejects.toThrow('Cửa sổ đăng nhập Zalo đã bị đóng.');

    // Cleanup verified
    expect(sessionStorage.getItem('zalo_oauth_state')).toBeNull();
    expect(removeEventListenerSpy).toHaveBeenCalledWith('message', expect.any(Function));

    // Progress flag reset in finally: switch back to real timers to start second flow
    vi.useRealTimers();

    const secondMockPopup = { closed: false, close: vi.fn() } as any;
    openSpy.mockImplementation(() => secondMockPopup);

    const secondPromise = service.signInWithZalo();
    await waitAsync(30);
    expect(sessionStorage.getItem('zalo_oauth_state')).toBeTruthy();

    vi.useFakeTimers();
    secondMockPopup.closed = true;
    vi.advanceTimersByTime(500);
    await expect(secondPromise).rejects.toThrow('Cửa sổ đăng nhập Zalo đã bị đóng.');
    vi.useRealTimers();
  });

  it('should resolve authorization code when valid message received with matching origin and popup source', async () => {
    (service as any).zaloAppId = 'test-zalo-id';
    (service as any).configLoaded = true;

    const mockPopup = { closed: false, close: vi.fn() } as any;
    vi.spyOn(window, 'open').mockImplementation(() => mockPopup);

    const signInPromise = service.signInWithZalo();

    // Wait for async crypto tasks
    await waitAsync(30);

    const savedState = sessionStorage.getItem('zalo_oauth_state');
    expect(savedState).toBeTruthy();

    // Dispatch message from popup
    const event = new MessageEvent('message', {
      origin: window.location.origin,
      source: mockPopup,
      data: {
        type: 'ZALO_AUTH_CALLBACK',
        code: 'valid-zalo-code-123',
        state: savedState
      }
    });
    window.dispatchEvent(event);

    const result = await signInPromise;
    expect(result.code).toBe('valid-zalo-code-123');
    expect(result.codeVerifier).toBeTruthy();
    expect(sessionStorage.getItem('zalo_oauth_state')).toBeNull();
  });
});
