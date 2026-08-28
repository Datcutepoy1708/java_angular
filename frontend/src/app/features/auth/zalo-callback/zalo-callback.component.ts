import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-zalo-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="callback-container">
      <div class="spinner"></div>
      <p class="status-text">Đang hoàn tất đăng nhập Zalo...</p>
    </div>
  `,
  styles: [`
    .callback-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      min-height: 100vh;
      background: #0f172a;
      color: #f8fafc;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }
    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid rgba(0, 104, 255, 0.2);
      border-top-color: #0068ff;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin-bottom: 16px;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    .status-text {
      font-size: 15px;
      color: #94a3b8;
    }
  `]
})
export class ZaloCallbackComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);

  ngOnInit(): void {
    const queryParams = this.route.snapshot.queryParams;
    const code = queryParams['code'] || null;
    const state = queryParams['state'] || null;
    const error = queryParams['error'] || null;
    const errorDescription = queryParams['error_description'] || null;

    if (window.opener && !window.opener.closed) {
      window.opener.postMessage({
        type: 'ZALO_AUTH_CALLBACK',
        code,
        state,
        error,
        errorDescription
      }, window.location.origin);

      setTimeout(() => {
        window.close();
      }, 100);
    } else {
      // Nếu mở trực tiếp trong tab chính mà không có popup opener
      window.location.href = '/auth/login';
    }
  }
}
