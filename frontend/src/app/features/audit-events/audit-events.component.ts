import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AgGridAngular } from 'ag-grid-angular';
import { AllCommunityModule, ColDef, ModuleRegistry } from 'ag-grid-community';
import { AuditEventsStore } from '../../core/state/audit-events.store';

ModuleRegistry.registerModules([AllCommunityModule]);

@Component({
  selector: 'app-audit-events',
  standalone: true,
  imports: [AgGridAngular, RouterLink],
  templateUrl: './audit-events.component.html',
  styleUrl: './audit-events.component.css'
})
export class AuditEventsComponent {
  private readonly router = inject(Router);
  readonly store = inject(AuditEventsStore);

  readonly rowData = computed(() =>
    this.store.events().map((event) => ({
      eventId: event.id ?? event.eventId,
      eventType: event.eventType,
      actor: event.actor,
      sourceSystem: event.sourceSystem ?? event.metadata?.['sourceSystem'] ?? event.target ?? 'N/A',
      status: event.status,
      riskScore: (event.payload?.['riskScore'] as number | undefined) ?? (event.metadata?.['riskScore'] as number | undefined) ?? 0,
      confidenceScore: (event.payload?.['confidenceScore'] as number | undefined) ?? (event.metadata?.['confidenceScore'] as number | undefined) ?? 0,
      toolsUsed: Array.isArray(event.payload?.['toolExecutions']) ? event.payload['toolExecutions'].length : 0,
      createdAt: event.timestamp ?? event.eventTime
    }))
  );

  constructor() {
    void this.store.loadEvents();
  }

  columnDefs: ColDef[] = [
    { field: 'eventId', headerName: 'Event ID', flex: 2 },
    { field: 'eventType', headerName: 'Event Type', flex: 2 },
    { field: 'actor', headerName: 'Actor', flex: 2 },
    { field: 'sourceSystem', headerName: 'Source System', flex: 2 },
    { field: 'status', headerName: 'Risk Status', flex: 1 },
    { field: 'riskScore', headerName: 'Risk', flex: 1 },
    { field: 'confidenceScore', headerName: 'Confidence', flex: 1 },
    { field: 'toolsUsed', headerName: 'Tools', flex: 1 },
    { field: 'createdAt', headerName: 'Created At', flex: 2 }
  ];

  defaultColDef: ColDef = { sortable: true, filter: true, resizable: true };

  openAnalysis(event: any): void {
    this.router.navigate(['/analysis', event.data.eventId]);
  }
}
