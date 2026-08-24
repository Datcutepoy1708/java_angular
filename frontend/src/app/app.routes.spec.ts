import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Location } from '@angular/common';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, it, expect, beforeEach } from 'vitest';
import { routes } from './app.routes';
import { AuthService } from './core/services/auth.service';

describe('App Routes', () => {
  let router: Router;
  let location: Location;

  beforeEach(async () => {
    localStorage.clear();

    await TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter(routes)
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    location = TestBed.inject(Location);
  });

  it('should navigate directly to /admin/login without guard redirection', async () => {
    await router.navigateByUrl('/admin/login');
    expect(location.path()).toBe('/admin/login');
  });

  it('should redirect unauthenticated access on /admin to /admin/login', async () => {
    await router.navigateByUrl('/admin');
    expect(location.path()).toContain('/admin/login');
  });

  it('should navigate to /auth/login', async () => {
    await router.navigateByUrl('/auth/login');
    expect(location.path()).toBe('/auth/login');
  });

  it('should navigate to /auth/register', async () => {
    await router.navigateByUrl('/auth/register');
    expect(location.path()).toBe('/auth/register');
  });

  it('should navigate to Storefront home route /', async () => {
    await router.navigateByUrl('/');
    expect(location.path()).toBe('');
  });

  it('should navigate to Storefront products listing /products', async () => {
    await router.navigateByUrl('/products');
    expect(location.path()).toBe('/products');
  });
});
