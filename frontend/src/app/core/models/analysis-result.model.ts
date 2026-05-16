import { PolicyEvidence } from './policy-evidence.model';
import { ToolExecution } from './tool-execution.model';

export interface AnalysisResult {

  eventId?: string;

  auditEventId?: string;

  riskScore: number;

  confidenceScore?: number;

  category: string;

  summary: string;

  recommendedAction: string;

  reasons?: string[];

  tags?: string[];

  reasoningTrace?: string[];

  diagnostics?: Record<string, any>;

  toolExecutions?: ToolExecution[];

  matchedPolicyEvidence?: PolicyEvidence[];

}
