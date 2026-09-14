import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NgForm } from '@angular/forms';
import { UsersService } from './users.service';
import { UserAuthService } from './user-auth.service';
import { Users } from '../_model/users';
import { Page } from '../_model/page';
import { Profile, TokenResponse } from '../_model/auth';

describe('UsersService', () => {
  const API = 'http://localhost:8080';

  let service: UsersService;
  let userAuthService: UserAuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(UsersService);
    userAuthService = TestBed.inject(UserAuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('devrait être créé', () => {
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

  describe('login (POST /auth/token)', () => {
    const formulaire = { value: { username: 'A1', password: 'A1123' } } as NgForm;

    it('envoie identifiant et mot de passe en JSON au backend et renvoie les jetons', () => {
      let reponse: TokenResponse | undefined;
      service.login(formulaire).subscribe(r => reponse = r);

      const req = httpMock.expectOne(`${API}/auth/token`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'A1', password: 'A1123' });
      req.flush({
        accessToken: 'jeton-acces', refreshToken: 'jeton-refresh', tokenType: 'Bearer',
        expiresIn: 1800, refreshExpiresIn: 1800, scope: 'profile email'
      });

      expect(reponse!.accessToken).toBe('jeton-acces');
      expect(reponse!.refreshToken).toBe('jeton-refresh');
    });

    it('ne contacte plus Keycloak directement', () => {
      service.login(formulaire).subscribe();

      httpMock.expectNone(req => req.url.includes('/protocol/openid-connect/token'));
      httpMock.expectOne(`${API}/auth/token`).flush({});
    });

    it('marque la requête No-Auth : aucun ancien jeton n’est envoyé à la connexion', () => {
      service.login(formulaire).subscribe();

      const req = httpMock.expectOne(`${API}/auth/token`);
      expect(req.request.headers.get('No-Auth')).toBe('True');
      req.flush({});
    });
  });

  describe('API utilisateurs', () => {
    it('getProfile appelle GET /profile pour l’utilisateur local du jeton', () => {
      let profil: Profile | undefined;
      service.getProfile().subscribe(r => profil = r);

      const req = httpMock.expectOne(`${API}/profile`);
      expect(req.request.method).toBe('GET');
      req.flush({ userId: 2, username: 'a1', name: 'Adhérent Un', email: 'a1@bibliotheque.local', roles: ['ADHERENT', 'User'] });

      expect(profil!.userId).toBe(2);
      expect(profil!.name).toBe('Adhérent Un');
    });

    it('getUsersList demande une page complète et convertit les rôles en objets, sans mot de passe', () => {
      let utilisateurs: Users[] = [];
      service.getUsersList().subscribe(u => utilisateurs = u);

      const req = httpMock.expectOne(`${API}/admin/users?size=1000`);
      expect(req.request.method).toBe('GET');
      req.flush({
        content: [{ userId: 2, username: 'a1', name: 'Adhérent Un', roles: ['User', 'ADHERENT'] }],
        totalElements: 1, totalPages: 1, number: 0, size: 1000
      });

      expect(utilisateurs.length).toBe(1);
      expect(utilisateurs[0].username).toBe('a1');
      expect(utilisateurs[0].role).toEqual([{ roleName: 'User' }, { roleName: 'ADHERENT' }]);
      expect('password' in utilisateurs[0]).toBeFalse();
    });

    it('getUsersList tolère un utilisateur sans rôles', () => {
      let utilisateurs: Users[] = [];
      service.getUsersList().subscribe(u => utilisateurs = u);

      httpMock.expectOne(`${API}/admin/users?size=1000`).flush({
        content: [{ userId: 5, username: 'sans-role', name: 'Sans Rôle', roles: null }],
        totalElements: 1, totalPages: 1, number: 0, size: 1000
      });

      expect(utilisateurs[0].role).toEqual([]);
    });

    it('getUsersPage demande une seule page et convertit son contenu', () => {
      let resultat: Page<Users> | undefined;
      service.getUsersPage(2).subscribe(p => resultat = p);

      httpMock.expectOne(`${API}/admin/users?page=2&size=10`).flush({
        content: [{ userId: 21, username: 'u21', name: 'U21', roles: ['User'] }],
        totalElements: 21, totalPages: 3, number: 2, size: 10
      });

      expect(resultat!.number).toBe(2);
      expect(resultat!.totalPages).toBe(3);
      expect(resultat!.content[0].role).toEqual([{ roleName: 'User' }]);
    });

    it('getUserById appelle GET /admin/users/{id} et convertit la réponse', () => {
      let utilisateur: Users | undefined;
      service.getUserById(3).subscribe(u => utilisateur = u);

      const req = httpMock.expectOne(`${API}/admin/users/3`);
      expect(req.request.method).toBe('GET');
      req.flush({ userId: 3, username: 'a2', name: 'Adhérent Deux', roles: ['ADHERENT'] });

      expect(utilisateur!.name).toBe('Adhérent Deux');
      expect(utilisateur!.role).toEqual([{ roleName: 'ADHERENT' }]);
    });

    it('createUser envoie POST /admin/users avec le formulaire saisi', () => {
      const nouveau = { username: 'nouveau', name: 'Nouveau', password: 'secret1', roles: ['User'] };
      service.createUser(nouveau).subscribe();

      const req = httpMock.expectOne(`${API}/admin/users`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(nouveau);
      req.flush({});
    });

    it('updateUser sans nouveau mot de passe n’envoie pas le champ password', () => {
      const utilisateur: Users = {
        userId: 3, username: 'a2', name: 'Nom Modifié', role: [{ roleName: 'Admin' }]
      };
      service.updateUser(3, utilisateur).subscribe();

      const req = httpMock.expectOne(`${API}/admin/users/3`);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual({ username: 'a2', name: 'Nom Modifié', roles: ['Admin'] });
      expect('password' in req.request.body).toBeFalse();
      req.flush({});
    });

    it('updateUser avec un nouveau mot de passe l’envoie au backend', () => {
      const utilisateur: Users = { userId: 3, username: 'a2', name: 'A2', role: [] };
      service.updateUser(3, utilisateur, 'nouveau-secret').subscribe();

      const req = httpMock.expectOne(`${API}/admin/users/3`);
      expect(req.request.body.password).toBe('nouveau-secret');
      expect(req.request.body.roles).toEqual([]);
      req.flush({});
    });
  });
});
