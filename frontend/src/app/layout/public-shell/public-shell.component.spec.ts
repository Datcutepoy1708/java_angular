import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { PublicShellComponent } from './public-shell.component';
import { CategoryService } from '../../core/services/category.service';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';

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

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PublicShellComponent],
      providers: [
        provideRouter([]),
        { provide: CategoryService, useValue: mockCategoryService },
        { provide: AuthService, useValue: mockAuthService },
        { provide: CartService, useValue: mockCartService },
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
});
