import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { SettingService } from '../../../core/services/setting.service';
import { SettingItem } from '../../../core/models/setting.model';

export type SettingTab =
  | 'general'
  | 'footer'
  | 'orderShipping'
  | 'seo'
  | 'systemNotification'
  | 'policy';

export interface ManageFaqItem {
  id: number;
  question: string;
  answer: string;
  category: 'order' | 'shipping' | 'warranty' | 'general';
}

@Component({
  selector: 'app-setting-manage',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './setting-manage.component.html',
  styleUrl: './setting-manage.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SettingManageComponent implements OnInit {
  private readonly settingService = inject(SettingService);
  private readonly fb = inject(FormBuilder);

  readonly activeTab = signal<SettingTab>('general');
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly showResetModal = signal<boolean>(false);
  readonly toast = signal<{ message: string; type: 'success' | 'error' } | null>(null);

  readonly faqList = signal<ManageFaqItem[]>([]);

  readonly settingForm: FormGroup = this.fb.group({
    // 1. General
    STORE_NAME: ['', [Validators.required, Validators.maxLength(150)]],
    STORE_SLOGAN: ['', [Validators.maxLength(255)]],
    STORE_HOTLINE: ['', [Validators.required, Validators.maxLength(50)]],
    STORE_EMAIL: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    STORE_ADDRESS: ['', [Validators.maxLength(255)]],
    STORE_WORKING_HOURS: ['', [Validators.maxLength(100)]],

    // 2. Footer
    FOOTER_BRAND_TITLE: ['', [Validators.required, Validators.maxLength(100)]],
    FOOTER_DESCRIPTION: ['', [Validators.maxLength(500)]],
    FOOTER_HOTLINE: ['', [Validators.maxLength(100)]],
    FOOTER_EMAIL: ['', [Validators.email, Validators.maxLength(100)]],
    FOOTER_ADDRESS: ['', [Validators.maxLength(255)]],
    FOOTER_COPYRIGHT: ['', [Validators.maxLength(255)]],
    FOOTER_BUSINESS_LICENSE: ['', [Validators.maxLength(255)]],
    FOOTER_FACEBOOK_URL: ['', [Validators.maxLength(255)]],
    FOOTER_YOUTUBE_URL: ['', [Validators.maxLength(255)]],
    FOOTER_TIKTOK_URL: ['', [Validators.maxLength(255)]],

    // 3. Orders & Shipping
    FREE_SHIPPING_THRESHOLD: [
      5000000,
      [Validators.required, Validators.min(0), Validators.pattern('^[0-9]+$')],
    ],
    DEFAULT_SHIPPING_FEE: [
      35000,
      [Validators.required, Validators.min(0), Validators.pattern('^[0-9]+$')],
    ],
    ORDER_AUTO_CANCEL_HOURS: [
      24,
      [Validators.required, Validators.min(1), Validators.pattern('^[0-9]+$')],
    ],
    ENABLE_COD: [true],
    ENABLE_BANK_TRANSFER: [true],

    // 4. SEO
    META_TITLE: ['', [Validators.maxLength(255)]],
    META_DESCRIPTION: ['', [Validators.maxLength(500)]],

    // 5. System Notification & Maintenance
    LOW_STOCK_THRESHOLD: [
      5,
      [Validators.required, Validators.min(1), Validators.pattern('^[0-9]+$')],
    ],
    MAINTENANCE_MODE: [false],

    // 6. Policy & Customer Support
    RETURN_WINDOW_DAYS: [
      14,
      [Validators.required, Validators.min(1), Validators.pattern('^[0-9]+$')],
    ],
    POLICY_SHOPPING_GUIDE: [''],
    POLICY_SHIPPING_DELIVERY: [''],
    POLICY_WARRANTY_RETURN: [''],
  });

  ngOnInit(): void {
    this.loadSettings();
  }

  setTab(tab: SettingTab): void {
    this.activeTab.set(tab);
  }

  loadSettings(): void {
    this.loading.set(true);
    this.settingService.getAllSettings().subscribe({
      next: res => {
        if (res.success && res.data) {
          this.populateForm(res.data);
        }
        this.loading.set(false);
      },
      error: () => {
        this.showToast('Không thể tải cấu hình từ máy chủ. Đang dùng cấu hình bộ nhớ đệm.', 'error');
        this.loading.set(false);
      },
    });
  }

  saveSettings(): void {
    if (this.settingForm.invalid) {
      this.settingForm.markAllAsTouched();
      this.showToast('Vui lòng kiểm tra lại các trường thông tin chưa hợp lệ!', 'error');
      return;
    }

    this.saving.set(true);
    const formValue = this.settingForm.value;

    const payload: Record<string, string> = {
      STORE_NAME: String(formValue.STORE_NAME || '').trim(),
      STORE_SLOGAN: String(formValue.STORE_SLOGAN || '').trim(),
      STORE_HOTLINE: String(formValue.STORE_HOTLINE || '').trim(),
      STORE_EMAIL: String(formValue.STORE_EMAIL || '').trim(),
      STORE_ADDRESS: String(formValue.STORE_ADDRESS || '').trim(),
      STORE_WORKING_HOURS: String(formValue.STORE_WORKING_HOURS || '').trim(),
      FOOTER_BRAND_TITLE: String(formValue.FOOTER_BRAND_TITLE || '').trim(),
      FOOTER_DESCRIPTION: String(formValue.FOOTER_DESCRIPTION || '').trim(),
      FOOTER_HOTLINE: String(formValue.FOOTER_HOTLINE || '').trim(),
      FOOTER_EMAIL: String(formValue.FOOTER_EMAIL || '').trim(),
      FOOTER_ADDRESS: String(formValue.FOOTER_ADDRESS || '').trim(),
      FOOTER_COPYRIGHT: String(formValue.FOOTER_COPYRIGHT || '').trim(),
      FOOTER_BUSINESS_LICENSE: String(formValue.FOOTER_BUSINESS_LICENSE || '').trim(),
      FOOTER_FACEBOOK_URL: String(formValue.FOOTER_FACEBOOK_URL || '').trim(),
      FOOTER_YOUTUBE_URL: String(formValue.FOOTER_YOUTUBE_URL || '').trim(),
      FOOTER_TIKTOK_URL: String(formValue.FOOTER_TIKTOK_URL || '').trim(),
      FREE_SHIPPING_THRESHOLD: String(formValue.FREE_SHIPPING_THRESHOLD),
      DEFAULT_SHIPPING_FEE: String(formValue.DEFAULT_SHIPPING_FEE),
      ORDER_AUTO_CANCEL_HOURS: String(formValue.ORDER_AUTO_CANCEL_HOURS),
      ENABLE_COD: String(Boolean(formValue.ENABLE_COD)),
      ENABLE_BANK_TRANSFER: String(Boolean(formValue.ENABLE_BANK_TRANSFER)),
      META_TITLE: String(formValue.META_TITLE || '').trim(),
      META_DESCRIPTION: String(formValue.META_DESCRIPTION || '').trim(),
      LOW_STOCK_THRESHOLD: String(formValue.LOW_STOCK_THRESHOLD),
      MAINTENANCE_MODE: String(Boolean(formValue.MAINTENANCE_MODE)),
      RETURN_WINDOW_DAYS: String(formValue.RETURN_WINDOW_DAYS ?? 14),
      POLICY_SHOPPING_GUIDE: String(formValue.POLICY_SHOPPING_GUIDE || '').trim(),
      POLICY_SHIPPING_DELIVERY: String(formValue.POLICY_SHIPPING_DELIVERY || '').trim(),
      POLICY_WARRANTY_RETURN: String(formValue.POLICY_WARRANTY_RETURN || '').trim(),
      POLICY_FAQ_JSON: JSON.stringify(this.faqList()),
    };

    this.settingService.updateSettings({ settings: payload }).subscribe({
      next: res => {
        this.saving.set(false);
        if (res.success) {
          this.showToast('Lưu toàn bộ cài đặt hệ thống thành công!', 'success');
        } else {
          this.showToast(res.message || 'Lỗi khi lưu cài đặt', 'error');
        }
      },
      error: err => {
        this.saving.set(false);
        const msg = err.error?.message || 'Không thể lưu cài đặt. Vui lòng kiểm tra lại.';
        this.showToast(msg, 'error');
      },
    });
  }

  addFaq(): void {
    const nextId = this.faqList().length > 0 ? Math.max(...this.faqList().map(f => f.id)) + 1 : 1;
    this.faqList.update(list => [
      ...list,
      {
        id: nextId,
        question: '',
        answer: '',
        category: 'general'
      }
    ]);
  }

  removeFaq(index: number): void {
    this.faqList.update(list => list.filter((_, i) => i !== index));
  }

  updateFaq(index: number, field: 'question' | 'answer' | 'category', value: string): void {
    this.faqList.update(list => {
      const updated = [...list];
      if (updated[index]) {
        updated[index] = { ...updated[index], [field]: value };
      }
      return updated;
    });
  }

  confirmReset(): void {
    this.saving.set(true);
    this.settingService.resetDefaults().subscribe({
      next: res => {
        this.saving.set(false);
        this.showResetModal.set(false);
        if (res.success) {
          this.showToast('Đã khôi phục toàn bộ cài đặt về mặc định ban đầu!', 'success');
          this.loadSettings();
        }
      },
      error: err => {
        this.saving.set(false);
        this.showResetModal.set(false);
        this.showToast(err.error?.message || 'Lỗi khi khôi phục mặc định', 'error');
      },
    });
  }

  private populateForm(items: SettingItem[]): void {
    const map: Record<string, string> = {};
    items.forEach(item => {
      map[item.settingKey] = item.settingValue;
    });

    this.settingForm.patchValue({
      STORE_NAME: map['STORE_NAME'] ?? 'Complexus Computer & Technology',
      STORE_SLOGAN: map['STORE_SLOGAN'] ?? '',
      STORE_HOTLINE: map['STORE_HOTLINE'] ?? '1800 6868',
      STORE_EMAIL: map['STORE_EMAIL'] ?? 'support@complexus.vn',
      STORE_ADDRESS: map['STORE_ADDRESS'] ?? '',
      STORE_WORKING_HOURS: map['STORE_WORKING_HOURS'] ?? '',
      FOOTER_BRAND_TITLE: map['FOOTER_BRAND_TITLE'] ?? 'COMPLEXUS',
      FOOTER_DESCRIPTION: map['FOOTER_DESCRIPTION'] ?? '',
      FOOTER_HOTLINE: map['FOOTER_HOTLINE'] ?? '',
      FOOTER_EMAIL: map['FOOTER_EMAIL'] ?? '',
      FOOTER_ADDRESS: map['FOOTER_ADDRESS'] ?? '',
      FOOTER_COPYRIGHT: map['FOOTER_COPYRIGHT'] ?? '',
      FOOTER_BUSINESS_LICENSE: map['FOOTER_BUSINESS_LICENSE'] ?? '',
      FOOTER_FACEBOOK_URL: map['FOOTER_FACEBOOK_URL'] ?? '',
      FOOTER_YOUTUBE_URL: map['FOOTER_YOUTUBE_URL'] ?? '',
      FOOTER_TIKTOK_URL: map['FOOTER_TIKTOK_URL'] ?? '',
      FREE_SHIPPING_THRESHOLD: Number(map['FREE_SHIPPING_THRESHOLD'] ?? 5000000),
      DEFAULT_SHIPPING_FEE: Number(map['DEFAULT_SHIPPING_FEE'] ?? 35000),
      ORDER_AUTO_CANCEL_HOURS: Number(map['ORDER_AUTO_CANCEL_HOURS'] ?? 24),
      ENABLE_COD: map['ENABLE_COD'] !== 'false',
      ENABLE_BANK_TRANSFER: map['ENABLE_BANK_TRANSFER'] !== 'false',
      META_TITLE: map['META_TITLE'] ?? '',
      META_DESCRIPTION: map['META_DESCRIPTION'] ?? '',
      LOW_STOCK_THRESHOLD: Number(map['LOW_STOCK_THRESHOLD'] ?? 5),
      MAINTENANCE_MODE: map['MAINTENANCE_MODE'] === 'true',
      RETURN_WINDOW_DAYS: Number(map['RETURN_WINDOW_DAYS'] ?? 14),
      POLICY_SHOPPING_GUIDE: map['POLICY_SHOPPING_GUIDE'] ?? '',
      POLICY_SHIPPING_DELIVERY: map['POLICY_SHIPPING_DELIVERY'] ?? '',
      POLICY_WARRANTY_RETURN: map['POLICY_WARRANTY_RETURN'] ?? '',
    });

    if (map['POLICY_FAQ_JSON']) {
      try {
        const parsed = JSON.parse(map['POLICY_FAQ_JSON']);
        if (Array.isArray(parsed) && parsed.length > 0) {
          this.faqList.set(parsed);
        } else {
          this.faqList.set(this.getDefaultFaqs());
        }
      } catch {
        this.faqList.set(this.getDefaultFaqs());
      }
    } else {
      this.faqList.set(this.getDefaultFaqs());
    }
  }

  private getDefaultFaqs(): ManageFaqItem[] {
    return [
      {
        id: 1,
        category: 'order',
        question: 'Tôi có bắt buộc phải tạo tài khoản để mua hàng không?',
        answer:
          'Không bắt buộc. Quý khách hoàn toàn có thể mua hàng với tư cách Khách vãng lai (Guest Checkout) chỉ bằng cách nhập thông tin họ tên, số điện thoại và địa chỉ nhận hàng.',
      },
      {
        id: 2,
        category: 'order',
        question: 'Đơn hàng chưa thanh toán chuyển khoản sẽ được giữ trong bao lâu?',
        answer:
          'Đơn hàng chọn phương thức chuyển khoản sẽ được giữ linh kiện trong 24 giờ trước khi tự động hủy hoàn kho.',
      },
      {
        id: 3,
        category: 'shipping',
        question: 'Làm thế nào để tôi kiểm tra tình trạng vận chuyển đơn hàng của mình?',
        answer:
          'Quý khách truy cập vào mục "Tra cứu đơn hàng", nhập Mã đơn hàng và Số điện thoại để theo dõi lộ trình thời gian thực.',
      },
      {
        id: 4,
        category: 'shipping',
        question: 'Tôi có được quyền kiểm tra hàng trước khi thanh toán (đồng kiểm) không?',
        answer:
          'Có. Quý khách được quyền mở kiện hàng kiểm tra ngoại quan sản phẩm trước khi thanh toán cho shipper.',
      },
      {
        id: 5,
        category: 'warranty',
        question: 'Sản phẩm của Complexus có phải là hàng chính hãng 100% không?',
        answer:
          'Tất cả linh kiện đều là hàng chính hãng 100%, có tem bảo hành và hóa đơn VAT đầy đủ từ nhà phân phối.',
      },
      {
        id: 6,
        category: 'warranty',
        question: 'Nếu linh kiện bị lỗi trong quá trình sử dụng, tôi cần liên hệ ai để được hỗ trợ?',
        answer:
          'Quý khách liên hệ trực tiếp hotline hoặc mang sản phẩm đến showroom của Complexus để được kỹ thuật viên hỗ trợ nhanh nhất.',
      },
    ];
  }

  private showToast(message: string, type: 'success' | 'error'): void {
    this.toast.set({ message, type });
    setTimeout(() => {
      this.toast.set(null);
    }, 4000);
  }
}
