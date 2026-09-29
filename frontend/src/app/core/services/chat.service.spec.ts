import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Client } from '@stomp/stompjs';
import { ChatService } from './chat.service';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

describe('ChatService socket lifecycle', () => {
  let service: ChatService;
  let client: Client;
  const originalApiUrl = environment.apiUrl;

  beforeEach(() => {
    vi.spyOn(Client.prototype, 'activate').mockImplementation(function (this: Client) {
      client = this;
    });
    vi.spyOn(Client.prototype, 'deactivate').mockResolvedValue();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), {
        provide: AuthService,
        useValue: { getAccessToken: () => 'test-token', currentUser: () => null },
      }],
    });
    service = TestBed.inject(ChatService);
  });

  afterEach(() => {
    service.disconnect();
    environment.apiUrl = originalApiUrl;
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it.each(['customer', 'staff'])('clears connected state on an unexpected %s socket close', role => {
    if (role === 'customer') service.connectAsCustomer(6);
    else service.connectAsStaff(6, () => undefined);
    service.isConnected.set(true);
    client.onWebSocketClose({} as CloseEvent);
    expect(service.isConnected()).toBe(false);
  });

  it('uses the configured backend origin and native WebSocket endpoint', () => {
    environment.apiUrl = 'https://api.example.test';
    // Recreate after changing configuration: service fields capture apiUrl on creation.
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), {
      provide: AuthService, useValue: { currentUser: () => null },
    }] });
    service = TestBed.inject(ChatService);
    const socket = vi.fn(function () { return {}; });
    vi.stubGlobal('WebSocket', socket);
    service.connectAsCustomer(6);
    client.webSocketFactory!();
    expect(socket).toHaveBeenCalledWith('wss://api.example.test/ws-chat');
    expect(client.connectionTimeout).toBe(10000);
  });
});
