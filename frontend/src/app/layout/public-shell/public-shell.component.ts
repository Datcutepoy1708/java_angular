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
import { ThemeService } from '../../core/services/theme.service';
import { BannerService } from '../../core/services/banner.service';
import { Banner } from '../../core/models/banner.model';
import { ChatWidgetComponent } from '../../shared/chat-widget/chat-widget.component';

@Component({
  selector: 'app-public-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, FormsModule, ChatWidgetComponent],
  templateUrl: './public-shell.component.html',
  styleUrl: './public-shell.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicShellComponent implements OnInit {
  private readonly categoryService = inject(CategoryService);
  private readonly settingService = inject(SettingService);
  private readonly bannerService = inject(BannerService);
  readonly authService = inject(AuthService);
  readonly cartService = inject(CartService);
  readonly themeService = inject(ThemeService);
  private readonly router = inject(Router);

  // State
  readonly categoriesTree = signal<CategoryResponse[]>([]);
  readonly searchQuery = signal('');
  readonly isMegaMenuOpen = signal(false);
  readonly cartCount = this.cartService.totalQuantity;
  readonly toastMessage = this.cartService.toastMessage;
  readonly publicSettings = this.settingService.publicSettings;
  readonly flankBannersLeft = signal<Banner[]>([]);
  readonly flankBannersRight = signal<Banner[]>([]);

  ngOnInit(): void {
    // 0. Ensure public storefront always defaults to clean light theme
    this.themeService.setTheme('light');

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

    // 3. Fetch floating side flank banners (Left and Right)
    this.loadFlankBanners();
  }

  loadFlankBanners(): void {
    this.bannerService.getPublicBanners('flank_left').subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.flankBannersLeft.set(res.data);
        }
      },
      error: () => {}
    });

    this.bannerService.getPublicBanners('flank_right').subscribe({
      next: (res) => {
        if (res.success && res.data) {
          this.flankBannersRight.set(res.data);
        }
      },
      error: () => {}
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
