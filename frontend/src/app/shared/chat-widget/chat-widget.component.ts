import {
  Component,
  OnInit,
  OnDestroy,
  signal,
  computed,
  ViewChild,
  ElementRef,
  AfterViewChecked,
  inject,
  PLATFORM_ID,
} from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../core/services/chat.service';
import { AuthService } from '../../core/services/auth.service';
import { ChatMessageDto, ConversationStatus } from '../../core/models/chat.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-chat-widget',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-widget.component.html',
  styleUrls: ['./chat-widget.component.scss'],
})
export class ChatWidgetComponent implements OnInit, OnDestroy, AfterViewChecked {
  @ViewChild('messageListEl') messageListEl!: ElementRef<HTMLDivElement>;
  @ViewChild('inputEl') inputEl!: ElementRef<HTMLTextAreaElement>;

  private readonly chatService = inject(ChatService);
  private readonly authService = inject(AuthService);
  private readonly platformId = inject(PLATFORM_ID);

  // ─── UI State ────────────────────────────────────────────────────────────
  readonly isOpen = signal(false);
  readonly isLoading = signal(false);
  readonly isSending = signal(false);
  readonly isUploadingImage = signal(false);
  readonly inputText = signal('');
  readonly showScrollDown = signal(false);
  readonly shouldScrollBottom = signal(true);

  // ─── From Service ─────────────────────────────────────────────────────────
  readonly messages = this.chatService.messages;
  readonly convStatus = this.chatService.status;
  readonly isConnected = this.chatService.isConnected;
  readonly isTyping = this.chatService.isTyping;
  readonly unreadCount = this.chatService.unreadCount;

  readonly convId = computed(() => this.chatService.conversationId());
  readonly isClosed = computed(() => this.convStatus() === 'CLOSED');
  readonly isWaiting = computed(() => this.convStatus() === 'WAITING_STAFF');

  private newMsgSub?: Subscription;
  private typingTimer?: ReturnType<typeof setTimeout>;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;

    // Subscribe to new messages to handle scroll and unread count
    this.newMsgSub = this.chatService.newMessage$.subscribe((msg) => {
      if (!this.isOpen()) {
        this.chatService.unreadCount.update((c) => c + 1);
      } else {
        this.shouldScrollBottom.set(true);
      }
    });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScrollBottom() && this.messageListEl) {
      this.scrollToBottom();
      this.shouldScrollBottom.set(false);
    }
  }

  ngOnDestroy(): void {
    this.newMsgSub?.unsubscribe();
    this.chatService.disconnect();
  }

  // ─── Toggle Widget ─────────────────────────────────────────────────────────
  toggleWidget(): void {
    this.isOpen.update((v) => !v);
    if (this.isOpen()) {
      this.chatService.unreadCount.set(0);
      if (!this.convId()) {
        this.initChat();
      }
    }
  }

  closeWidget(): void {
    this.isOpen.set(false);
  }

  // ─── Init Chat ─────────────────────────────────────────────────────────────
  private initChat(): void {
    this.isLoading.set(true);
    this.chatService.initConversation().subscribe({
      next: (resp) => {
        if (resp.success && resp.data) {
          const data = resp.data;
          this.chatService.conversationId.set(data.conversationId);
          this.chatService.status.set(data.status);

          // Load message history then connect WS
          this.loadHistory(data.conversationId);
          this.chatService.connectAsCustomer(data.conversationId);
        }
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  private loadHistory(convId: number): void {
    this.chatService.loadMessages(convId).subscribe({
      next: (resp) => {
        if (resp.success && resp.data) {
          this.chatService.messages.set(resp.data);
          this.shouldScrollBottom.set(true);
        }
      },
    });
  }

  // ─── Send Message ──────────────────────────────────────────────────────────
  sendMessage(): void {
    const text = this.inputText().trim();
    if (!text || this.isSending() || this.isClosed()) return;

    const convId = this.convId();
    if (!convId) return;

    this.isSending.set(true);
    this.inputText.set('');
    this.resetTextareaHeight();

    // Send via WebSocket
    this.chatService.sendMessageViaWs(convId, text);

    // Optimistic local message for immediate feedback
    const optimistic: ChatMessageDto = {
      messageId: Date.now(),
      conversationId: convId,
      senderType: 'CUSTOMER',
      senderId: null,
      senderName: this.authService.currentUser()?.fullName ?? 'Bạn',
      content: text,
      attachmentUrl: null,
      quickReplies: [],
      read: false,
      createdAt: new Date().toISOString(),
    };
    this.chatService.messages.update((msgs) => [...msgs, optimistic]);
    this.shouldScrollBottom.set(true);

    // The server will broadcast the real message back; we deduplicate by ignoring
    // duplicates with same content+timestamp. For now we allow both (server echo
    // is identified by messageId from DB which won't equal Date.now()).
    setTimeout(() => this.isSending.set(false), 500);
  }

  sendQuickReply(text: string): void {
    const convId = this.convId();
    if (!convId || this.isClosed()) return;
    this.chatService.sendMessageViaWs(convId, text);

    const optimistic: ChatMessageDto = {
      messageId: Date.now(),
      conversationId: convId,
      senderType: 'CUSTOMER',
      senderId: null,
      senderName: this.authService.currentUser()?.fullName ?? 'Bạn',
      content: text,
      attachmentUrl: null,
      quickReplies: [],
      read: false,
      createdAt: new Date().toISOString(),
    };
    this.chatService.messages.update((msgs) => [...msgs, optimistic]);
    this.shouldScrollBottom.set(true);
  }

  // ─── Image Upload ──────────────────────────────────────────────────────────
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];

    const convId = this.convId();
    if (!convId || this.isClosed()) return;

    this.isUploadingImage.set(true);
    this.chatService.uploadChatImage(file).subscribe({
      next: (resp) => {
        if (resp.success && resp.data?.url) {
          this.chatService.sendMessageViaWs(convId, '', resp.data.url);
        }
        this.isUploadingImage.set(false);
      },
      error: () => this.isUploadingImage.set(false),
    });

    // Reset file input
    input.value = '';
  }

  // ─── Typing ───────────────────────────────────────────────────────────────
  onInputChange(event: Event): void {
    const target = event.target as HTMLTextAreaElement;
    this.inputText.set(target.value);
    this.autoResizeTextarea(target);

    // Typing signal throttle
    const convId = this.convId();
    if (!convId) return;

    clearTimeout(this.typingTimer);
    const user = this.authService.currentUser();
    this.chatService.sendTypingSignal(convId, true, 'CUSTOMER', user?.fullName ?? 'Khách');
    this.typingTimer = setTimeout(() => {
      this.chatService.sendTypingSignal(convId, false, 'CUSTOMER', user?.fullName ?? 'Khách');
    }, 2000);
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  // ─── Scroll ───────────────────────────────────────────────────────────────
  private scrollToBottom(): void {
    try {
      const el = this.messageListEl?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    } catch (e) {}
  }

  onScroll(event: Event): void {
    const el = event.target as HTMLDivElement;
    const distFromBottom = el.scrollHeight - el.scrollTop - el.clientHeight;
    this.showScrollDown.set(distFromBottom > 120);
  }

  scrollDown(): void {
    this.shouldScrollBottom.set(true);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────
  private autoResizeTextarea(el: HTMLTextAreaElement): void {
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 120) + 'px';
  }

  private resetTextareaHeight(): void {
    if (this.inputEl?.nativeElement) {
      this.inputEl.nativeElement.style.height = 'auto';
    }
  }

  formatTime(iso: string): string {
    return new Date(iso).toLocaleTimeString('vi-VN', {
      hour: '2-digit',
      minute: '2-digit',
    });
  }

  isCustomer(msg: ChatMessageDto): boolean {
    return msg.senderType === 'CUSTOMER';
  }

  isBot(msg: ChatMessageDto): boolean {
    return msg.senderType === 'BOT';
  }

  isSystem(msg: ChatMessageDto): boolean {
    return msg.senderType === 'SYSTEM';
  }

  getStatusLabel(status: ConversationStatus | null): string {
    switch (status) {
      case 'BOT_ACTIVE': return 'Trợ lý tự động';
      case 'WAITING_STAFF': return 'Đang kết nối nhân viên...';
      case 'STAFF_ACTIVE': return 'Đang kết nối';
      case 'CLOSED': return 'Hội thoại đã kết thúc';
      default: return 'Chat với Complexus';
    }
  }

  getStatusDotClass(status: ConversationStatus | null): string {
    switch (status) {
      case 'BOT_ACTIVE': return 'dot--bot';
      case 'WAITING_STAFF': return 'dot--waiting';
      case 'STAFF_ACTIVE': return 'dot--active';
      case 'CLOSED': return 'dot--closed';
      default: return '';
    }
  }
}
