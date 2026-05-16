import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AnalysisResult } from '../models/analysis-result.model';
import { ToolExecution } from '../models/tool-execution.model';
import { AnalysisApiService } from '../services/analysis-api.service';

@Injectable({ providedIn: 'root' })
export class AnalysisStore {
  private readonly analysisApi = inject(AnalysisApiService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly selectedAnalysis = signal<AnalysisResult | null>(null);
  readonly toolTrace = signal<ToolExecution[]>([]);

  async analyzeEvent(eventId: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const result = await firstValueFrom(this.analysisApi.analyzeEvent(eventId));
      this.selectedAnalysis.set(result);
      this.toolTrace.set(result.toolExecutions ?? []);
    } catch {
      this.error.set('Failed to run AI analysis.');
    } finally {
      this.loading.set(false);
    }
  }

  async loadFullAnalysis(eventId: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const result = await firstValueFrom(this.analysisApi.getFullAnalysis(eventId));
      this.selectedAnalysis.set(result);
      this.toolTrace.set(result.toolExecutions ?? []);
    } catch {
      this.error.set('Failed to load analysis details.');
    } finally {
      this.loading.set(false);
    }
  }
}
