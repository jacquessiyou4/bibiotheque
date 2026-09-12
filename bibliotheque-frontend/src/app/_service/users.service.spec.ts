import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { UsersService } from './users.service';
import { UserAuthService } from './user-auth.service';

describe('UsersService', () => {
  let service: UsersService;
  let userAuthService: UserAuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(UsersService);
    userAuthService = TestBed.inject(UserAuthService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('roleMatch', () => {
    it('retourne vrai quand un rôle de l utilisateur correspond', () => {
      userAuthService.setRoles([{ roleName: 'ADHERENT' }]);
      expect(service.roleMatch(['ADHERENT', 'BIBLIOTHECAIRE'])).toBeTrue();
    });

    it('retourne faux quand aucun rôle ne correspond', () => {
      userAuthService.setRoles([{ roleName: 'User' }]);
      expect(service.roleMatch(['ADHERENT'])).toBeFalse();
    });

    it('trouve le bon rôle même après un premier rôle sans correspondance', () => {
      // Le bug initial : roleMatch s'arrêtait au premier rôle non correspondant,
      // un compte Admin+BIBLIOTHECAIRE n'aurait jamais été reconnu.
      userAuthService.setRoles([{ roleName: 'Admin' }, { roleName: 'BIBLIOTHECAIRE' }]);
      expect(service.roleMatch(['BIBLIOTHECAIRE'])).toBeTrue();
      expect(service.roleMatch(['ADHERENT'])).toBeFalse();
    });

    it('retourne faux quand aucun rôle n est stocké', () => {
      localStorage.removeItem('roles');
      expect(service.roleMatch(['ADHERENT'])).toBeFalse();
    });
  });
});