import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { AuditApiService } from '../../core/services/audit-api.service';
import { AnalysisApiService } from '../../core/services/analysis-api.service';
import { AuditEvent } from '../../core/models/audit-event.model';

@Component({
  selector: 'app-submit-event',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './submit-event.component.html',
  styleUrl: './submit-event.component.css'
})
export class SubmitEventComponent {
  private readonly router = inject(Router);
  private readonly auditApi = inject(AuditApiService);
  private readonly analysisApi = inject(AnalysisApiService);

  readonly isValidJson = signal(true);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  payload = `{
  "eventId": "EVT-2025-05-28-001247",
  "eventType": "PRIVILEGED_ACCESS",
  "actor": "john.doe@bank.com",
  "sourceSystem": "Core Banking",
  "timestamp": "2025-05-28T14:31:15Z",
  "status": "NEW",
  "payload": {
    "action": "LOGIN",
    "resource": "CBS-Enterprise",
    "ipAddress": "10.45.23.187"
  }
}`;

  validateJson(): void {
    try {
      JSON.parse(this.payload);
      this.isValidJson.set(true);
    } catch {
      this.isValidJson.set(false);
    }
  }

  async analyzeEvent(): Promise<void> {
    this.validateJson();
    if (!this.isValidJson()) return;

    this.loading.set(true);
    this.error.set(null);

    try {
      const eventRequest = JSON.parse(this.payload) as AuditEvent;
      const submitted = await firstValueFrom(this.auditApi.submitEvent(eventRequest));
      await firstValueFrom(this.analysisApi.analyzeEvent(submitted.eventId));
      await this.router.navigate(['/analysis', submitted.eventId]);
    } catch {
      this.error.set('Unable to submit/analyze event. Verify backend is running and payload matches API contract.');
    } finally {
      this.loading.set(false);
    }
  }

  clearPayload(): void {
    this.payload = '';
  }
}
