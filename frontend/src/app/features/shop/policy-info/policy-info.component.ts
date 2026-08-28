import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { SettingService } from '../../../core/services/setting.service';

export type PolicyTopic =
  | 'shopping-guide'
  | 'shipping-delivery'
  | 'warranty-return'
  | 'faq';

export interface FaqItem {
  id: number;
  question: string;
  answer: string;
  category: 'order' | 'shipping' | 'warranty' | 'general';
  isOpen?: boolean;
}

@Component({
  selector: 'app-policy-info',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './policy-info.component.html',
  styleUrl: './policy-info.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PolicyInfoComponent implements OnInit {
  private readonly settingService = inject(SettingService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly publicSettings = this.settingService.publicSettings;
  readonly activeTopic = signal<PolicyTopic>('shopping-guide');
  readonly returnWindowDays = computed(
    () => this.publicSettings().returnWindowDays || 14
  );

  readonly expandedFaqId = signal<number | null>(1);

  readonly navItems: { id: PolicyTopic; title: string; subtitle: string; icon: string }[] = [
    {
      id: 'shopping-guide',
      title: 'Hướng dẫn mua hàng Online',
      subtitle: 'Quy trình đặt hàng, thanh toán & tra cứu',
      icon: 'cart',
    },
    {
      id: 'shipping-delivery',
      title: 'Chính sách giao hàng & Lắp đặt',
      subtitle: 'Phí ship, hỏa tốc & lắp ráp tận nơi',
      icon: 'truck',
    },
    {
      id: 'warranty-return',
      title: 'Chính sách bảo hành & Đổi trả',
      subtitle: 'Cam kết 1 đổi 1 & thời hạn bảo hành',
      icon: 'shield',
    },
    {
      id: 'faq',
      title: 'Câu hỏi thường gặp (FAQ)',
      subtitle: 'Giải đáp thắc mắc dịch vụ & hỗ trợ',
      icon: 'help',
    },
  ];

  readonly faqs = computed<FaqItem[]>(() => {
    const rawJson = this.publicSettings().policyFaqJson;
    if (rawJson && rawJson.trim().length > 0) {
      try {
        const parsed = JSON.parse(rawJson);
        if (Array.isArray(parsed) && parsed.length > 0) {
          return parsed;
        }
      } catch {
        // Fallback to default
      }
    }
    return this.defaultFaqList;
  });

  readonly defaultFaqList: FaqItem[] = [
    {
      id: 1,
      category: 'order',
      question: 'Tôi có bắt buộc phải tạo tài khoản để mua hàng không?',
      answer:
        'Không bắt buộc. Quý khách hoàn toàn có thể mua hàng với tư cách Khách vãng lai (Guest Checkout) chỉ bằng cách nhập thông tin họ tên, số điện thoại và địa chỉ nhận hàng. Tuy nhiên, việc đăng ký tài khoản thành viên sẽ giúp quý khách lưu lại lịch sử đơn hàng, tích lũy điểm thưởng và nhận thông báo khuyến mãi sớm nhất.',
    },
    {
      id: 2,
      category: 'order',
      question: 'Đơn hàng chưa thanh toán chuyển khoản sẽ được giữ trong bao lâu?',
      answer:
        'Đơn hàng chọn phương thức thanh toán chuyển khoản ngân hàng sẽ được giữ chỗ linh kiện trong vòng thời gian quy định (thông thường 24 giờ). Nếu sau khoảng thời gian này hệ thống chưa ghi nhận thanh toán hoặc không liên hệ được với quý khách, đơn hàng sẽ tự động hủy để hoàn kho linh kiện phục vụ khách hàng khác.',
    },
    {
      id: 3,
      category: 'shipping',
      question: 'Làm thế nào để tôi kiểm tra tình trạng vận chuyển đơn hàng của mình?',
      answer:
        'Quý khách có thể truy cập vào mục "Tra cứu đơn hàng" trên menu hoặc thanh điều hướng, nhập Mã đơn hàng (ví dụ: ORD-2026...) cùng Số điện thoại đặt hàng để xem ngay lộ trình xử lý, đóng gói và đơn vị vận chuyển theo thời gian thực.',
    },
    {
      id: 4,
      category: 'shipping',
      question: 'Tôi có được quyền kiểm tra hàng trước khi thanh toán (đồng kiểm) không?',
      answer:
        'Có. Complexus luôn khuyến khích quý khách mở kiện hàng kiểm tra ngoại quan sản phẩm (đúng mã hàng, nguyên tem niêm phong, không bị cấn móp hay vỡ hỏng do vận chuyển) trước khi thanh toán tiền cho nhân viên giao hàng.',
    },
    {
      id: 5,
      category: 'warranty',
      question: 'Sản phẩm của Complexus có phải là hàng chính hãng 100% không?',
      answer:
        'Tất cả sản phẩm và linh kiện PC tại Complexus đều là hàng chính hãng 100% được phân phối chính thức từ các thương hiệu hàng đầu thế giới (Intel, AMD, ASUS, MSI, Gigabyte, Corsair, Samsung...). Sản phẩm có đầy đủ hóa đơn VAT, tem bảo hành chính hãng từ nhà phân phối ủy quyền tại Việt Nam.',
    },
    {
      id: 6,
      category: 'warranty',
      question: 'Nếu linh kiện bị lỗi trong quá trình sử dụng, tôi cần liên hệ ai để được hỗ trợ?',
      answer:
        'Quý khách có thể liên hệ trực tiếp hotline Chăm sóc khách hàng hoặc mang sản phẩm đến các trung tâm bảo hành của Complexus cùng số điện thoại mua hàng để được kiểm tra kỹ thuật và xử lý bảo hành nhanh nhất.',
    },
    {
      id: 7,
      category: 'general',
      question: 'Complexus có hỗ trợ xuất hóa đơn điện tử GTGT (VAT) cho doanh nghiệp không?',
      answer:
        'Có. Giá niêm yết trên website đã bao gồm thuế GTGT (VAT). Khi thanh toán đơn hàng, quý khách chỉ cần điền đầy đủ thông tin Tên công ty, Mã số thuế và Địa chỉ công ty tại mục Ghi chú / Xuất hóa đơn. Hóa đơn điện tử hợp lệ sẽ được gửi về email của quý khách trong vòng 24 - 48 giờ làm việc sau khi giao hàng thành công.',
    },
    {
      id: 8,
      category: 'general',
      question: 'Complexus có hỗ trợ kỹ thuật và tư vấn nâng cấp PC miễn phí không?',
      answer:
        'Đội ngũ chuyên gia kỹ thuật và tư vấn viên của Complexus luôn sẵn sàng hỗ trợ kiểm tra độ tương thích linh kiện, tối ưu tản nhiệt, tư vấn cấu hình PC theo đúng nhu cầu công việc/gaming của quý khách hoàn toàn miễn phí qua Hotline, Fanpage hoặc trực tiếp tại Showroom.',
    },
  ];

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const topic = params.get('topic') as PolicyTopic | null;
      if (
        topic &&
        ['shopping-guide', 'shipping-delivery', 'warranty-return', 'faq'].includes(topic)
      ) {
        this.activeTopic.set(topic);
      } else {
        this.activeTopic.set('shopping-guide');
      }
      // Scroll to top on topic change
      if (typeof window !== 'undefined') {
        window.scrollTo({ top: 0, behavior: 'smooth' });
      }
    });
  }

  setTopic(topic: PolicyTopic): void {
    this.router.navigate(['/policy', topic]);
  }

  toggleFaq(faqId: number): void {
    this.expandedFaqId.update((current) => (current === faqId ? null : faqId));
  }

  formatCurrency(val: number | string | null | undefined): string {
    const num = Number(val) || 0;
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(num);
  }
}
