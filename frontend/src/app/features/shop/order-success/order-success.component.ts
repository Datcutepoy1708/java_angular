import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { Order } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-success',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './order-success.component.html',
  styleUrls: ['./order-success.component.scss']
})
export class OrderSuccessComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);

  readonly orderCode = signal<string>('');
  readonly order = signal<Order | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);
  readonly copied = signal<boolean>(false);

  ngOnInit(): void {
    const code = this.route.snapshot.paramMap.get('orderCode');
    if (code) {
      this.orderCode.set(code);
      this.loadOrderDetails(code);
    } else {
      this.isLoading.set(false);
      this.errorMessage.set('Không tìm thấy mã đơn hàng.');
    }
  }

  loadOrderDetails(code: string): void {
    this.isLoading.set(true);
    this.orderService.getOrderByCode(code).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.order.set(res.data);
        }
      },
      error: () => {
        this.isLoading.set(false);
        // If unauthenticated or direct landing, keep orderCode visible
      }
    });
  }

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.copied.set(true);
      setTimeout(() => this.copied.set(false), 2000);
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0);
  }
}
