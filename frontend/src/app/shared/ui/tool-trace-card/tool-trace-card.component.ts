import { DecimalPipe } from '@angular/common';
import { Component, input, signal } from '@angular/core';
import { ToolExecution } from '../../../core/models/tool-execution.model';
import { JsonViewerComponent } from '../json-viewer/json-viewer.component';
import { ToolStatusBadgeComponent } from '../tool-status-badge/tool-status-badge.component';

@Component({
  selector: 'app-tool-trace-card',
  standalone: true,
  imports: [JsonViewerComponent, ToolStatusBadgeComponent, DecimalPipe],
  templateUrl: './tool-trace-card.component.html'
})
export class ToolTraceCardComponent {
  execution = input.required<ToolExecution>();
  index = input(0);
  readonly expanded = signal(false);

  toggleExpanded(): void {
    this.expanded.update((value) => !value);
  }
}
