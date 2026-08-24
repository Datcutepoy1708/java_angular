import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-rating-stars',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './rating-stars.component.html',
  styleUrl: './rating-stars.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RatingStarsComponent {
  readonly rating = input<number>(5);
  readonly maxStars = input<number>(5);
  readonly showScore = input<boolean>(false);
  readonly size = input<'sm' | 'md' | 'lg'>('md');

  get stars(): boolean[] {
    const r = Math.round(this.rating());
    const m = this.maxStars();
    return Array.from({ length: m }, (_, i) => i < r);
  }
}
