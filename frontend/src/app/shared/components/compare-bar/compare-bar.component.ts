import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { CompareService } from '../../../core/services/compare.service';

@Component({
  selector: 'app-compare-bar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './compare-bar.component.html',
  styleUrl: './compare-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CompareBarComponent {
  readonly compareService = inject(CompareService);
  private readonly router = inject(Router);

  readonly items = this.compareService.items;
  readonly count = this.compareService.count;
  readonly notification = this.compareService.notification;

  // Max 4 slots
  readonly maxSlots = [0, 1, 2, 3];

  formatCurrency(val: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
    }).format(val);
  }

  onRemove(productId: number, event: Event): void {
    event.stopPropagation();
    this.compareService.removeFromCompare(productId);
  }

  onClearAll(event: Event): void {
    event.stopPropagation();
    this.compareService.clear();
  }

  onNavigateToCompare(): void {
    if (this.count() < 2) {
      alert('Vui lòng chọn ít nhất 2 sản phẩm để so sánh!');
      return;
    }
    const ids = this.items().map((i) => i.productId).join(',');
    this.router.navigate(['/compare'], { queryParams: { ids } });
  }
}
