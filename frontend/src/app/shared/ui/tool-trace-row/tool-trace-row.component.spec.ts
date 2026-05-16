import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ToolTraceRowComponent } from './tool-trace-row.component';

describe('ToolTraceRowComponent', () => {
  let component: ToolTraceRowComponent;
  let fixture: ComponentFixture<ToolTraceRowComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolTraceRowComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ToolTraceRowComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
