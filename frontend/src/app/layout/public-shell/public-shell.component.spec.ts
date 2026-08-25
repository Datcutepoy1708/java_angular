import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PublicShellComponent } from './public-shell.component';
import { CategoryService } from '../../core/services/category.service';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';

import { SettingService } from '../../core/services/setting.service';
import { ThemeService } from '../../core/services/theme.service';
import { signal } from '@angular/core';

describe('PublicShellComponent', () => {
  let component: PublicShellComponent;
  let fixture: ComponentFixture<PublicShellComponent>;

  const mockCategoryService = {
    getTree: () => of({ success: true, message: 'OK', data: [] }),
  };

  const mockAuthService = {
    currentUser: () => null,
    logout: () => {},
  };

  const mockCartService = {
    totalQuantity: () => 0,
    toastMessage: () => null,
    clearToast: () => {},
  };

  const isDarkSignal = signal(false);
  const mockThemeService = {
    currentTheme: signal<'light' | 'dark'>('light'),
    isDark: isDarkSignal,
    toggleTheme: () => isDarkSignal.update(v => !v)
  };

  const mockSettingService = {
    publicSettings: () => ({
      storeName: 'Complexus',
      footerBrandTitle: 'COMPLEXUS',
      footerDescription: 'Computer Store',
      contactPhone: '1800 6868',
      contactEmail: 'support@complexus.com',
      storeAddress: 'Hà Nội',
      freeShippingThreshold: 5000000,
      defaultShippingFee: 30000,
      enableCod: true,
      enableBankTransfer: true,
      maintenanceMode: false
    }),
    loadPublicSettings: () => of({
      storeName: 'Complexus',
      footerBrandTitle: 'COMPLEXUS',
      footerDescription: 'Computer Store',
      contactPhone: '1800 6868',
      contactEmail: 'support@complexus.com',
      storeAddress: 'Hà Nội',
      freeShippingThreshold: 5000000,
      defaultShippingFee: 30000,
      enableCod: true,
      enableBankTransfer: true,
      maintenanceMode: false
    })
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PublicShellComponent],
      providers: [
        provideRouter([]),
        { provide: CategoryService, useValue: mockCategoryService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: CartService, useValue: mockCartService },
        { provide: SettingService, useValue: mockSettingService },
        { provide: ThemeService, useValue: mockThemeService }
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PublicShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should toggle mega menu state', () => {
    expect(component.isMegaMenuOpen()).toBe(false);
    component.toggleMegaMenu();
    expect(component.isMegaMenuOpen()).toBe(true);
    component.closeMegaMenu();
    expect(component.isMegaMenuOpen()).toBe(false);
  });

  it('should toggle theme when toggleTheme is called', () => {
    expect(component.isDark()).toBe(false);
    component.toggleTheme();
    expect(component.isDark()).toBe(true);
  });
});
