import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { AkkaStreamingComponent } from './akka-streaming.component';

describe('AkkaStreamingComponent', () => {
  let component: AkkaStreamingComponent;
  let fixture: ComponentFixture<AkkaStreamingComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ AkkaStreamingComponent ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(AkkaStreamingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
