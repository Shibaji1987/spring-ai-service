import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RiskBadgeComponent } from './risk-badge.component';

describe('RiskBadgeComponent', () => {
  let component: RiskBadgeComponent;
  let fixture: ComponentFixture<RiskBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RiskBadgeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(RiskBadgeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
