import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { OrderService } from '../../../core/services/order.service';
import { Order, OrderStatus } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './order-history.component.html',
  styleUrls: ['./order-history.component.scss']
})
export class OrderHistoryComponent implements OnInit {
  private readonly orderService = inject(OrderService);

  readonly orders = signal<Order[]>([]);
  readonly totalOrders = signal<number>(0);
  readonly currentPage = signal<number>(0);
  readonly totalPages = signal<number>(0);
  readonly isLoading = signal<boolean>(true);
  readonly selectedTab = signal<string>('all');

  readonly tabs: { key: string; label: string }[] = [
    { key: 'all', label: 'Tất cả' },
    { key: 'pending', label: 'Chờ xác nhận' },
    { key: 'shipping', label: 'Đang giao' },
    { key: 'completed', label: 'Hoàn tất' },
    { key: 'cancelled', label: 'Đã hủy' }
  ];

  ngOnInit(): void {
    this.loadOrders(0);
  }

  loadOrders(page = 0): void {
    this.isLoading.set(true);
    this.currentPage.set(page);

    this.orderService.getMyOrders(page, 10).subscribe({
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
      }
    });
  }

  filterByTab(tabKey: string): void {
    this.selectedTab.set(tabKey);
  }

  getFilteredOrders(): Order[] {
    const tab = this.selectedTab();
    if (tab === 'all') return this.orders();
    if (tab === 'pending') return this.orders().filter(o => o.orderStatus === 'pending' || o.orderStatus === 'confirmed' || o.orderStatus === 'processing');
    if (tab === 'shipping') return this.orders().filter(o => o.orderStatus === 'shipping');
    if (tab === 'completed') return this.orders().filter(o => o.orderStatus === 'completed');
    if (tab === 'cancelled') return this.orders().filter(o => o.orderStatus === 'cancelled');
    return this.orders();
  }

  formatPrice(price: number): string {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price || 0);
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    });
  }
}
