import { Component, computed, input } from '@angular/core';

export type ToolExecutionStatus = 'SUCCESS' | 'FAILED' | 'RUNNING' | 'FALLBACK_USED' | 'SKIPPED' | 'RETRYING';

@Component({
  selector: 'app-tool-status-badge',
  standalone: true,
  templateUrl: './tool-status-badge.component.html'
})
export class ToolStatusBadgeComponent {
  status = input<ToolExecutionStatus>('RUNNING');

  readonly className = computed(() => {
    const base = 'rounded-full border px-2.5 py-1 text-[11px] font-semibold tracking-wide';
    switch (this.status()) {
      case 'SUCCESS':
        return `${base} border-emerald-500/30 bg-emerald-500/10 text-emerald-300`;
      case 'FAILED':
        return `${base} border-red-500/30 bg-red-500/10 text-red-300`;
      case 'FALLBACK_USED':
        return `${base} border-amber-500/30 bg-amber-500/10 text-amber-300`;
      case 'RETRYING':
      case 'RUNNING':
        return `${base} border-cyan-500/30 bg-cyan-500/10 text-cyan-300`;
      default:
        return `${base} border-slate-500/30 bg-slate-500/10 text-slate-300`;
    }
  });
}
