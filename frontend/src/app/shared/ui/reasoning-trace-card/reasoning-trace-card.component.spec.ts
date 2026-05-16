import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ReasoningTraceCardComponent } from './reasoning-trace-card.component';

describe('ReasoningTraceCardComponent', () => {
  let component: ReasoningTraceCardComponent;
  let fixture: ComponentFixture<ReasoningTraceCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReasoningTraceCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ReasoningTraceCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
