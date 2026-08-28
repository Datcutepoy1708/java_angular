import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PolicyInfoComponent } from './policy-info.component';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { SettingService } from '../../../core/services/setting.service';
import { signal } from '@angular/core';
import { convertToParamMap } from '@angular/router';
import { of } from 'rxjs';

describe('PolicyInfoComponent', () => {
  let component: PolicyInfoComponent;
  let fixture: ComponentFixture<PolicyInfoComponent>;

  const mockPublicSettings = signal({
    storeName: 'Complexus Tech',
    storeSlogan: 'PC Gaming',
    storeHotline: '1800 6868',
    storeEmail: 'support@complexus.vn',
    storeAddress: '123 Đường Công Nghệ, Hà Nội',
    storeWorkingHours: '08:00 - 21:30',
    freeShippingThreshold: 5000000,
    defaultShippingFee: 35000,
    orderAutoCancelHours: 24,
    enableCod: true,
    enableBankTransfer: true,
    maintenanceMode: false,
    metaTitle: 'Complexus',
    metaDescription: 'Tech store',
    returnWindowDays: 14,
    footerBrandTitle: 'COMPLEXUS',
    footerDescription: 'Tech',
    footerHotline: '1800 6868',
    footerEmail: 'support@complexus.vn',
    footerAddress: '123 Đường Công Nghệ, Hà Nội',
    footerCopyright: '© 2026 Complexus',
    footerBusinessLicense: '',
    footerFacebookUrl: '',
    footerYoutubeUrl: '',
    footerTiktokUrl: '',
    policyShoppingGuide: 'Lưu ý từ admin',
    policyShippingDelivery: 'Giao hỏa tốc 1h',
    policyWarrantyReturn: 'Bảo hành 1 đổi 1 60 ngày',
    policyFaqJson: JSON.stringify([
      { id: 99, question: 'Câu hỏi động từ Admin?', answer: 'Câu trả lời động', category: 'general' }
    ]),
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PolicyInfoComponent],
      providers: [
        provideRouter([]),
        {
          provide: SettingService,
          useValue: {
            publicSettings: mockPublicSettings,
          },
        },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ topic: 'shipping-delivery' })),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PolicyInfoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create PolicyInfoComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should read topic from ActivatedRoute and set activeTopic', () => {
    expect(component.activeTopic()).toBe('shipping-delivery');
  });

  it('should toggle FAQ accordion items', () => {
    expect(component.expandedFaqId()).toBe(1);
    component.toggleFaq(1);
    expect(component.expandedFaqId()).toBeNull();
    component.toggleFaq(2);
    expect(component.expandedFaqId()).toBe(2);
  });

  it('should format currency correctly', () => {
    const formatted = component.formatCurrency(5000000);
    expect(formatted).toContain('5.000.000');
  });

  it('should parse dynamic FAQs from publicSettings', () => {
    expect(component.faqs().length).toBe(1);
    expect(component.faqs()[0].question).toBe('Câu hỏi động từ Admin?');
  });
});
