import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { OrderSuccessComponent } from './order-success.component';
import { OrderService } from '../../../core/services/order.service';
import { PaymentService } from '../../../core/services/payment.service';
import { Order } from '../../../core/models/order.model';

describe('OrderSuccessComponent', () => {
  let component: OrderSuccessComponent;
  let fixture: ComponentFixture<OrderSuccessComponent>;
  let orderServiceMock: any;
  let paymentServiceMock: any;

  const mockBankOrder: Order = {
    orderId: 100,
    orderCode: 'ORD-TEST-123',
    userId: 1,
    subtotal: 500000,
    totalAmount: 500000,
    shippingFee: 0,
    discountAmount: 0,
    paymentMethod: 'bank_transfer',
    paymentStatus: 'unpaid',
    orderStatus: 'pending',
    receiverName: 'Test User',
    receiverPhone: '0901234567',
    shippingAddress: '123 Test St',
    paymentInstruction: {
      paymentReference: 'CS23456789AB',
      bankId: 'MB',
      bankAccountNo: '090123456789',
      bankAccountName: 'TEST STORE',
      totalAmount: 500000,
      qrCodeUrl: 'https://img.vietqr.io/test.png',
      paymentPollingToken: 'raw-token-123456'
    },
    items: [],
    statusHistory: [],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  beforeEach(async () => {
    vi.useFakeTimers();

    orderServiceMock = {
      getOrderByCode: vi.fn().mockReturnValue(of({ success: true, data: mockBankOrder }))
    };

    paymentServiceMock = {
      getPaymentStatus: vi.fn().mockReturnValue(of({ success: true, data: { status: 'PENDING', paidAt: null } }))
    };

    sessionStorage.clear();

    await TestBed.configureTestingModule({
      imports: [OrderSuccessComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: (key: string) => (key === 'orderCode' ? 'ORD-TEST-123' : null)
              }
            }
          }
        },
        { provide: OrderService, useValue: orderServiceMock },
        { provide: PaymentService, useValue: paymentServiceMock }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
    vi.useRealTimers();
  });

  it('should create and extract orderCode from route', () => {
    fixture = TestBed.createComponent(OrderSuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(component.orderCode()).toBe('ORD-TEST-123');
  });

  it('should start polling when order is bank transfer with polling token', () => {
    fixture = TestBed.createComponent(OrderSuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    // Initial check fired at 0ms
    vi.advanceTimersByTime(0);
    expect(paymentServiceMock.getPaymentStatus).toHaveBeenCalledWith('raw-token-123456');
    expect(paymentServiceMock.getPaymentStatus).toHaveBeenCalledTimes(1);

    // Second check fired at 5000ms
    vi.advanceTimersByTime(5000);
    expect(paymentServiceMock.getPaymentStatus).toHaveBeenCalledTimes(2);

    fixture.destroy();
  });

  it('should not poll when payment method is COD', () => {
    const codOrder: Order = { ...mockBankOrder, paymentMethod: 'cod', paymentInstruction: undefined };
    orderServiceMock.getOrderByCode.mockReturnValue(of({ success: true, data: codOrder }));

    fixture = TestBed.createComponent(OrderSuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    vi.advanceTimersByTime(10000);
    expect(paymentServiceMock.getPaymentStatus).not.toHaveBeenCalled();
    fixture.destroy();
  });

  it('should stop polling immediately when status becomes PAID', () => {
    paymentServiceMock.getPaymentStatus
      .mockReturnValueOnce(of({ success: true, data: { status: 'PENDING', paidAt: null } }))
      .mockReturnValueOnce(of({ success: true, data: { status: 'PAID', paidAt: '2026-09-04T10:00:00' } }));

    fixture = TestBed.createComponent(OrderSuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    vi.advanceTimersByTime(0);
    expect(component.isPaid()).toBe(false);

    vi.advanceTimersByTime(5000);
    expect(component.isPaid()).toBe(true);
    expect(component.order()?.paymentStatus).toBe('paid');

    // Advancing more should not poll again
    vi.advanceTimersByTime(10000);
    expect(paymentServiceMock.getPaymentStatus).toHaveBeenCalledTimes(2);

    fixture.destroy();
  });

  it('should survive temporary network errors without killing the polling stream (catchError resilience)', () => {
    paymentServiceMock.getPaymentStatus
      .mockReturnValueOnce(throwError(() => new Error('Network failure')))
      .mockReturnValueOnce(of({ success: true, data: { status: 'PENDING', paidAt: null } }))
      .mockReturnValueOnce(of({ success: true, data: { status: 'PAID', paidAt: '2026-09-04T10:00:00' } }));

    fixture = TestBed.createComponent(OrderSuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    vi.advanceTimersByTime(0); // Call 1: fails
    expect(component.isPaid()).toBe(false);

    vi.advanceTimersByTime(5000); // Call 2: pending
    expect(component.isPaid()).toBe(false);

    vi.advanceTimersByTime(5000); // Call 3: paid
    expect(component.isPaid()).toBe(true);

    fixture.destroy();
  });

  it('should restore token from sessionStorage if page is refreshed', () => {
    sessionStorage.setItem('payment_polling_ORD-TEST-123', 'stored-token-999');
    const orderWithoutInstructionToken: Order = {
      ...mockBankOrder,
      paymentInstruction: { ...mockBankOrder.paymentInstruction!, paymentPollingToken: undefined }
    };
    orderServiceMock.getOrderByCode.mockReturnValue(of({ success: true, data: orderWithoutInstructionToken }));

    fixture = TestBed.createComponent(OrderSuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    vi.advanceTimersByTime(0);
    expect(paymentServiceMock.getPaymentStatus).toHaveBeenCalledWith('stored-token-999');

    fixture.destroy();
  });

  it('should cancel polling when component is destroyed', () => {
    fixture = TestBed.createComponent(OrderSuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    vi.advanceTimersByTime(0);
    expect(paymentServiceMock.getPaymentStatus).toHaveBeenCalledTimes(1);

    fixture.destroy();

    // After destruction, timer ticks should not trigger any more calls
    vi.advanceTimersByTime(15000);
    expect(paymentServiceMock.getPaymentStatus).toHaveBeenCalledTimes(1);
  });
});
