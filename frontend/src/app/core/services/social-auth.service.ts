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

  /**
   * Tự động tải Client ID trực tiếp từ Backend (nguồn file .env)
   */
  public async loadConfig(): Promise<void> {
    if (this.configLoaded && (this.googleClientId || this.facebookAppId)) {
      return;
    }
    try {
      const res = await firstValueFrom(
        this.http.get<ApiResponse<{ googleClientId: string; facebookAppId: string }>>(
          `${environment.apiUrl}/api/v1/auth/oauth2/config`
        )
      );
      if (res && res.data) {
        this.googleClientId = res.data.googleClientId || '';
        this.facebookAppId = res.data.facebookAppId || '';
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
}
