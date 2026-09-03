import { Component, OnInit, inject, signal, DestroyRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { timer, of } from 'rxjs';
import { exhaustMap, map, catchError, takeWhile, takeUntil } from 'rxjs/operators';
import { OrderService } from '../../../core/services/order.service';
import { PaymentService } from '../../../core/services/payment.service';
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
  private readonly paymentService = inject(PaymentService);
  private readonly destroyRef = inject(DestroyRef);

  readonly orderCode = signal<string>('');
  readonly order = signal<Order | null>(null);
  readonly isLoading = signal<boolean>(true);
  readonly errorMessage = signal<string | null>(null);
  readonly copied = signal<boolean>(false);
  readonly isPaid = signal<boolean>(false);
  readonly isChecking = signal<boolean>(false);

  private pollingToken: string | null = null;

  ngOnInit(): void {
    const state = history.state;
    if (state?.order) {
      this.order.set(state.order);
      if (state.order.paymentStatus === 'paid') {
        this.isPaid.set(true);
      }
    }
    if (state?.pollingToken) {
      this.pollingToken = state.pollingToken;
    }

    const code = this.route.snapshot.paramMap.get('orderCode');
    if (code) {
      this.orderCode.set(code);
      if (!this.pollingToken) {
        try {
          this.pollingToken = sessionStorage.getItem(`payment_polling_${code}`);
        } catch {
          // Ignore session storage error
        }
      }
      if (!this.order()) {
        this.loadOrderDetails(code);
      } else {
        this.isLoading.set(false);
        this.initPollingIfEligible();
      }
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
          if (res.data.paymentStatus === 'paid') {
            this.isPaid.set(true);
            this.clearSessionToken(code);
          }
          if (res.data.paymentInstruction?.paymentPollingToken) {
            this.pollingToken = res.data.paymentInstruction.paymentPollingToken;
          }
          this.initPollingIfEligible();
        }
      },
      error: () => {
        this.isLoading.set(false);
      }
    });
  }

  private initPollingIfEligible(): void {
    const currentOrder = this.order();
    if (!currentOrder || currentOrder.paymentMethod !== 'bank_transfer' || this.isPaid()) {
      return;
    }

    if (!this.pollingToken) {
      return;
    }

    const token = this.pollingToken;
    const code = this.orderCode();

    timer(0, 5000).pipe(
      exhaustMap(() =>
        this.paymentService.getPaymentStatus(token).pipe(
          map(res => res.data),
          catchError(() => of({ status: 'PENDING' as const, paidAt: null }))
        )
      ),
      takeWhile(res => res.status !== 'PAID', true),
      takeUntil(timer(15 * 60 * 1000)),
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (res) => {
        if (res.status === 'PAID') {
          this.isPaid.set(true);
          this.clearSessionToken(code);
          const o = this.order();
          if (o) {
            this.order.set({ ...o, paymentStatus: 'paid' });
          }
        }
      }
    });
  }

  private clearSessionToken(code: string): void {
    if (!code) return;
    try {
      sessionStorage.removeItem(`payment_polling_${code}`);
    } catch {
      // Ignore
    }
  }

  checkPaymentNow(): void {
    if (!this.pollingToken || this.isChecking()) return;
    this.isChecking.set(true);
    this.paymentService.getPaymentStatus(this.pollingToken).pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (res) => {
        this.isChecking.set(false);
        if (res.data?.status === 'PAID') {
          this.isPaid.set(true);
          const o = this.order();
          if (o) {
            this.order.set({ ...o, paymentStatus: 'paid' });
          }
        }
      },
      error: () => {
        this.isChecking.set(false);
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
