import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/auth.model';

declare const google: any;
declare const FB: any;

@Injectable({
  providedIn: 'root'
})
export class SocialAuthService {
  private readonly http = inject(HttpClient);

  private googleLoaded = false;
  private facebookLoaded = false;
  private configLoaded = false;

  private googleClientId = '';
  private facebookAppId = '';
  private zaloAppId = '';
  private zaloLoginInProgress = false;

  /**
   * Tự động tải Client ID trực tiếp từ Backend (nguồn file .env)
   */
  public async loadConfig(): Promise<void> {
    if (this.configLoaded && (this.googleClientId || this.facebookAppId || this.zaloAppId)) {
      return;
    }
    try {
      const res = await firstValueFrom(
        this.http.get<ApiResponse<{ googleClientId: string; facebookAppId: string; zaloAppId: string }>>(
          `${environment.apiUrl}/api/v1/auth/oauth2/config`
        )
      );
      if (res && res.data) {
        this.googleClientId = res.data.googleClientId || '';
        this.facebookAppId = res.data.facebookAppId || '';
        this.zaloAppId = res.data.zaloAppId || '';
        this.configLoaded = true;
      }
    } catch (err) {
      console.warn('Không thể tải cấu hình OAuth2 động từ backend (.env):', err);
    }
  }

  /**
   * Tải Google Identity Services SDK (GIS)
   */
  public async loadGoogleScript(): Promise<void> {
    await this.loadConfig();

    return new Promise((resolve, reject) => {
      if (this.googleLoaded || (typeof google !== 'undefined' && google?.accounts?.id)) {
        this.googleLoaded = true;
        resolve();
        return;
      }

      const existingScript = document.getElementById('google-jssdk');
      if (existingScript) {
        this.googleLoaded = true;
        resolve();
        return;
      }

      const script = document.createElement('script');
      script.id = 'google-jssdk';
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => {
        this.googleLoaded = true;
        resolve();
      };
      script.onerror = (err) => reject(new Error('Không thể tải Google SDK: ' + err));
      document.head.appendChild(script);
    });
  }

  /**
   * Tải Facebook Javascript SDK v21.0
   */
  public async loadFacebookScript(): Promise<void> {
    await this.loadConfig();

    return new Promise((resolve, reject) => {
      if (typeof FB !== 'undefined' && FB?.init) {
        try {
          FB.init({
            appId: this.facebookAppId,
            cookie: true,
            xfbml: true,
            version: 'v21.0'
          });
        } catch (e) {
          console.warn('FB.init error:', e);
        }
        this.facebookLoaded = true;
        resolve();
        return;
      }

      (window as any).fbAsyncInit = () => {
        try {
          FB.init({
            appId: this.facebookAppId,
            cookie: true,
            xfbml: true,
            version: 'v21.0'
          });
          this.facebookLoaded = true;
          resolve();
        } catch (err: any) {
          reject(new Error('Lỗi khởi tạo Facebook SDK: ' + (err.message || err)));
        }
      };

      const existingScript = document.getElementById('facebook-jssdk');
      if (existingScript) {
        const timer = setInterval(() => {
          if (typeof FB !== 'undefined' && FB?.init) {
            clearInterval(timer);
            FB.init({
              appId: this.facebookAppId,
              cookie: true,
              xfbml: true,
              version: 'v21.0'
            });
            this.facebookLoaded = true;
            resolve();
          }
        }, 100);
        setTimeout(() => {
          clearInterval(timer);
          resolve();
        }, 2000);
        return;
      }

      const script = document.createElement('script');
      script.id = 'facebook-jssdk';
      script.src = 'https://connect.facebook.net/vi_VN/sdk.js';
      script.async = true;
      script.defer = true;
      script.onerror = (err) => reject(new Error('Không thể tải Facebook SDK: ' + err));
      document.head.appendChild(script);
    });
  }

  /**
   * Mở popup đăng nhập Google và nhận về ID Token (credential)
   */
  public async signInWithGoogle(): Promise<string> {
    await this.loadConfig();

    if (!this.googleClientId) {
      throw new Error('Google Client ID chưa được cấu hình trong file .env');
    }

    await this.loadGoogleScript();

    return new Promise((resolve, reject) => {
      try {
        let isResolved = false;

        google.accounts.id.initialize({
          client_id: this.googleClientId,
          callback: (response: any) => {
            if (response && response.credential) {
              isResolved = true;
              resolve(response.credential);
            } else {
              reject(new Error('Không nhận được Google ID Token từ phản hồi'));
            }
          },
          auto_select: false,
          cancel_on_tap_outside: true
        });

        // Tạo container ẩn để render button chuẩn của Google và tự động kích hoạt click
        const buttonDiv = document.createElement('div');
        buttonDiv.style.display = 'none';
        document.body.appendChild(buttonDiv);

        google.accounts.id.renderButton(buttonDiv, {
          theme: 'outline',
          size: 'large'
        });

        // Kích hoạt click vào button ẩn của Google để mở popup chính chủ
        const innerBtn = buttonDiv.querySelector('div[role=button]') as HTMLElement;
        if (innerBtn) {
          innerBtn.click();
        } else {
          // Fallback dùng prompt() nếu không tìm thấy DOM button
          google.accounts.id.prompt((notification: any) => {
            if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
              if (!isResolved) {
                reject(new Error('Không thể hiển thị cửa sổ đăng nhập Google.'));
              }
            }
          });
        }

        // Tự động dọn dẹp buttonDiv sau khi hoàn tất
        setTimeout(() => {
          if (document.body.contains(buttonDiv)) {
            document.body.removeChild(buttonDiv);
          }
        }, 3000);

      } catch (err: any) {
        reject(new Error('Lỗi khởi tạo Google Login: ' + (err.message || err)));
      }
    });
  }

  /**
   * Mở popup đăng nhập Facebook qua OAuth Dialog (Hỗ trợ cả HTTP localhost và HTTPS production)
   */
  public async signInWithFacebook(): Promise<string> {
    await this.loadConfig();

    if (!this.facebookAppId) {
      throw new Error('Facebook App ID chưa được cấu hình trong file .env');
    }

    return new Promise((resolve, reject) => {
      // Redirect URI về chính trang login hiện tại
      const redirectUri = `${window.location.origin}/auth/login`;
      const oauthUrl = `https://www.facebook.com/v21.0/dialog/oauth?client_id=${this.facebookAppId}&redirect_uri=${encodeURIComponent(redirectUri)}&scope=public_profile,email&response_type=token`;

      const width = 600;
      const height = 700;
      const left = window.screenX + Math.max(0, (window.outerWidth - width) / 2);
      const top = window.screenY + Math.max(0, (window.outerHeight - height) / 2);

      const popup = window.open(
        oauthUrl,
        'FacebookLoginPopup',
        `width=${width},height=${height},left=${left},top=${top},scrollbars=yes,status=no,toolbar=no`
      );

      if (!popup || popup.closed || typeof popup.closed === 'undefined') {
        reject(new Error('Trình duyệt đã chặn cửa sổ bật lên (popup). Vui lòng cho phép popup để tiếp tục.'));
        return;
      }

      let isCompleted = false;

      // Lắng nghe URL của popup khi Facebook redirect về
      const interval = setInterval(() => {
        try {
          if (!popup || popup.closed) {
            clearInterval(interval);
            if (!isCompleted) {
              reject(new Error('Cửa sổ đăng nhập Facebook đã bị đóng.'));
            }
            return;
          }

          // Kiểm tra xem popup đã chuyển hướng về cùng origin chưa (same-origin)
          const currentUrl = popup.location.href;
          if (currentUrl && currentUrl.includes(window.location.origin)) {
            const hash = popup.location.hash;
            const search = popup.location.search;

            isCompleted = true;
            clearInterval(interval);
            popup.close();

            // Trích xuất access_token từ hash: #access_token=...&expires_in=...
            if (hash && hash.includes('access_token=')) {
              const hashParams = new URLSearchParams(hash.replace(/^#/, ''));
              const token = hashParams.get('access_token');
              if (token) {
                resolve(token);
                return;
              }
            }

            // Kiểm tra lỗi nếu người dùng bấm Hủy trên Facebook: ?error=...
            if (search && search.includes('error=')) {
              const searchParams = new URLSearchParams(search);
              const errorDesc = searchParams.get('error_description') || searchParams.get('error_message') || 'Đăng nhập Facebook bị từ chối.';
              reject(new Error(errorDesc));
              return;
            }

            reject(new Error('Không nhận được Access Token từ phản hồi của Facebook.'));
          }
        } catch (e) {
          // Khi popup còn ở domain https://www.facebook.com, việc đọc popup.location.href
          // sẽ ném SecurityError do khác origin (Cross-Origin Frame). Bỏ qua và chờ tick kế tiếp.
        }
      }, 250);
    });
  }

  /**
   * Tạo chuỗi ngẫu nhiên bảo mật dùng cho PKCE code_verifier
   */
  private generateRandomString(length: number = 64): string {
    const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
    const randomValues = new Uint8Array(length);
    crypto.getRandomValues(randomValues);
    let result = '';
    for (let i = 0; i < length; i++) {
      result += charset[randomValues[i] % charset.length];
    }
    return result;
  }

  /**
   * Băm chuỗi SHA-256 và mã hóa Base64URL không đệm padding (PKCE code_challenge)
   */
  private async generateCodeChallenge(codeVerifier: string): Promise<string> {
    const encoder = new TextEncoder();
    const data = encoder.encode(codeVerifier);
    const digest = await crypto.subtle.digest('SHA-256', data);
    const base64 = btoa(String.fromCharCode(...new Uint8Array(digest)));
    return base64
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
  }

  /**
   * Mở popup đăng nhập Zalo qua OAuth v4 PKCE:
   * - Ngăn chặn mở đồng thời nhiều popup (single-popup lock)
   * - Sinh state ngẫu nhiên lưu vào sessionStorage để chống tấn công CSRF
   * - Sinh cặp mã PKCE (code_verifier & code_challenge)
   * - Mở popup tới Zalo OAuth và lắng nghe postMessage từ route /auth/zalo/callback
   */
  public async signInWithZalo(): Promise<{ code: string; codeVerifier: string }> {
    if (this.zaloLoginInProgress) {
      throw new Error('Đang trong quá trình đăng nhập Zalo. Vui lòng chờ hoàn tất.');
    }

    this.zaloLoginInProgress = true;
    try {
      return await this.executeZaloLogin();
    } finally {
      this.zaloLoginInProgress = false;
    }
  }

  private async executeZaloLogin(): Promise<{ code: string; codeVerifier: string }> {
    await this.loadConfig();

    if (!this.zaloAppId) {
      throw new Error('Zalo App ID chưa được cấu hình trong file .env');
    }

    const codeVerifier = this.generateRandomString(64);
    const codeChallenge = await this.generateCodeChallenge(codeVerifier);

    // Tạo state ngẫu nhiên chống tấn công CSRF
    const stateArray = new Uint8Array(16);
    crypto.getRandomValues(stateArray);
    const state = Array.from(stateArray, b => b.toString(16).padStart(2, '0')).join('');
    sessionStorage.setItem('zalo_oauth_state', state);

    return new Promise((resolve, reject) => {
      const redirectUri = `${window.location.origin}/auth/zalo/callback`;
      const oauthUrl = `https://oauth.zaloapp.com/v4/permission?app_id=${this.zaloAppId}&redirect_uri=${encodeURIComponent(redirectUri)}&code_challenge=${codeChallenge}&state=${state}`;

      const width = 550;
      const height = 650;
      const left = window.screenX + Math.max(0, (window.outerWidth - width) / 2);
      const top = window.screenY + Math.max(0, (window.outerHeight - height) / 2);

      const popup = window.open(
        oauthUrl,
        'ZaloLoginPopup',
        `width=${width},height=${height},left=${left},top=${top},scrollbars=yes,status=no,toolbar=no`
      );

      if (!popup || popup.closed || typeof popup.closed === 'undefined') {
        sessionStorage.removeItem('zalo_oauth_state');
        reject(new Error('Trình duyệt đã chặn cửa sổ bật lên (popup). Vui lòng cho phép popup để tiếp tục.'));
        return;
      }

      let isCompleted = false;

      const cleanup = () => {
        window.removeEventListener('message', messageHandler);
        if (checkClosedInterval) {
          clearInterval(checkClosedInterval);
        }
        sessionStorage.removeItem('zalo_oauth_state');
        if (popup && !popup.closed) {
          try {
            popup.close();
          } catch (e) {
            // Ignore cross-origin close errors
          }
        }
      };

      // Lắng nghe postMessage từ ZaloCallbackComponent (kiểm tra cả origin và window source)
      const messageHandler = (event: MessageEvent) => {
        if (event.origin !== window.location.origin || event.source !== popup) {
          return;
        }

        if (event.data && event.data.type === 'ZALO_AUTH_CALLBACK') {
          isCompleted = true;
          const savedState = sessionStorage.getItem('zalo_oauth_state');
          cleanup();

          // Xác thực CSRF State nghiêm ngặt
          if (!event.data.state || event.data.state !== savedState) {
            reject(new Error('Lỗi bảo mật CSRF: State không khớp hoặc phiên đăng nhập không hợp lệ!'));
            return;
          }

          if (event.data.error) {
            const desc = event.data.errorDescription || event.data.error || 'Đăng nhập Zalo bị từ chối.';
            reject(new Error(`Lỗi Zalo: ${desc}`));
            return;
          }

          if (!event.data.code) {
            reject(new Error('Không nhận được Authorization Code từ Zalo.'));
            return;
          }

          resolve({
            code: event.data.code,
            codeVerifier
          });
        }
      };

      window.addEventListener('message', messageHandler);

      // Giám sát trường hợp người dùng chủ động tắt popup
      const checkClosedInterval = setInterval(() => {
        if (!isCompleted && (!popup || popup.closed)) {
          isCompleted = true;
          cleanup();
          reject(new Error('Cửa sổ đăng nhập Zalo đã bị đóng.'));
        }
      }, 500);
    });
  }
}
