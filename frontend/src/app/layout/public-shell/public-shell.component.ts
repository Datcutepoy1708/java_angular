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
  readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // State
  readonly categoriesTree = signal<CategoryResponse[]>([]);
  readonly searchQuery = signal('');
  readonly isMegaMenuOpen = signal(false);
  // TODO (Phase 5 - Cart): Connect cartCount to CartService.cartTotalItems$
  readonly cartCount = signal(0);

  ngOnInit(): void {
    // Fetch category tree exactly ONCE when shell initializes
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
}
