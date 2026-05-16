import { DecimalPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AnalysisStore } from '../../core/state/analysis.store';
import { RiskBadgeComponent } from '../../shared/ui/risk-badge/risk-badge.component';
import { ToolTraceCardComponent } from '../../shared/ui/tool-trace-card/tool-trace-card.component';

@Component({
  selector: 'app-analysis-details',
  standalone: true,
  imports: [RiskBadgeComponent, ToolTraceCardComponent, DecimalPipe, RouterLink],
  templateUrl: './analysis-details.component.html',
  styleUrl: './analysis-details.component.css'
})
export class AnalysisDetailsComponent {
  private readonly route = inject(ActivatedRoute);
  readonly analysisStore = inject(AnalysisStore);

  readonly analysis = computed(() => this.analysisStore.selectedAnalysis());
  readonly eventId = this.route.snapshot.paramMap.get('eventId');

  constructor() {
    if (this.eventId) {
      void this.analysisStore.loadFullAnalysis(this.eventId);
    }
  }
}
