import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AnalysisResult } from '../models/analysis-result.model';

@Injectable({ providedIn: 'root' })
export class AnalysisApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/audit';

  analyzeEvent(eventId: string): Observable<AnalysisResult> {
    return this.http.post<AnalysisResult>(`${this.baseUrl}/analyze/${eventId}`, {});
  }

  getFullAnalysis(eventId: string): Observable<AnalysisResult> {
    return this.http.get<AnalysisResult>(`${this.baseUrl}/full/${eventId}`);
  }
}
