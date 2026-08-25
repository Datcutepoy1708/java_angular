import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { Order, OrderStatus } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-tracking',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './order-tracking.component.html',
  styleUrls: ['./order-tracking.component.scss']
})
export class OrderTrackingComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orderService = inject(OrderService);

  readonly order = signal<Order | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);
  readonly cancelReason = signal<string>('');
  readonly showCancelModal = signal<boolean>(false);
  readonly isCancelling = signal<boolean>(false);
  readonly cancelSuccessMessage = signal<string | null>(null);

  readonly statusSteps: { key: OrderStatus; label: string; stepNumber: number }[] = [
    { key: 'pending', label: 'Chờ xác nhận', stepNumber: 1 },
    { key: 'confirmed', label: 'Đã xác nhận', stepNumber: 2 },
    { key: 'processing', label: 'Đang chuẩn bị', stepNumber: 3 },
    { key: 'shipping', label: 'Đang giao hàng', stepNumber: 4 },
    { key: 'completed', label: 'Giao thành công', stepNumber: 5 }
  ];

  ngOnInit(): void {
    const code = this.route.snapshot.paramMap.get('orderCode');
    if (code) {
      this.loadOrder(code);
    } else {
      this.isLoading.set(false);
      this.errorMessage.set('Không tìm thấy mã đơn hàng.');
    }
  }

  loadOrder(code: string): void {
    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.orderService.getOrderByCode(code).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.order.set(res.data);
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể tải thông tin đơn hàng.');
      }
    });
  }

  getStepIndex(status?: OrderStatus): number {
    if (!status || status === 'cancelled') return -1;
    return this.statusSteps.findIndex(s => s.key === status);
  }

  canCustomerCancel(): boolean {
    return this.order()?.orderStatus === 'pending';
  }

  openCancelModal(): void {
    this.cancelReason.set('');
    this.showCancelModal.set(true);
  }

  closeCancelModal(): void {
    this.showCancelModal.set(false);
  }

  confirmCancelOrder(): void {
    const currentOrder = this.order();
    if (!currentOrder) return;

    this.isCancelling.set(true);
    this.orderService.cancelMyOrder(currentOrder.orderCode, this.cancelReason()).subscribe({
      next: (res) => {
        this.isCancelling.set(false);
        this.showCancelModal.set(false);
        if (res.success && res.data) {
          this.order.set(res.data);
          this.cancelSuccessMessage.set('Đã hủy đơn hàng thành công và hoàn trả tồn kho.');
          setTimeout(() => this.cancelSuccessMessage.set(null), 5000);
        }
      },
      error: (err) => {
        this.isCancelling.set(false);
        this.errorMessage.set(err.error?.message || 'Không thể hủy đơn hàng.');
      }
    });
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
