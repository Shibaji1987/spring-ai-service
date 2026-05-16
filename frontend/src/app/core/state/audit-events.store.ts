import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuditEvent } from '../models/audit-event.model';
import { AuditApiService } from '../services/audit-api.service';

@Injectable({ providedIn: 'root' })
export class AuditEventsStore {
  private readonly auditApi = inject(AuditApiService);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly events = signal<AuditEvent[]>([]);
  readonly selectedEvent = signal<AuditEvent | null>(null);
  readonly filters = signal<Record<string, string>>({});
  readonly pagination = signal({ page: 1, pageSize: 20, total: 0 });

  async loadEvents(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const events = await firstValueFrom(this.auditApi.getEvents());
      this.events.set(events);
      this.pagination.update((value) => ({ ...value, total: events.length }));
    } catch {
      this.error.set('Failed to load audit events.');
    } finally {
      this.loading.set(false);
    }
  }
}
