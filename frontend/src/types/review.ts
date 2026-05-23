export type ReviewItemStatus = 'pending' | 'approved' | 'rejected' | string;

export interface CreateAiReviewRunRequest {
  providerName?: string;
  modelName?: string;
  baseUrl?: string;
  providerType?: string;
  configSource?: string;
}

export interface AiReviewRun {
  runUid: string;
  sourceUid?: string | null;
  sourceFileUid?: string | null;
  status: string;
  currentStep?: string | null;
  modelProvider?: string | null;
  modelName?: string | null;
  reviewItemUid?: string | null;
  reviewStatus?: ReviewItemStatus | null;
  createdAt: string;
}

export interface ReviewItem {
  reviewUid: string;
  sourceUid?: string | null;
  sourceFileUid?: string | null;
  runUid?: string | null;
  reviewType: string;
  status: ReviewItemStatus;
  reason?: string | null;
  suggestedChanges?: string | null;
  markdownDraft?: string | null;
  createdAt: string;
}

export interface ReviewItemPage {
  items: ReviewItem[];
  page: number;
  pageSize: number;
  total: number;
}

export interface ReviewItemListParams {
  status?: ReviewItemStatus;
  page?: number;
  pageSize?: number;
}
