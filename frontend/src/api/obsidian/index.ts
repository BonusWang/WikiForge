import { http, type ApiResponse } from '../../services/http';
import type {
  ObsidianInitResult,
  ObsidianNote,
  ObsidianNotePreview,
  ObsidianVaultStatus,
  SourceNoteDraft,
  WriteSourceNoteRequest
} from '../../types/obsidianNotes';

function unwrapResponse<T>(response: ApiResponse<T | null>): T {
  if (!response.success || response.data === null) {
    throw new Error(response.message || 'Request failed');
  }
  return response.data;
}

export async function initializeObsidianVault(): Promise<ObsidianInitResult> {
  const response = await http.post<ApiResponse<ObsidianInitResult | null>>('/v1/obsidian/init');
  return unwrapResponse(response.data);
}

export async function getObsidianStatus(): Promise<ObsidianVaultStatus> {
  const response = await http.get<ApiResponse<ObsidianVaultStatus | null>>('/v1/obsidian/status');
  return unwrapResponse(response.data);
}

export async function getSourceFileObsidianNote(fileUid: string): Promise<ObsidianNote | null> {
  const response = await http.get<ApiResponse<ObsidianNote | null>>(
    `/v1/source-files/${encodeURIComponent(fileUid)}/obsidian-note`
  );
  if (!response.data.success) {
    throw new Error(response.data.message || 'Request failed');
  }
  return response.data.data;
}

export async function generateSourceNoteDraft(fileUid: string): Promise<SourceNoteDraft> {
  const response = await http.post<ApiResponse<SourceNoteDraft | null>>(
    `/v1/source-files/${encodeURIComponent(fileUid)}/obsidian-note/draft`
  );
  return unwrapResponse(response.data);
}

export async function writeSourceNote(
  fileUid: string,
  payload: WriteSourceNoteRequest
): Promise<ObsidianNote> {
  const response = await http.post<ApiResponse<ObsidianNote | null>>(
    `/v1/source-files/${encodeURIComponent(fileUid)}/obsidian-note/write`,
    payload
  );
  return unwrapResponse(response.data);
}

export async function previewObsidianNote(noteUid: string): Promise<ObsidianNotePreview> {
  const response = await http.get<ApiResponse<ObsidianNotePreview | null>>(
    `/v1/obsidian/notes/${encodeURIComponent(noteUid)}/preview`
  );
  return unwrapResponse(response.data);
}
