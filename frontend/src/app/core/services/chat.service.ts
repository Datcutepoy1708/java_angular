import {
  inject,
  Injectable,
  PLATFORM_ID,
  signal,
  computed,
  OnDestroy,
} from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { Observable, Subject } from 'rxjs';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';
import {
  ChatBotRuleRequest,
  ChatBotRuleResponse,
  ChatConversationSummaryResponse,
  ChatInitRequest,
  ChatInitResponse,
  ChatMessageDto,
  ChatTypingSignal,
  ConversationStatus,
} from '../models/chat.model';
import { ApiResponse } from '../models/auth.model';

const CHAT_SESSION_KEY = 'complexus_chat_session_id';
const CHAT_CONV_KEY = 'complexus_chat_conv_id';

@Injectable({ providedIn: 'root' })
export class ChatService implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly authService = inject(AuthService);

  private readonly baseUrl = `${environment.apiUrl}/api/v1/chat`;
  private readonly adminUrl = `${environment.apiUrl}/api/v1/admin/chat`;
  private readonly wsUrl = `${environment.apiUrl}/ws-chat`;

  // ─── STOMP Client ────────────────────────────────────────────────────────
  private stompClient: Client | null = null;
  private subscriptions: StompSubscription[] = [];

  // ─── Public Reactive State ───────────────────────────────────────────────
  readonly messages = signal<ChatMessageDto[]>([]);
  readonly conversationId = signal<number | null>(null);
  readonly status = signal<ConversationStatus | null>(null);
  readonly unreadCount = signal<number>(0);
  readonly isConnected = signal<boolean>(false);
  readonly isTyping = signal<boolean>(false); // staff is typing

  /** Message stream for components that need raw events */
  readonly newMessage$ = new Subject<ChatMessageDto>();

  // ─── Session ID ───────────────────────────────────────────────────────────
  getOrCreateSessionId(): string {
    if (!isPlatformBrowser(this.platformId)) return '';
    let sid = localStorage.getItem(CHAT_SESSION_KEY);
    if (!sid) {
      sid = crypto.randomUUID();
      localStorage.setItem(CHAT_SESSION_KEY, sid);
    }
    return sid;
  }

  getSessionId(): string | null {
    if (!isPlatformBrowser(this.platformId)) return null;
    return localStorage.getItem(CHAT_SESSION_KEY);
  }

  private sessionHeaders(): Record<string, string> {
    return { 'X-Session-Id': this.getOrCreateSessionId() };
  }

  // ─── REST: Customer APIs ─────────────────────────────────────────────────

  initConversation(): Observable<ApiResponse<ChatInitResponse>> {
    const sessionId = this.getOrCreateSessionId();
    const user = this.authService.currentUser();
    const body: ChatInitRequest = {
      sessionId,
      customerName: user?.fullName ?? undefined,
      customerEmail: user?.email ?? undefined,
    };
    return this.http.post<ApiResponse<ChatInitResponse>>(
      `${this.baseUrl}/init`,
      body
    );
  }

  loadMessages(convId: number): Observable<ApiResponse<ChatMessageDto[]>> {
    return this.http.get<ApiResponse<ChatMessageDto[]>>(
      `${this.baseUrl}/${convId}/messages`,
      { headers: this.sessionHeaders() }
    );
  }

  markReadByCustomer(convId: number): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(
      `${this.baseUrl}/${convId}/mark-read`,
      {},
      { headers: this.sessionHeaders() }
    );
  }

  uploadChatImage(file: File): Observable<ApiResponse<{ url: string }>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<{ url: string }>>(
      `${this.baseUrl}/upload-image`,
      formData,
      { headers: this.sessionHeaders() }
    );
  }

  mergeSession(): Observable<ApiResponse<void>> {
    const sessionId = this.getOrCreateSessionId();
    return this.http.post<ApiResponse<void>>(
      `${this.baseUrl}/merge-session`,
      { sessionId }
    );
  }

  // ─── REST: Admin APIs ────────────────────────────────────────────────────

  getConversationsByStatus(
    status: ConversationStatus = 'WAITING_STAFF'
  ): Observable<ApiResponse<ChatConversationSummaryResponse[]>> {
    return this.http.get<ApiResponse<ChatConversationSummaryResponse[]>>(
      `${this.adminUrl}/conversations`,
      { params: { status } }
    );
  }

  getMyConversations(): Observable<
    ApiResponse<ChatConversationSummaryResponse[]>
  > {
    return this.http.get<ApiResponse<ChatConversationSummaryResponse[]>>(
      `${this.adminUrl}/conversations/my`
    );
  }

  getAdminMessages(convId: number): Observable<ApiResponse<ChatMessageDto[]>> {
    return this.http.get<ApiResponse<ChatMessageDto[]>>(
      `${this.adminUrl}/conversations/${convId}/messages`
    );
  }

  claimConversation(
    convId: number
  ): Observable<ApiResponse<ChatConversationSummaryResponse>> {
    return this.http.post<ApiResponse<ChatConversationSummaryResponse>>(
      `${this.adminUrl}/conversations/${convId}/claim`,
      {}
    );
  }

  closeConversation(convId: number): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(
      `${this.adminUrl}/conversations/${convId}/close`,
      {}
    );
  }

  markReadByStaff(convId: number): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(
      `${this.adminUrl}/conversations/${convId}/mark-read`,
      {}
    );
  }

  // Bot rules
  getAllBotRules(): Observable<ApiResponse<ChatBotRuleResponse[]>> {
    return this.http.get<ApiResponse<ChatBotRuleResponse[]>>(
      `${this.adminUrl}/bot-rules`
    );
  }

  getBotRule(id: number): Observable<ApiResponse<ChatBotRuleResponse>> {
    return this.http.get<ApiResponse<ChatBotRuleResponse>>(
      `${this.adminUrl}/bot-rules/${id}`
    );
  }

  createBotRule(
    req: ChatBotRuleRequest
  ): Observable<ApiResponse<ChatBotRuleResponse>> {
    return this.http.post<ApiResponse<ChatBotRuleResponse>>(
      `${this.adminUrl}/bot-rules`,
      req
    );
  }

  updateBotRule(
    id: number,
    req: ChatBotRuleRequest
  ): Observable<ApiResponse<ChatBotRuleResponse>> {
    return this.http.put<ApiResponse<ChatBotRuleResponse>>(
      `${this.adminUrl}/bot-rules/${id}`,
      req
    );
  }

  deleteBotRule(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(
      `${this.adminUrl}/bot-rules/${id}`
    );
  }

  // ─── WebSocket STOMP ─────────────────────────────────────────────────────

  /**
   * Connect to STOMP WebSocket and subscribe to the given conversation's topic.
   * Customer widget: subscribe to /topic/chat/{convId}
   */
  connectAsCustomer(convId: number): void {
    if (!isPlatformBrowser(this.platformId)) return;

    this.disconnect();
    const sessionId = this.getOrCreateSessionId();

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(this.wsUrl),
      connectHeaders: {
        'X-Session-Id': sessionId,
      },
      onConnect: () => {
        this.isConnected.set(true);

        // Subscribe to incoming messages for this conversation
        const sub = this.stompClient!.subscribe(
          `/topic/chat/${convId}`,
          (frame: IMessage) => {
            const body = frame.body;
            if (body === '"CLOSED"' || body === 'CLOSED') {
              this.status.set('CLOSED');
              return;
            }
            try {
              const msg: ChatMessageDto = JSON.parse(body);
              this.messages.update((msgs) => [...msgs, msg]);
              this.newMessage$.next(msg);
            } catch (e) {
              // Ignore parse errors
            }
          }
        );
        this.subscriptions.push(sub);

        // Subscribe to typing signals
        const typingSub = this.stompClient!.subscribe(
          `/topic/chat/${convId}/typing`,
          (frame: IMessage) => {
            try {
              const signal: ChatTypingSignal = JSON.parse(frame.body);
              if (signal.senderType !== 'CUSTOMER') {
                this.isTyping.set(signal.isTyping);
                // Auto-clear typing after 3s
                if (signal.isTyping) {
                  setTimeout(() => this.isTyping.set(false), 3000);
                }
              }
            } catch (e) {}
          }
        );
        this.subscriptions.push(typingSub);
      },
      onDisconnect: () => {
        this.isConnected.set(false);
      },
      onStompError: (frame) => {
        console.error('[Chat] STOMP error:', frame);
        this.isConnected.set(false);
      },
      reconnectDelay: 5000,
    });

    this.stompClient.activate();
  }

  /**
   * Connect as authenticated staff/admin for a specific conversation.
   * Subscribes to /topic/admin/chat/{convId}
   */
  connectAsStaff(convId: number, onMessage: (msg: ChatMessageDto) => void, onTyping?: (t: ChatTypingSignal) => void): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const token = this.authService.getAccessToken();
    if (!token) return;

    this.disconnect();

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(this.wsUrl),
      connectHeaders: {
        Authorization: `Bearer ${token}`,
      },
      onConnect: () => {
        this.isConnected.set(true);

        const sub = this.stompClient!.subscribe(
          `/topic/admin/chat/${convId}`,
          (frame: IMessage) => {
            try {
              const msg: ChatMessageDto = JSON.parse(frame.body);
              onMessage(msg);
            } catch (e) {}
          }
        );
        this.subscriptions.push(sub);

        if (onTyping) {
          const typingSub = this.stompClient!.subscribe(
            `/topic/admin/chat/${convId}/typing`,
            (frame: IMessage) => {
              try {
                onTyping(JSON.parse(frame.body));
              } catch (e) {}
            }
          );
          this.subscriptions.push(typingSub);
        }
      },
      onDisconnect: () => this.isConnected.set(false),
      reconnectDelay: 5000,
    });

    this.stompClient.activate();
  }

  /**
   * Subscribe to admin queue notifications (new conversations, claims, closes).
   * Returns subscription for the caller to manage.
   */
  subscribeToAdminQueue(onUpdate: (convId: number) => void): void {
    if (!this.stompClient?.connected) return;
    const sub = this.stompClient.subscribe(
      '/topic/admin/chat-queue',
      (frame: IMessage) => {
        try {
          onUpdate(JSON.parse(frame.body));
        } catch (e) {}
      }
    );
    this.subscriptions.push(sub);
  }

  /** Send a message via STOMP (used by both customer and staff) */
  sendMessageViaWs(convId: number, content: string, attachmentUrl?: string): void {
    if (!this.stompClient?.connected) return;
    this.stompClient.publish({
      destination: '/app/chat.send',
      body: JSON.stringify({
        conversationId: convId,
        content,
        attachmentUrl: attachmentUrl ?? null,
      }),
    });
  }

  /** Send typing signal */
  sendTypingSignal(convId: number, isTyping: boolean, senderType: string, senderName: string): void {
    if (!this.stompClient?.connected) return;
    this.stompClient.publish({
      destination: '/app/chat.typing',
      body: JSON.stringify({ conversationId: convId, senderType, senderName, isTyping }),
    });
  }

  disconnect(): void {
    this.subscriptions.forEach((sub) => {
      try { sub.unsubscribe(); } catch (e) {}
    });
    this.subscriptions = [];
    if (this.stompClient) {
      try { this.stompClient.deactivate(); } catch (e) {}
      this.stompClient = null;
    }
    this.isConnected.set(false);
    this.isTyping.set(false);
  }

  /** Reset widget state (e.g. when closing widget) */
  resetState(): void {
    this.messages.set([]);
    this.conversationId.set(null);
    this.status.set(null);
    this.unreadCount.set(0);
  }

  ngOnDestroy(): void {
    this.disconnect();
    this.newMessage$.complete();
  }
}
