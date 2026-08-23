export type BulkActionType = 'delete' | 'restore' | 'activate' | 'deactivate';

export interface BulkActionRequest {
  ids: number[];
  action: BulkActionType | string;
}

export interface BulkItemResult {
  id: number;
  success: boolean;
  error?: string | null;
}

export interface BulkActionResult {
  successCount: number;
  failCount: number;
  results: BulkItemResult[];
}
