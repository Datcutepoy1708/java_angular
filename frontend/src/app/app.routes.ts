import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  // 1. Auth routes (Split-screen for customers: /auth/login, /auth/register)
  {
    path: 'auth',
    loadComponent: () =>
      import('./features/auth/auth-layout/auth-layout.component').then((m) => m.AuthLayoutComponent),
    children: [
      { path: '', redirectTo: 'login', pathMatch: 'full' },
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/login/login.component').then((m) => m.LoginComponent)
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./features/auth/register/register.component').then((m) => m.RegisterComponent)
      }
    ]
  },

  // Shorthand aliases
  { path: 'login', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: 'register', redirectTo: 'auth/register', pathMatch: 'full' },

  // 2. Admin area — parent route handles both login AND protected dashboard
  {
    path: 'admin',
    children: [
      // 2a. Admin Login: /admin/login — NO guard, public
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/admin-login/admin-login.component').then((m) => m.AdminLoginComponent)
      },

      // 2b. Admin protected area: /admin, /admin/dashboard etc — guarded
      {
        path: '',
        loadComponent: () =>
          import('./layout/admin-shell/admin-shell.component').then((m) => m.AdminShellComponent),
        canActivate: [authGuard, roleGuard],
        data: { roles: ['ROLE_ADMIN', 'ROLE_STAFF'] },
        children: [
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
        ]
      }
    ]
  },

  // 3. Storefront public routes (PublicShell)
  {
    path: '',
    loadComponent: () =>
      import('./layout/public-shell/public-shell.component').then((m) => m.PublicShellComponent),
    children: [
      { path: '', redirectTo: 'auth/login', pathMatch: 'full' }
    ]
  },

  // 4. Wildcard fallback
  { path: '**', redirectTo: 'auth/login' }
];
