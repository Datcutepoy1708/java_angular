import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { OrderManageComponent } from './order-manage.component';
import { OrderService } from '../../../core/services/order.service';

describe('OrderManageComponent', () => {
  let component: OrderManageComponent;
  let fixture: ComponentFixture<OrderManageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [OrderManageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        OrderService
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(OrderManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize filter and status forms', () => {
    expect(component.filterForm).toBeDefined();
    expect(component.statusForm).toBeDefined();
  });
});
