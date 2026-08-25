import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminShellComponent } from './admin-shell.component';
import { AuthService } from '../../core/services/auth.service';
import { ThemeService } from '../../core/services/theme.service';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { UserSummary } from '../../core/models/auth.model';

describe('AdminShellComponent', () => {
  let component: AdminShellComponent;
  let fixture: ComponentFixture<AdminShellComponent>;
  let authServiceMock: any;
  let themeServiceMock: any;

  const mockUser: UserSummary = {
    userId: 1,
    fullName: 'Admin Tester',
    email: 'admin@store.com',
    status: 'active',
    roles: ['ROLE_ADMIN'],
    permissions: []
  };

  beforeEach(async () => {
    authServiceMock = {
      currentUser: signal<UserSummary | null>(mockUser),
      logout: vi.fn()
    };

    const isDarkSignal = signal(false);
    themeServiceMock = {
      currentTheme: signal<'light' | 'dark'>('light'),
      isDark: isDarkSignal,
      toggleTheme: vi.fn(() => {
        isDarkSignal.update(v => !v);
      })
    };

    await TestBed.configureTestingModule({
      imports: [AdminShellComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
        { provide: ThemeService, useValue: themeServiceMock }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AdminShellComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and render navigation items and user initials', () => {
    expect(component).toBeTruthy();
    expect(component.navItems.length).toBeGreaterThan(5);
    expect(component.getUserInitials()).toBe('AT');
  });

  it('should toggle theme when toggleTheme is called', () => {
    expect(component.isDark()).toBe(false);
    component.toggleTheme();
    expect(themeServiceMock.toggleTheme).toHaveBeenCalled();
    expect(component.isDark()).toBe(true);
  });

  it('should call authService.logout on logout', () => {
    component.logout();
    expect(authServiceMock.logout).toHaveBeenCalled();
  });
});
