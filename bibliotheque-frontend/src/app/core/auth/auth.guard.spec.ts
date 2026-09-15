import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { AuthGuard } from './auth.guard';
import { UserAuthService } from '../services/user-auth.service';
import { UsersService } from '../api/users.service';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let router: jasmine.SpyObj<Router>;
  let userAuthService: jasmine.SpyObj<UserAuthService>;
  let usersService: jasmine.SpyObj<UsersService>;
  let route: ActivatedRouteSnapshot;
  let state: RouterStateSnapshot;

  beforeEach(() => {
    router = jasmine.createSpyObj('Router', ['navigate']);
    userAuthService = jasmine.createSpyObj('UserAuthService', ['getToken']);
    usersService = jasmine.createSpyObj('UsersService', ['roleMatch']);

    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: Router, useValue: router },
        { provide: UserAuthService, useValue: userAuthService },
        { provide: UsersService, useValue: usersService },
      ],
    });

    guard = TestBed.inject(AuthGuard);
    route = { data: { roles: ['ADHERENT', 'BIBLIOTHECAIRE'] } } as unknown as ActivatedRouteSnapshot;
    state = {} as RouterStateSnapshot;
  });

  it('sans token, refuse l accès et redirige vers /login', () => {
    userAuthService.getToken.and.returnValue(null as any);

    expect(guard.canActivate(route, state)).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('avec un token sans rôle autorisé, refuse et redirige vers /forbidden', () => {
    userAuthService.getToken.and.returnValue('token');
    usersService.roleMatch.and.returnValue(false);

    expect(guard.canActivate(route, state)).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/forbidden']);
  });

  it('autorise un ADHERENT sur la route /reservations', () => {
    userAuthService.getToken.and.returnValue('token');
    usersService.roleMatch.and.returnValue(true);

    expect(guard.canActivate(route, state)).toBeTrue();
  });

  it('autorise un BIBLIOTHECAIRE sur la route /reservations', () => {
    userAuthService.getToken.and.returnValue('token');
    usersService.roleMatch.and.returnValue(true);

    expect(guard.canActivate(route, state)).toBeTrue();
  });
});