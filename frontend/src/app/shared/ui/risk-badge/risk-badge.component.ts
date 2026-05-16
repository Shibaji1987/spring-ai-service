import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-risk-badge',
  standalone: true,
  templateUrl: './risk-badge.component.html'
})
export class RiskBadgeComponent {
  score = input<number>(0);

  readonly level = computed(() => {
    const score = this.score();
    if (score >= 75) return 'HIGH RISK';
    if (score >= 45) return 'MEDIUM RISK';
    return 'LOW RISK';
  });

  readonly className = computed(() => {
    const base = 'rounded-full border px-3 py-1 text-xs font-semibold';
    if (this.score() >= 75) return `${base} border-red-500/30 bg-red-500/10 text-red-300`;
    if (this.score() >= 45) return `${base} border-amber-500/30 bg-amber-500/10 text-amber-300`;
    return `${base} border-emerald-500/30 bg-emerald-500/10 text-emerald-300`;
  });
}
