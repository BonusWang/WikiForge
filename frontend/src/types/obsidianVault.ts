export interface ObsidianInitResult {
  vaultName: string;
  managedRoot: string;
  createdPaths: string[];
}

export interface ObsidianVaultStatus {
  vaultName: string;
  vaultPathMasked?: string | null;
  managedRoot: string;
  exists: boolean;
  writable: boolean;
  managedRootExists: boolean;
  lastWriteAt?: string | null;
  failureReason?: string | null;
}
