import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ToolTraceCardComponent } from './tool-trace-card.component';

describe('ToolTraceCardComponent', () => {
  let component: ToolTraceCardComponent;
  let fixture: ComponentFixture<ToolTraceCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolTraceCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ToolTraceCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
