import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';

import { HeaderComponent } from './header.component';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { TranslationService } from '../../services/translation.service';
import { ThemeService } from '../../services/theme.service';
import { UserAuthService } from '../../services/user-auth.service';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let fixture: ComponentFixture<HeaderComponent>;
  let userAuthService: UserAuthService;
  let router: Router;
  let themeServiceSpy: jasmine.SpyObj<ThemeService>;
  let translationServiceSpy: jasmine.SpyObj<TranslationService>;

  beforeEach(async () => {
    themeServiceSpy = jasmine.createSpyObj('ThemeService', ['toggleTheme', 'getTheme']);
    themeServiceSpy.getTheme.and.returnValue('dark');
    translationServiceSpy = jasmine.createSpyObj('TranslationService', ['toggleLang', 'translate', 'getLang']);
    translationServiceSpy.translate.and.callFake((key: string) => key);
    translationServiceSpy.getLang.and.returnValue('en');

    await TestBed.configureTestingModule({
      imports: [RouterTestingModule, HttpClientTestingModule],
      declarations: [ HeaderComponent, TranslatePipe ],
      providers: [
        { provide: TranslationService, useValue: translationServiceSpy },
        { provide: ThemeService, useValue: themeServiceSpy },
      ]
    })
    .compileComponents();

    sessionStorage.clear();
    userAuthService = TestBed.inject(UserAuthService);
    userAuthService.clear();
    userAuthService.setName('Jean');
    fixture = TestBed.createComponent(HeaderComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  afterEach(() => {
    userAuthService.clear();
    sessionStorage.clear();
  });

  it('devrait être créé', () => {
    expect(component).toBeTruthy();
  });

  it('isLoggedIn reflète la présence d\u2019une session', () => {
    expect(component.isLoggedIn()).toBeFalsy();

    userAuthService.setToken('jeton');
    userAuthService.setRoles([{ roleName: 'ADHERENT' }]);

    expect(component.isLoggedIn()).toBeTruthy();
  });

  it('logout efface la session puis redirige vers /', () => {
    userAuthService.setToken('jeton');
    userAuthService.setRoles([{ roleName: 'ADHERENT' }]);
    spyOn(router, 'navigate');

    component.logout();

    expect(userAuthService.getToken()).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/']);
  });

  it('toggleTheme délègue au ThemeService', () => {
    component.toggleTheme();

    expect(themeServiceSpy.toggleTheme).toHaveBeenCalled();
  });

  it('toggleLang délègue au TranslationService', () => {
    component.toggleLang();

    expect(translationServiceSpy.toggleLang).toHaveBeenCalled();
  });

  it('name reflète le nom stocké dans le UserAuthService', () => {
    expect(component.name).toBe('Jean');
  });
});
