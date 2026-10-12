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

  it('uses the configured backend origin and SockJS endpoint', () => {
    environment.apiUrl = 'https://api.example.test';
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), {
      provide: AuthService, useValue: { currentUser: () => null },
    }] });
    service = TestBed.inject(ChatService);
    service.connectAsCustomer(6);
    expect(client.webSocketFactory).toBeDefined();
    expect(client.connectionTimeout).toBe(10000);
  });

  describe('appendOrReconcile', () => {
    it('appends a new message when no optimistic message exists', () => {
      const existing: any[] = [];
      const incoming: any = {
        messageId: 1,
        conversationId: 10,
        senderType: 'STAFF',
        senderId: 2,
        senderName: 'Admin',
        content: 'Xin chào',
        attachmentUrl: null,
        quickReplies: [],
        read: false,
        createdAt: '2026-10-12T10:00:00Z',
      };
      const result = service.appendOrReconcile(existing, incoming);
      expect(result).toHaveLength(1);
      expect(result[0].messageId).toBe(1);
    });

    it('ignores duplicate message with same real messageId', () => {
      const existing: any[] = [{
        messageId: 1,
        conversationId: 10,
        senderType: 'CUSTOMER',
        senderId: null,
        senderName: 'Bạn',
        content: 'Alo',
        attachmentUrl: null,
        quickReplies: [],
        read: false,
        createdAt: '2026-10-12T10:00:00Z',
      }];
      const duplicate: any = { ...existing[0] };
      const result = service.appendOrReconcile(existing, duplicate);
      expect(result).toHaveLength(1);
      expect(result).toBe(existing);
    });

    it('reconciles optimistic message with incoming real message to prevent duplicates', () => {
      const tempId = Date.now();
      const existing: any[] = [{
        messageId: tempId,
        conversationId: 10,
        senderType: 'CUSTOMER',
        senderId: null,
        senderName: 'Bạn',
        content: 'tôi cần xem chi tiết',
        attachmentUrl: null,
        quickReplies: [],
        read: false,
        createdAt: new Date().toISOString(),
        isOptimistic: true,
      }];
      const confirmed: any = {
        messageId: 42,
        conversationId: 10,
        senderType: 'CUSTOMER',
        senderId: null,
        senderName: 'Bạn',
        content: 'tôi cần xem chi tiết',
        attachmentUrl: null,
        quickReplies: [],
        read: true,
        createdAt: new Date().toISOString(),
      };
      const result = service.appendOrReconcile(existing, confirmed);
      expect(result).toHaveLength(1);
      expect(result[0].messageId).toBe(42);
      expect(result[0].isOptimistic).toBeUndefined();
    });
  });
});
