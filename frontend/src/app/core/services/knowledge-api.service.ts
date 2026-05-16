import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class KnowledgeApiService {
  private readonly http = inject(HttpClient);

  searchKnowledge(query: string, topK = 3): Observable<unknown> {
    const params = new HttpParams().set('query', query).set('topK', topK);
    return this.http.get('/knowledge/search', { params });
  }
}
