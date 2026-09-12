import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ForbiddenComponent } from './forbidden.component';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { TranslationService } from '../_service/translation.service';

describe('ForbiddenComponent', () => {
  let component: ForbiddenComponent;
  let fixture: ComponentFixture<ForbiddenComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ForbiddenComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ForbiddenComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});