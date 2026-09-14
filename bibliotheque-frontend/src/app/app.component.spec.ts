import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AppComponent } from './app.component';
import { HeaderComponent } from './header/header.component';
import { TranslatePipe } from './_i18n/translate.pipe';
import { TranslationService } from './_service/translation.service';
import { ThemeService } from './_service/theme.service';

describe('AppComponent', () => {
  let themeServiceSpy: jasmine.SpyObj<ThemeService>;

  beforeEach(async () => {
    themeServiceSpy = jasmine.createSpyObj('ThemeService', ['init', 'getTheme', 'toggleTheme', 'setTheme']);
    themeServiceSpy.getTheme.and.returnValue('dark');

    await TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        HttpClientTestingModule
      ],
      declarations: [
        AppComponent,
        HeaderComponent,
        TranslatePipe
      ],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key, getLang: () => 'en' } },
        { provide: ThemeService, useValue: themeServiceSpy },
      ],
    }).compileComponents();
  });

  it('devrait créer l\'application', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('devrait avoir le titre \'Library Management System\'', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.title).toEqual('Library Management System');
  });

  it('devrait afficher le header et le router-outlet', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('app-header')).toBeTruthy();
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });

  it('devrait appeler themeService.init() à la construction', () => {
    TestBed.createComponent(AppComponent);
    expect(themeServiceSpy.init).toHaveBeenCalled();
  });
});
