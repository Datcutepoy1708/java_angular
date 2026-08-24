import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RatingStarsComponent } from './rating-stars.component';

describe('RatingStarsComponent', () => {
  let component: RatingStarsComponent;
  let fixture: ComponentFixture<RatingStarsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RatingStarsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(RatingStarsComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('rating', 4);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render correct number of filled and empty stars', () => {
    expect(component.stars).toEqual([true, true, true, true, false]);
  });
});
