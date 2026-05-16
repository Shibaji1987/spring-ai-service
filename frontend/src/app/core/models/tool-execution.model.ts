export interface ToolExecution {

  toolName: string;

  status:
    | 'SUCCESS'
    | 'FAILED'
    | 'RUNNING'
    | 'FALLBACK_USED'
    | 'RETRYING'
    | 'SKIPPED';

  durationMs: number;

  confidence: number;

  input: Record<string, any>;

  output: Record<string, any>;

  reasoning?: string;

  errorMessage?: string;

  fallbackTool?: string;

  retryCount?: number;

}
