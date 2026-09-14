import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { CreateUserRequest, UserListItem, Users } from '../_model/users';
import { DEFAULT_PAGE_SIZE, LIST_PAGE_SIZE, Page } from '../_model/page';
import { Profile, TokenResponse } from '../_model/auth';
import { UserAuthService } from './user-auth.service';
import { apiUrl } from './api-config';

/**
 * Le backend renvoie des UserResponse (rôles = liste de noms, jamais de mot
 * de passe) ; les composants manipulent le modèle Users (rôles = [{ roleName }]).
 */
function toUsers(user: UserListItem): Users {
  return {
    userId: user.userId,
    username: user.username,
    name: user.name,
    role: (user.roles || []).map(roleName => ({ roleName }))
  };
}

@Injectable({
  providedIn: 'root'
})
export class UsersService {

  private baseURL = `${apiUrl()}/admin/users`;
  requestHeader = new HttpHeaders(
    { 'No-Auth': 'True' }
  );

  constructor(
    private httpClient: HttpClient,
    private userAuthService: UserAuthService
  ) { }

  /**
   * Connexion via le backend (POST /auth/token), qui obtient les jetons
   * auprès de Keycloak : le navigateur ne contacte plus Keycloak.
   * L'accessToken est ensuite présenté au backend dans l'en-tête
   * Authorization (voir AuthInterceptor). No-Auth : un ancien jeton expiré
   * ne doit pas être envoyé, le backend le refuserait avant la connexion.
   */
  public login(loginData: NgForm): Observable<TokenResponse> {
    return this.httpClient.post<TokenResponse>(
      `${apiUrl()}/auth/token`,
      { username: loginData.value.username, password: loginData.value.password },
      { headers: new HttpHeaders({ 'No-Auth': 'True' }) }
    );
  }

  /**
   * Renvoie l'utilisateur LOCAL de l'application associé au jeton courant
   * (utilisé notamment pour récupérer le userId des emprunts).
   */
  public getProfile(): Observable<Profile> {
    return this.httpClient.get<Profile>(`${apiUrl()}/profile`);
  }

  public roleMatch(allowedRoles: string[]): boolean {
    const userRoles = this.userAuthService.getRoles();

    if (userRoles != null && userRoles) {
      for (let i = 0; i < userRoles.length; i++) {
        for (let j = 0; j < allowedRoles.length; j++) {
          if (userRoles[i].roleName === allowedRoles[j]) {
            return true;
          }
        }
      }
    }

    return false;
  }

  // Liste complète, pour les écrans qui recoupent les utilisateurs (emprunts,
  // réservations). GET /admin/users est paginé côté backend (Page<UserResponse>).
  getUsersList(): Observable<Users[]> {
    return this.httpClient.get<Page<UserListItem>>(this.baseURL, { params: { size: LIST_PAGE_SIZE } })
      .pipe(map(page => page.content.map(toUsers)));
  }

  // Tableau paginé de la liste des utilisateurs : une seule page à la fois.
  getUsersPage(page: number, size: number = DEFAULT_PAGE_SIZE): Observable<Page<Users>> {
    return this.httpClient.get<Page<UserListItem>>(this.baseURL, { params: { page, size } })
      .pipe(map(resultat => ({ ...resultat, content: resultat.content.map(toUsers) })));
  }

  createUser(user: CreateUserRequest): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}`, user);
  }

  getUserById(userId: number): Observable<Users> {
    return this.httpClient.get<UserListItem>(`${this.baseURL}/${userId}`).pipe(map(toUsers));
  }

  /**
   * Le backend attend un UserCreateRequest : rôles en liste de noms, et mot
   * de passe absent (pas vide, sinon @Size le rejette) pour le conserver. Un
   * nouveau mot de passe se passe à part : il ne fait pas partie du modèle Users.
   */
  updateUser(userId: number, user: Users, newPassword?: string): Observable<Object> {
    const body: Partial<CreateUserRequest> = {
      username: user.username,
      name: user.name,
      roles: (user.role || []).map(r => r.roleName)
    };
    if (newPassword) {
      body.password = newPassword;
    }
    return this.httpClient.put(`${this.baseURL}/${userId}`, body);
  }

}
