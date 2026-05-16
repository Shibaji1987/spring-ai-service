import { PolicyEvidence } from './policy-evidence.model';
import { ToolExecution } from './tool-execution.model';

export interface AnalysisResult {

  eventId: string;

  riskScore: number;

  confidenceScore: number;

  category: string;

  summary: string;

  recommendedAction: string;

  reasoningTrace: string[];

  diagnostics: Record<string, any>;

  toolExecutions: ToolExecution[];

  matchedPolicyEvidence: PolicyEvidence[];

}
