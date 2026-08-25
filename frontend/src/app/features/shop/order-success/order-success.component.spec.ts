import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { OrderSuccessComponent } from './order-success.component';
import { OrderService } from '../../../core/services/order.service';

describe('OrderSuccessComponent', () => {
  let component: OrderSuccessComponent;
  let fixture: ComponentFixture<OrderSuccessComponent>;

  beforeEach(async () => {
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
        OrderService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrderSuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and extract orderCode from route', () => {
    expect(component).toBeTruthy();
    expect(component.orderCode()).toBe('ORD-TEST-123');
  });
});
