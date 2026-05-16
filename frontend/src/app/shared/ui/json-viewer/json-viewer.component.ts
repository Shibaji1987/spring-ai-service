import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-json-viewer',
  standalone: true,
  template: `
    <pre class="max-h-56 overflow-auto rounded-xl border border-slate-800 bg-slate-950 p-3 text-xs text-slate-300">{{ formatted() }}</pre>
  `
})
export class JsonViewerComponent {
  data = input<unknown>(null);

  readonly formatted = computed(() => JSON.stringify(this.data(), null, 2) ?? 'null');
}
