export interface CreateLinkSourceRequest {
  title: string;
  sourceUrl: string;
  sourcePlatform?: string;
  rawContent?: string;
  sourceType?: string;
  processingIntent?: string;
}

export interface LinkSourceResponse {
  sourceUid: string;
  fileUid: string;
  jobUid: string;
  title: string;
  sourceUrl: string;
  sourcePlatform: string;
  status: string;
  createdAt: string;
}

export type PersonalRecordType = 'expense' | 'bill' | 'email' | 'relationship' | 'event' | 'note' | string;
export type SensitivityLevel = 'low' | 'medium' | 'high';

export interface CreatePersonalRecordRequest {
  recordType: PersonalRecordType;
  title: string;
  occurredAt?: string;
  rawContent: string;
  sourceChannel?: string;
  sourceRef?: string;
  structured?: Record<string, unknown>;
  sensitivityLevel?: SensitivityLevel;
  createdBy?: string;
}

export interface PersonalRecord {
  recordUid: string;
  recordType: PersonalRecordType;
  title: string;
  occurredAt?: string | null;
  sourceChannel: string;
  sourceRef?: string | null;
  rawContent: string;
  structuredJson?: string | null;
  status: string;
  sensitivityLevel: SensitivityLevel;
  createdBy: string;
  obsidianVaultPath?: string | null;
  obsidianUri?: string | null;
  archivedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface PersonalRecordPageResponse {
  items: PersonalRecord[];
  page: number;
  pageSize: number;
  total: number;
}

export interface PersonalRecordSummaryResponse {
  period: string;
  total: number;
  byType: Record<string, number>;
  byStatus: Record<string, number>;
  recentItems: PersonalRecord[];
}

export interface PersonalRecordObsidianNoteResponse {
  recordUid: string;
  title: string;
  vaultName: string;
  vaultPath: string;
  obsidianUri: string;
  status: string;
  archivedAt: string;
}

export interface PersonalRecordListParams {
  recordType?: string;
  status?: string;
  sourceChannel?: string;
  page?: number;
  pageSize?: number;
}
