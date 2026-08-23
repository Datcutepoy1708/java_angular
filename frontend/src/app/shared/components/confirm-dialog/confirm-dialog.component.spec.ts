import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ConfirmDialogComponent } from './confirm-dialog.component';

describe('ConfirmDialogComponent', () => {
  let component: ConfirmDialogComponent;
  let fixture: ComponentFixture<ConfirmDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not render dialog content when isOpen is false', () => {
    const dialog = fixture.nativeElement.querySelector('.confirm-dialog');
    expect(dialog).toBeNull();
  });

  it('should render dialog content when isOpen is true', () => {
    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();
    const dialog = fixture.nativeElement.querySelector('.confirm-dialog');
    expect(dialog).toBeTruthy();
  });

  it('should emit confirmed event when confirm button clicked', () => {
    let emitted = false;
    component.confirmed.subscribe(() => (emitted = true));

    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();

    const confirmBtn = fixture.nativeElement.querySelector('#btn-confirm-dialog-action');
    confirmBtn?.click();

    expect(emitted).toBe(true);
  });

  it('should emit cancelled event when cancel button clicked', () => {
    let emitted = false;
    component.cancelled.subscribe(() => (emitted = true));

    fixture.componentRef.setInput('isOpen', true);
    fixture.detectChanges();

    const cancelBtn = fixture.nativeElement.querySelector('.btn-ghost');
    cancelBtn?.click();

    expect(emitted).toBe(true);
  });
});
