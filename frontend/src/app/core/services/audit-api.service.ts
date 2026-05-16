import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AuditEvent } from '../models/audit-event.model';

@Injectable({ providedIn: 'root' })
export class AuditApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/audit';

  getEvents(): Observable<AuditEvent[]> {
    return this.http.get<AuditEvent[]>(`${this.baseUrl}/events`);
  }

  getEventById(eventId: string): Observable<AuditEvent> {
    return this.http.get<AuditEvent>(`${this.baseUrl}/events/${eventId}`);
  }

  submitEvent(payload: AuditEvent): Observable<AuditEvent> {
    return this.http.post<AuditEvent>(`${this.baseUrl}/event`, payload);
  }
}
