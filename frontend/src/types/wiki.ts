export type WikiPageType = 'topic' | 'project';
export type WikiPageStatus = 'active' | 'archived';
export type WikiIntegrationStatus = 'pending_review' | 'auto_applied' | 'approved' | 'rejected';
export type WikiRiskLevel = 'low' | 'medium' | 'high';

export interface CreateWikiPageRequest {
  pageType: WikiPageType;
  title: string;
  slug?: string;
  vaultPath: string;
  status?: WikiPageStatus;
}

export interface WikiPage {
  pageUid: string;
  pageType: WikiPageType;
  title: string;
  slug: string;
  vaultPath: string;
  status: WikiPageStatus;
  createdAt: string;
}

export interface WikiPageListParams {
  type?: WikiPageType;
  status?: WikiPageStatus;
  page?: number;
  pageSize?: number;
}

export interface CreateWikiCompileRunRequest {
  targetPageUid?: string;
  riskLevel?: WikiRiskLevel;
  confidenceScore?: number;
  changeSummary?: string;
  proposedMarkdown?: string;
}

export interface WikiCompileRunResponse {
  runUid: string;
  integrationUid: string;
  status: WikiIntegrationStatus;
  finalDecision: string;
}

export interface WikiIntegration {
  integrationUid: string;
  pageUid?: string | null;
  pageTitle?: string | null;
  pageType?: WikiPageType | null;
  vaultPath?: string | null;
  sourceUid: string;
  sourceFileUid?: string | null;
  runUid: string;
  status: WikiIntegrationStatus;
  riskLevel: WikiRiskLevel;
  confidenceScore: number;
  changeSummary?: string | null;
  proposedMarkdown?: string | null;
  appliedAt?: string | null;
  createdAt: string;
}

export interface WikiIntegrationListParams {
  status?: WikiIntegrationStatus;
  pageUid?: string;
  sourceUid?: string;
  page?: number;
  pageSize?: number;
}

export interface WikiIntegrationDecisionRequest {
  reason?: string;
  markdown?: string;
}

export interface WikiPagePageResponse {
  items: WikiPage[];
  page: number;
  pageSize: number;
  total: number;
}

export interface WikiIntegrationPageResponse {
  items: WikiIntegration[];
  page: number;
  pageSize: number;
  total: number;
}
