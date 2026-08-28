import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ZaloCallbackComponent } from './zalo-callback.component';

describe('ZaloCallbackComponent', () => {
  let component: ZaloCallbackComponent;
  let fixture: ComponentFixture<ZaloCallbackComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ZaloCallbackComponent],
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams: {
                code: 'mock-zalo-code',
                state: 'mock-zalo-state'
              }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ZaloCallbackComponent);
    component = fixture.componentInstance;
  });

  it('should create ZaloCallbackComponent and postMessage to opener if present', () => {
    const postMessageSpy = vi.fn();
    const closeSpy = vi.fn();

    // Mock window.opener
    (window as any).opener = {
      closed: false,
      postMessage: postMessageSpy
    };
    window.close = closeSpy;

    fixture.detectChanges();

    expect(component).toBeTruthy();
    expect(postMessageSpy).toHaveBeenCalledWith(
      {
        type: 'ZALO_AUTH_CALLBACK',
        code: 'mock-zalo-code',
        state: 'mock-zalo-state',
        error: null,
        errorDescription: null
      },
      window.location.origin
    );
  });
});
