import { Routes } from '@angular/router';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  {
    path: '',
    component: ShellComponent,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
      },
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component')
            .then(m => m.DashboardComponent)
      },
      {
        path: 'audit-events',
        loadComponent: () =>
          import('./features/audit-events/audit-events.component')
            .then(m => m.AuditEventsComponent)
      },
      {
        path: 'submit-event',
        loadComponent: () =>
          import('./features/submit-event/submit-event.component')
            .then(m => m.SubmitEventComponent)
      },
      {
        path: 'analysis',
        loadComponent: () =>
          import('./features/analysis-details/analysis-details.component')
            .then(m => m.AnalysisDetailsComponent)
      },
      {
        path: 'analysis/:eventId',
        loadComponent: () =>
          import('./features/analysis-details/analysis-details.component')
            .then(m => m.AnalysisDetailsComponent)
      }
    ]
  }
];
