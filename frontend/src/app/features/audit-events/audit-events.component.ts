import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AgGridAngular } from 'ag-grid-angular';
import {
  AllCommunityModule,
  ColDef,
  ModuleRegistry
} from 'ag-grid-community';

ModuleRegistry.registerModules([AllCommunityModule]);

@Component({
  selector: 'app-audit-events',
  standalone: true,
  imports: [AgGridAngular, RouterLink],
  templateUrl: './audit-events.component.html',
  styleUrl: './audit-events.component.css'
})
export class AuditEventsComponent {

  constructor(private router: Router) {}

  rowData = [
    {
      eventId: 'EVT-2025-05-28-001247',
      eventType: 'PRIVILEGED_ACCESS',
      actor: 'john.doe@bank.com',
      sourceSystem: 'Core Banking',
      status: 'HIGH_RISK',
      riskScore: 85,
      confidenceScore: 92,
      toolsUsed: 7,
      createdAt: '2025-05-28 02:31 PM'
    },
    {
      eventId: 'EVT-2025-05-28-001248',
      eventType: 'TOKEN_USAGE',
      actor: 'payment-service',
      sourceSystem: 'Payments API',
      status: 'MEDIUM_RISK',
      riskScore: 61,
      confidenceScore: 87,
      toolsUsed: 5,
      createdAt: '2025-05-28 02:35 PM'
    },
    {
      eventId: 'EVT-2025-05-28-001249',
      eventType: 'DATA_EXPORT',
      actor: 'analyst-team',
      sourceSystem: 'Wealth Systems',
      status: 'LOW_RISK',
      riskScore: 32,
      confidenceScore: 81,
      toolsUsed: 4,
      createdAt: '2025-05-28 02:40 PM'
    }
  ];

  columnDefs: ColDef[] = [
    {
      field: 'eventId',
      headerName: 'Event ID',
      flex: 2
    },
    {
      field: 'eventType',
      headerName: 'Event Type',
      flex: 2
    },
    {
      field: 'actor',
      headerName: 'Actor',
      flex: 2
    },
    {
      field: 'sourceSystem',
      headerName: 'Source System',
      flex: 2
    },
    {
      field: 'status',
      headerName: 'Risk Status',
      flex: 1
    },
    {
      field: 'riskScore',
      headerName: 'Risk',
      flex: 1
    },
    {
      field: 'confidenceScore',
      headerName: 'Confidence',
      flex: 1
    },
    {
      field: 'toolsUsed',
      headerName: 'Tools',
      flex: 1
    },
    {
      field: 'createdAt',
      headerName: 'Created At',
      flex: 2
    }
  ];

  defaultColDef: ColDef = {
    sortable: true,
    filter: true,
    resizable: true
  };

  openAnalysis(event: any): void {
    this.router.navigate(['/analysis', event.data.eventId]);
  }

}
