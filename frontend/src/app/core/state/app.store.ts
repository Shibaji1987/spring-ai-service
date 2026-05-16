import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AppStore {
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly search = signal('');

  setLoading(loading: boolean): void {
    this.loading.set(loading);
  }

  setError(error: string | null): void {
    this.error.set(error);
  }

  setSearch(search: string): void {
    this.search.set(search);
  }
}
