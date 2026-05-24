export interface AppSettings {
  rawSourcesRootMasked: string;
  obsidianVaultPathMasked: string;
  obsidianManagedRoot: string;
  allowedScanRoots: string[];
  maxUploadFileSizeMb: number;
  autoWikiWriteback: boolean;
  modelProviderConfigured: boolean;
}

export interface UpdateAppSettingsRequest {
  rawSourcesRoot?: string;
  obsidianVaultPath?: string;
  allowedScanRoots?: string[];
  autoWikiWriteback?: boolean;
}
