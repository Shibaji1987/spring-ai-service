import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PolicyEvidenceCardComponent } from './policy-evidence-card.component';

describe('PolicyEvidenceCardComponent', () => {
  let component: PolicyEvidenceCardComponent;
  let fixture: ComponentFixture<PolicyEvidenceCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PolicyEvidenceCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PolicyEvidenceCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
