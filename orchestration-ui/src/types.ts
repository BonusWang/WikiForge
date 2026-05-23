export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message: string;
  code: string | null;
}

export interface TaskStats {
  total: number;
  ready: number;
  doing: number;
  review: number;
  blocked: number;
  done: number;
}

export interface OrchestrationOverview {
  mode: string;
  workflowEntry: string;
  projectRoadmap: string;
  activeBranch: string;
  currentStage: string;
  source: string;
  stats: TaskStats;
  nextActions: string[];
}

export interface OrchestrationTask {
  taskId: string;
  parentTask: string;
  title: string;
  status: 'Backlog' | 'Ready' | 'Doing' | 'Review' | 'Blocked' | 'Done' | string;
  owner: string;
  goal: string;
  scope: string;
  allowedFiles: string[];
  forbiddenFiles: string[];
  contracts: string[];
  verificationCommands: string[];
  handoff: string;
  nextStep: string;
  tags: string[];
}
