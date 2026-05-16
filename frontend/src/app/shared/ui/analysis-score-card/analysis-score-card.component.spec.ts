import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AnalysisScoreCardComponent } from './analysis-score-card.component';

describe('AnalysisScoreCardComponent', () => {
  let component: AnalysisScoreCardComponent;
  let fixture: ComponentFixture<AnalysisScoreCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AnalysisScoreCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AnalysisScoreCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
