export type ConversationStatus = 'BOT_ACTIVE' | 'WAITING_STAFF' | 'STAFF_ACTIVE' | 'CLOSED';
export type MessageSenderType = 'CUSTOMER' | 'BOT' | 'STAFF' | 'SYSTEM';
export type MatchType = 'CONTAINS' | 'EXACT' | 'REGEX';
export type RuleActionType = 'REPLY' | 'HANDOVER_STAFF' | 'CLOSE';

export interface ChatInitRequest {
  sessionId: string;
  customerName?: string;
  customerEmail?: string;
  customerPhone?: string;
}

export interface ChatInitResponse {
  conversationId: number;
  sessionId: string;
  status: ConversationStatus;
  unreadCustomerCount: number;
  createdAt: string;
}

export interface ChatMessageDto {
  messageId: number;
  conversationId: number;
  senderType: MessageSenderType;
  senderId: number | null;
  senderName: string;
  content: string;
  attachmentUrl: string | null;
  quickReplies: string[];
  read: boolean;
  createdAt: string;
}

export interface ChatConversationSummaryResponse {
  conversationId: number;
  sessionId: string;
  userId: number | null;
  staffId: number | null;
  staffName: string | null;
  customerName: string | null;
  customerEmail: string | null;
  customerPhone: string | null;
  status: ConversationStatus;
  unreadStaffCount: number;
  unreadCustomerCount: number;
  lastMessage: string | null;
  lastMessageAt: string | null;
  createdAt: string;
}

export interface ChatBotRuleRequest {
  ruleName: string;
  keywords: string;
  matchType: MatchType;
  responseMessage: string;
  quickReplies?: string;
  actionType: RuleActionType;
  priority: number;
  active: boolean;
}

export interface ChatBotRuleResponse {
  ruleId: number;
  ruleName: string;
  keywords: string;
  matchType: MatchType;
  responseMessage: string;
  quickReplies: string[];
  actionType: RuleActionType;
  priority: number;
  isActive?: boolean;
  active?: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ChatTypingSignal {
  conversationId: number;
  senderType: MessageSenderType;
  senderName: string;
  isTyping: boolean;
}

export interface ChatUploadResponse {
  url: string;
}
