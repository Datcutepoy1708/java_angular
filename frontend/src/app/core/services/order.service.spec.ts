import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { OrderService } from './order.service';
import { environment } from '../../../environments/environment';
import { CreateOrderRequest, Order } from '../models/order.model';

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;
  const baseUrl = `${environment.apiUrl}/api/v1/orders`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        OrderService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call createOrder POST endpoint', () => {
    const request: CreateOrderRequest = {
      receiverName: 'Nguyen Van A',
      receiverPhone: '0901234567',
      shippingAddress: '123 Cầu Giấy, Hà Nội',
      paymentMethod: 'cod'
    };

    const mockOrder: Order = {
      orderId: 1,
      orderCode: 'ORD-20260825-100000-TEST',
      userId: 1,
      receiverName: 'Nguyen Van A',
      receiverPhone: '0901234567',
      shippingAddress: '123 Cầu Giấy, Hà Nội',
      subtotal: 25000000,
      discountAmount: 0,
      shippingFee: 0,
      totalAmount: 25000000,
      paymentMethod: 'cod',
      paymentStatus: 'unpaid',
      orderStatus: 'pending',
      createdAt: '2026-08-25T10:00:00',
      updatedAt: '2026-08-25T10:00:00',
      items: [],
      statusHistory: []
    };

    service.createOrder(request).subscribe(res => {
      expect(res.success).toBe(true);
      expect(res.data.orderCode).toBe('ORD-20260825-100000-TEST');
    });

    const req = httpMock.expectOne(baseUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush({ success: true, message: 'Đặt hàng thành công', data: mockOrder });
  });

  it('should call getOrderByCode GET endpoint', () => {
    service.getOrderByCode('ORD-TEST').subscribe(res => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/ORD-TEST`);
    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: { orderCode: 'ORD-TEST' } });
  });

  it('should call cancelMyOrder POST endpoint', () => {
    service.cancelMyOrder('ORD-TEST', 'Không muốn mua nữa').subscribe(res => {
      expect(res.success).toBe(true);
    });

    const req = httpMock.expectOne(`${baseUrl}/ORD-TEST/cancel`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ reason: 'Không muốn mua nữa' });
    req.flush({ success: true, message: 'OK', data: {} });
  });
});
