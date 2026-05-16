import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SubmitEventComponent } from './submit-event.component';

describe('SubmitEventComponent', () => {
  let component: SubmitEventComponent;
  let fixture: ComponentFixture<SubmitEventComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubmitEventComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SubmitEventComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
