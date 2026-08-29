import {
  Component,
  OnInit,
  signal,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ChatService } from '../../../core/services/chat.service';
import {
  ChatBotRuleRequest,
  ChatBotRuleResponse,
  MatchType,
  RuleActionType,
} from '../../../core/models/chat.model';

@Component({
  selector: 'app-bot-rules-manage',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './bot-rules-manage.component.html',
  styleUrls: ['./bot-rules-manage.component.scss'],
})
export class BotRulesManageComponent implements OnInit {
  private readonly chatService = inject(ChatService);
  private readonly fb = inject(FormBuilder);

  readonly rules = signal<ChatBotRuleResponse[]>([]);
  readonly isLoading = signal(false);
  readonly isSaving = signal(false);
  readonly showForm = signal(false);
  readonly editingId = signal<number | null>(null);

  readonly matchTypes: MatchType[] = ['CONTAINS', 'EXACT', 'REGEX'];
  readonly actionTypes: RuleActionType[] = ['REPLY', 'HANDOVER_STAFF', 'CLOSE'];

  readonly matchTypeLabels: Record<MatchType, string> = {
    CONTAINS: 'Chứa từ khóa',
    EXACT: 'Khớp chính xác',
    REGEX: 'Regex',
  };
  readonly actionTypeLabels: Record<RuleActionType, string> = {
    REPLY: 'Trả lời tự động',
    HANDOVER_STAFF: 'Chuyển nhân viên',
    CLOSE: 'Đóng hội thoại',
  };

  form!: FormGroup;

  ngOnInit(): void {
    this.buildForm();
    this.loadRules();
  }

  private buildForm(): void {
    this.form = this.fb.group({
      ruleName: ['', [Validators.required, Validators.maxLength(150)]],
      keywords: ['', Validators.required],
      matchType: ['CONTAINS' as MatchType, Validators.required],
      responseMessage: ['', Validators.required],
      quickReplies: [''],
      actionType: ['REPLY' as RuleActionType, Validators.required],
      priority: [0, [Validators.required, Validators.min(0)]],
      active: [true],
    });
  }

  loadRules(): void {
    this.isLoading.set(true);
    this.chatService.getAllBotRules().subscribe({
      next: (resp) => {
        if (resp.success) this.rules.set(resp.data.sort((a, b) => b.priority - a.priority));
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  openCreateForm(): void {
    this.editingId.set(null);
    this.form.reset({
      ruleName: '',
      keywords: '',
      matchType: 'CONTAINS',
      responseMessage: '',
      quickReplies: '',
      actionType: 'REPLY',
      priority: 0,
      active: true,
    });
    this.showForm.set(true);
  }

  openEditForm(rule: ChatBotRuleResponse): void {
    this.editingId.set(rule.ruleId);
    this.form.patchValue({
      ruleName: rule.ruleName,
      keywords: rule.keywords,
      matchType: rule.matchType,
      responseMessage: rule.responseMessage,
      quickReplies: rule.quickReplies?.join(', ') ?? '',
      actionType: rule.actionType,
      priority: rule.priority,
      active: this.isRuleActive(rule),
    });
    this.showForm.set(true);
  }

  isRuleActive(rule: ChatBotRuleResponse): boolean {
    return rule.isActive ?? rule.active ?? true;
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  saveRule(): void {
    if (this.form.invalid || this.isSaving()) return;
    this.isSaving.set(true);

    const v = this.form.value;
    // Convert comma-separated quickReplies string to JSON array string
    const quickRepliesArray = v.quickReplies
      ? v.quickReplies.split(',').map((s: string) => s.trim()).filter((s: string) => s)
      : [];

    const req: ChatBotRuleRequest = {
      ruleName: v.ruleName,
      keywords: v.keywords,
      matchType: v.matchType,
      responseMessage: v.responseMessage,
      quickReplies: JSON.stringify(quickRepliesArray),
      actionType: v.actionType,
      priority: v.priority,
      active: v.active,
    };

    const editId = this.editingId();
    const obs = editId
      ? this.chatService.updateBotRule(editId, req)
      : this.chatService.createBotRule(req);

    obs.subscribe({
      next: (resp) => {
        if (resp.success) {
          this.loadRules();
          this.cancelForm();
        }
        this.isSaving.set(false);
      },
      error: () => this.isSaving.set(false),
    });
  }

  deleteRule(rule: ChatBotRuleResponse): void {
    if (!confirm(`Xóa rule "${rule.ruleName}"?`)) return;
    this.chatService.deleteBotRule(rule.ruleId).subscribe({
      next: () => this.loadRules(),
    });
  }

  getActionClass(type: RuleActionType): string {
    const m: Record<RuleActionType, string> = {
      REPLY: 'action--reply',
      HANDOVER_STAFF: 'action--handover',
      CLOSE: 'action--close',
    };
    return m[type] ?? '';
  }
}
