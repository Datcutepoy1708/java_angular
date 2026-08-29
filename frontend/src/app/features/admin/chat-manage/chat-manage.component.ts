import {
  Component,
  OnInit,
  OnDestroy,
  signal,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ChatService } from '../../../core/services/chat.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  ChatConversationSummaryResponse,
  ChatMessageDto,
  ConversationStatus,
  ChatTypingSignal,
} from '../../../core/models/chat.model';

@Component({
  selector: 'app-chat-manage',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-manage.component.html',
  styleUrls: ['./chat-manage.component.scss'],
})
export class ChatManageComponent implements OnInit, OnDestroy {
  private readonly chatService = inject(ChatService);
  private readonly authService = inject(AuthService);

  // Queue
  readonly queueStatus = signal<ConversationStatus>('WAITING_STAFF');
  readonly conversations = signal<ChatConversationSummaryResponse[]>([]);
  readonly isLoadingQueue = signal(false);

  // Active conversation
  readonly activeConv = signal<ChatConversationSummaryResponse | null>(null);
  readonly messages = signal<ChatMessageDto[]>([]);
  readonly isLoadingMessages = signal(false);
  readonly isSending = signal(false);
  readonly inputText = signal('');
  readonly isTyping = signal(false);
  readonly isConnectedWs = signal(false);

  private typingTimer?: ReturnType<typeof setTimeout>;
  private queueRefreshTimer?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.loadQueue();
    // Auto-refresh queue every 30s
    this.queueRefreshTimer = setInterval(() => this.loadQueue(), 30_000);
  }

  ngOnDestroy(): void {
    clearInterval(this.queueRefreshTimer);
    this.chatService.disconnect();
  }

  // ─── Queue ────────────────────────────────────────────────────────────────

  loadQueue(): void {
    this.isLoadingQueue.set(true);
    this.chatService.getConversationsByStatus(this.queueStatus()).subscribe({
      next: (resp) => {
        if (resp.success) this.conversations.set(resp.data);
        this.isLoadingQueue.set(false);
      },
      error: () => this.isLoadingQueue.set(false),
    });
  }

  setQueueTab(status: ConversationStatus): void {
    this.queueStatus.set(status);
    this.loadQueue();
  }

  // ─── Open Conversation ────────────────────────────────────────────────────

  openConversation(conv: ChatConversationSummaryResponse): void {
    if (this.activeConv()?.conversationId === conv.conversationId) return;

    // Disconnect from previous
    this.chatService.disconnect();

    this.activeConv.set(conv);
    this.messages.set([]);
    this.isLoadingMessages.set(true);

    this.chatService.getAdminMessages(conv.conversationId).subscribe({
      next: (resp) => {
        if (resp.success) this.messages.set(resp.data);
        this.isLoadingMessages.set(false);
      },
      error: () => this.isLoadingMessages.set(false),
    });

    // Connect WebSocket
    const user = this.authService.currentUser();
    this.chatService.connectAsStaff(
      conv.conversationId,
      (msg: ChatMessageDto) => {
        this.messages.update((msgs) => [...msgs, msg]);
      },
      (typing: ChatTypingSignal) => {
        if (typing.senderType === 'CUSTOMER') {
          this.isTyping.set(typing.isTyping);
          if (typing.isTyping) {
            clearTimeout(this.typingTimer);
            this.typingTimer = setTimeout(() => this.isTyping.set(false), 3000);
          }
        }
      }
    );
    this.isConnectedWs.set(true);

    // Mark read
    this.chatService.markReadByStaff(conv.conversationId).subscribe();
  }

  // ─── Claim ────────────────────────────────────────────────────────────────

  claimConversation(conv: ChatConversationSummaryResponse, event: Event): void {
    event.stopPropagation();
    this.chatService.claimConversation(conv.conversationId).subscribe({
      next: (resp) => {
        if (resp.success) {
          this.openConversation(resp.data);
          this.loadQueue();
        }
      },
      error: (err) => {
        alert(err?.error?.message ?? 'Hội thoại đã được nhân viên khác tiếp nhận');
        this.loadQueue();
      },
    });
  }

  // ─── Close ────────────────────────────────────────────────────────────────

  closeConversation(): void {
    const conv = this.activeConv();
    if (!conv) return;
    if (!confirm('Bạn có chắc muốn kết thúc hội thoại này?')) return;

    this.chatService.closeConversation(conv.conversationId).subscribe({
      next: () => {
        this.activeConv.set(null);
        this.chatService.disconnect();
        this.loadQueue();
      },
    });
  }

  // ─── Send Message ─────────────────────────────────────────────────────────

  sendMessage(): void {
    const text = this.inputText().trim();
    const conv = this.activeConv();
    if (!text || !conv || this.isSending()) return;

    this.isSending.set(true);
    this.inputText.set('');

    this.chatService.sendMessageViaWs(conv.conversationId, text);

    // Optimistic message
    const user = this.authService.currentUser();
    const optimistic: ChatMessageDto = {
      messageId: Date.now(),
      conversationId: conv.conversationId,
      senderType: 'STAFF',
      senderId: null,
      senderName: user?.fullName ?? 'Nhân viên',
      content: text,
      attachmentUrl: null,
      quickReplies: [],
      read: true,
      createdAt: new Date().toISOString(),
    };
    this.messages.update((msgs) => [...msgs, optimistic]);
    setTimeout(() => this.isSending.set(false), 500);
  }

  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  onInputChange(event: Event): void {
    const target = event.target as HTMLTextAreaElement;
    this.inputText.set(target.value);

    const conv = this.activeConv();
    if (!conv) return;
    const user = this.authService.currentUser();
    clearTimeout(this.typingTimer);
    this.chatService.sendTypingSignal(conv.conversationId, true, 'STAFF', user?.fullName ?? 'Nhân viên');
    this.typingTimer = setTimeout(() => {
      this.chatService.sendTypingSignal(conv.conversationId, false, 'STAFF', user?.fullName ?? 'Nhân viên');
    }, 2000);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  formatTime(iso: string): string {
    return new Date(iso).toLocaleString('vi-VN', {
      hour: '2-digit', minute: '2-digit', day: '2-digit', month: '2-digit',
    });
  }

  getStatusLabel(status: ConversationStatus): string {
    const labels: Record<ConversationStatus, string> = {
      BOT_ACTIVE: 'Bot đang xử lý',
      WAITING_STAFF: 'Chờ nhân viên',
      STAFF_ACTIVE: 'Đang xử lý',
      CLOSED: 'Đã kết thúc',
    };
    return labels[status] ?? status;
  }

  getStatusClass(status: ConversationStatus): string {
    const classes: Record<ConversationStatus, string> = {
      BOT_ACTIVE: 'badge--bot',
      WAITING_STAFF: 'badge--waiting',
      STAFF_ACTIVE: 'badge--active',
      CLOSED: 'badge--closed',
    };
    return classes[status] ?? '';
  }

  isCustomer(msg: ChatMessageDto): boolean { return msg.senderType === 'CUSTOMER'; }
  isSystem(msg: ChatMessageDto): boolean { return msg.senderType === 'SYSTEM'; }
  isStaff(msg: ChatMessageDto): boolean { return msg.senderType === 'STAFF' || msg.senderType === 'BOT'; }
}
