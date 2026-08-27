import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Location } from '@angular/common';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ForbiddenComponent } from './forbidden.component';

describe('ForbiddenComponent', () => {
  let component: ForbiddenComponent;
  let fixture: ComponentFixture<ForbiddenComponent>;
  let locationSpy: { back: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    locationSpy = { back: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ForbiddenComponent],
      providers: [
        provideRouter([]),
        { provide: Location, useValue: locationSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ForbiddenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create ForbiddenComponent', () => {
    expect(component).toBeTruthy();
  });

  it('should call location.back when goBack is triggered', () => {
    component.goBack();
    expect(locationSpy.back).toHaveBeenCalled();
  });
});
