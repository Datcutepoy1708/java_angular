import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import {
  Order,
  OrderFilter,
  OrderMetrics,
  OrderStatus,
  PaymentStatus,
  UpdateOrderStatusRequest,
  UpdatePaymentStatusRequest
} from '../../../core/models/order.model';

@Component({
  selector: 'app-order-manage',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './order-manage.component.html',
  styleUrls: ['./order-manage.component.scss']
})
export class OrderManageComponent implements OnInit {
  private readonly orderService = inject(OrderService);
  private readonly fb = inject(FormBuilder);

  readonly orders = signal<Order[]>([]);
  readonly metrics = signal<OrderMetrics | null>(null);
  readonly totalOrders = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly isLoading = signal<boolean>(false);
  readonly toastMessage = signal<{ text: string; type: 'success' | 'error' } | null>(null);

  // Selected Order for Detail Modal
  readonly selectedOrder = signal<Order | null>(null);
  readonly showDetailModal = signal<boolean>(false);
  readonly isUpdatingStatus = signal<boolean>(false);

  // Update Status Form
  statusForm!: FormGroup;

  // Filter Form
  filterForm!: FormGroup;

  ngOnInit(): void {
    this.initForms();
    this.loadMetrics();
    this.loadOrders(0);
  }

  private initForms(): void {
    this.filterForm = this.fb.group({
      status: [''],
      paymentStatus: [''],
      keyword: [''],
      startDate: [''],
      endDate: ['']
    });

    this.statusForm = this.fb.group({
      status: ['' as OrderStatus],
      note: ['']
    });
  }

  loadMetrics(): void {
    this.orderService.getAdminMetrics().subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.metrics.set(res.data);
        }
      }
    });
  }

  loadOrders(page = 0): void {
    this.isLoading.set(true);
    this.currentPage.set(page);

    const f = this.filterForm.value;
    const filter: OrderFilter = {
      status: f.status || undefined,
      paymentStatus: f.paymentStatus || undefined,
      keyword: f.keyword ? f.keyword.trim() : undefined,
      startDate: f.startDate || undefined,
      endDate: f.endDate || undefined,
      page,
      size: this.pageSize(),
      sortBy: 'createdAt',
      sortDir: 'desc'
    };

    this.orderService.getAdminOrders(filter).subscribe({
      next: (res) => {
        this.isLoading.set(false);
        if (res.success && res.data) {
          this.orders.set(res.data.content);
          this.totalOrders.set(res.data.totalElements);
          this.totalPages.set(res.data.totalPages);
        }
      },
      error: () => {
        this.isLoading.set(false);
        this.showToast('Không thể tải danh sách đơn hàng', 'error');
      }
    });
  }

  onSearch(): void {
    this.loadOrders(0);
  }

  resetFilter(): void {
    this.filterForm.reset({
      status: '',
      paymentStatus: '',
      keyword: '',
      startDate: '',
      endDate: ''
    });
    this.loadOrders(0);
  }

  openDetailModal(order: Order): void {
    this.selectedOrder.set(order);
    this.statusForm.patchValue({
      status: order.orderStatus,
      note: ''
    });
    this.showDetailModal.set(true);
  }

  closeDetailModal(): void {
    this.showDetailModal.set(false);
    this.selectedOrder.set(null);
  }

  onUpdateStatus(): void {
    const current = this.selectedOrder();
    if (!current) return;

    const newStatus = this.statusForm.get('status')?.value as OrderStatus;
    const note = this.statusForm.get('note')?.value;

    if (newStatus === current.orderStatus) {
      this.showToast('Vui lòng chọn trạng thái mới khác trạng thái hiện tại', 'error');
      return;
    }

    this.isUpdatingStatus.set(true);
    const request: UpdateOrderStatusRequest = { status: newStatus, note };

    this.orderService.updateOrderStatus(current.orderId, request).subscribe({
      next: (res) => {
        this.isUpdatingStatus.set(false);
        if (res.success && res.data) {
          this.selectedOrder.set(res.data);
          this.showToast('Cập nhật trạng thái đơn hàng thành công', 'success');
          this.loadOrders(this.currentPage());
          this.loadMetrics();
        }
      },
      error: (err) => {
        this.isUpdatingStatus.set(false);
        this.showToast(err.error?.message || 'Không thể cập nhật trạng thái đơn hàng', 'error');
      }
    });
  }

  onConfirmPayment(): void {
    const current = this.selectedOrder();
    if (!current) return;

    this.isUpdatingStatus.set(true);
    const request: UpdatePaymentStatusRequest = {
      paymentStatus: 'paid',
      note: 'Admin xác nhận đã nhận thanh toán chuyển khoản'
    };

    this.orderService.updatePaymentStatus(current.orderId, request).subscribe({
      next: (res) => {
        this.isUpdatingStatus.set(false);
        if (res.success && res.data) {
          this.selectedOrder.set(res.data);
          this.showToast('Đã xác nhận thanh toán thành công', 'success');
          this.loadOrders(this.currentPage());
          this.loadMetrics();
        }
      },
      error: (err) => {
        this.isUpdatingStatus.set(false);
        this.showToast(err.error?.message || 'Không thể cập nhật thanh toán', 'error');
      }
    });
  }

  showToast(text: string, type: 'success' | 'error'): void {
    this.toastMessage.set({ text, type });
    setTimeout(() => this.toastMessage.set(null), 4000);
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
