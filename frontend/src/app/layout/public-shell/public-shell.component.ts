import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CategoryService } from '../../core/services/category.service';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';
import { SettingService } from '../../core/services/setting.service';
import { CategoryResponse } from '../../core/models/category.model';

@Component({
  selector: 'app-public-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, FormsModule],
  templateUrl: './public-shell.component.html',
  styleUrl: './public-shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicShellComponent implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly settingService = inject(SettingService);
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);
  private readonly router = inject(Router);

  // State
  readonly categoriesTree = signal<CategoryResponse[]>([]);
  readonly searchQuery = signal('');
  readonly isMegaMenuOpen = signal(false);
  readonly cartCount = this.cartService.totalQuantity;
  readonly toastMessage = this.cartService.toastMessage;
  readonly publicSettings = this.settingService.publicSettings;

  ngOnInit(): void {
    // 1. Fetch public system settings (Footer, Branding, Maintenance Mode)
    this.settingService.loadPublicSettings().subscribe({
      error: err => console.error('Error fetching public settings:', err),
    });

    // 2. Fetch category tree exactly ONCE when shell initializes
    this.categoryService.getTree().subscribe({
      next: (res: { data: CategoryResponse[] }) => {
        this.categoriesTree.set(res.data);
      },
      error: (err: unknown) => {
        console.error('Error fetching category tree in PublicShell:', err);
      },
    });
  }

  toggleMegaMenu(): void {
    this.isMegaMenuOpen.update((v) => !v);
  }

  closeMegaMenu(): void {
    this.isMegaMenuOpen.set(false);
  }

  onSearchSubmit(): void {
    const q = this.searchQuery().trim();
    if (q) {
      this.router.navigate(['/products'], { queryParams: { keyword: q } });
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/']);
  }

  formatCategoryIcon(url: string | null | undefined, slug?: string): string {
    if (url && url.trim()) return url;
    if (slug) return `/assets/categories/${slug}.png`;
    return '/assets/categories/laptop.png';
  }

  handleImageError(event: Event, fallbackUrl: string): void {
    const target = event.target as HTMLImageElement;
    if (target && target.src !== fallbackUrl) {
      target.src = fallbackUrl;
    }
  }
}
