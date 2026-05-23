import { http, type ApiResponse } from '../../services/http';
import type {
  CreateLinkSourceRequest,
  CreatePersonalRecordRequest,
  LinkSourceResponse,
  PersonalRecord,
  PersonalRecordListParams,
  PersonalRecordObsidianNoteResponse,
  PersonalRecordPageResponse,
  PersonalRecordSummaryResponse
} from '../../types/lifeos';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function createLinkSource(payload: CreateLinkSourceRequest): Promise<LinkSourceResponse> {
  const response = await http.post<ApiResponse<LinkSourceResponse | null>>('/v1/link-sources', payload);
  return unwrapResponse(response.data);
}

export async function createPersonalRecord(
  payload: CreatePersonalRecordRequest
): Promise<PersonalRecord> {
  const response = await http.post<ApiResponse<PersonalRecord | null>>('/v1/personal-records', payload);
  return unwrapResponse(response.data);
}

export async function listPersonalRecords(
  params: PersonalRecordListParams = {}
): Promise<PersonalRecordPageResponse> {
  const response = await http.get<ApiResponse<PersonalRecordPageResponse | null>>('/v1/personal-records', {
    params
  });
  return unwrapResponse(response.data);
}

export async function getPersonalRecord(recordUid: string): Promise<PersonalRecord> {
  const response = await http.get<ApiResponse<PersonalRecord | null>>(`/v1/personal-records/${recordUid}`);
  return unwrapResponse(response.data);
}

export async function getPersonalRecordSummary(period = 'all'): Promise<PersonalRecordSummaryResponse> {
  const response = await http.get<ApiResponse<PersonalRecordSummaryResponse | null>>('/v1/personal-records/summary', {
    params: { period }
  });
  return unwrapResponse(response.data);
}

export async function writePersonalRecordObsidianNote(
  recordUid: string
): Promise<PersonalRecordObsidianNoteResponse> {
  const response = await http.post<ApiResponse<PersonalRecordObsidianNoteResponse | null>>(
    `/v1/personal-records/${recordUid}/obsidian-note`
  );
  return unwrapResponse(response.data);
}
