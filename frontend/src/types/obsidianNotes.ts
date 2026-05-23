export interface ObsidianInitResult {
  vaultName: string;
  vaultPath: string;
  createdDirectories: string[];
}

export interface SourceNoteDraft {
  fileUid: string;
  sourceUid: string;
  title: string;
  vaultName: string;
  vaultPath: string;
  markdown: string;
}

export interface WriteSourceNoteRequest {
  markdown?: string;
}

export interface ObsidianNote {
  noteUid: string;
  fileUid: string;
  sourceUid: string;
  title: string;
  vaultName: string;
  vaultPath: string;
  absolutePath: string;
  obsidianUri: string;
  contentHash: string;
  status: string;
  createdAt: string;
}

export interface ObsidianNotePreview {
  noteUid: string;
  title: string;
  vaultName: string;
  vaultPath: string;
  obsidianUri: string;
  markdown: string;
}
