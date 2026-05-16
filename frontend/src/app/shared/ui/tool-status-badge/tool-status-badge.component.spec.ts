import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ToolStatusBadgeComponent } from './tool-status-badge.component';

describe('ToolStatusBadgeComponent', () => {
  let component: ToolStatusBadgeComponent;
  let fixture: ComponentFixture<ToolStatusBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolStatusBadgeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ToolStatusBadgeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
