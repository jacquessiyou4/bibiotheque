import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { CreateUserRequest, UserListItem, Users } from '../_model/users';
import { LIST_PAGE_SIZE, Page } from '../_model/page';
import { UserAuthService } from './user-auth.service';
import { apiUrl, keycloakClient, keycloakRealm, keycloakUrl } from './api-config';

/**
 * Le backend renvoie des UserResponse (rôles = liste de noms) ; les
 * composants manipulent le modèle Users (rôles = [{ roleName }]).
 */
function toUsers(user: UserListItem): Users {
  return {
    userId: user.userId,
    username: user.username,
    name: user.name,
    password: '',
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
   * Authentification déléguée à Keycloak (Direct Access Grant, password flow).
   * Keycloak répond un access_token JWT qui sera présenté au backend dans
   * l'en-tête Authorization (voir AuthInterceptor).
   */
  public login(loginData: NgForm) {
    const body = new HttpParams()
      .set('grant_type', 'password')
      .set('client_id', keycloakClient())
      .set('username', loginData.value.username)
      .set('password', loginData.value.password);

    return this.httpClient.post(
      `${keycloakUrl()}/realms/${keycloakRealm()}/protocol/openid-connect/token`,
      body,
      {
        headers: new HttpHeaders({
          'Content-Type': 'application/x-www-form-urlencoded',
          'No-Auth': 'True'
        })
      }
    );
  }

  /**
   * Renvoie l'utilisateur LOCAL de l'application associé au jeton Keycloak
   * courant (utilisé notamment pour récupérer le userId des emprunts).
   */
  public getMe() {
    return this.httpClient.get(`${apiUrl()}/me`);
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

  // GET /admin/users est paginé côté backend (Page<UserResponse>).
  getUsersList(): Observable<Users[]> {
    return this.httpClient.get<Page<UserListItem>>(this.baseURL, { params: { size: LIST_PAGE_SIZE } })
      .pipe(map(page => page.content.map(toUsers)));
  }

  createUser(user: CreateUserRequest | Users): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}`, user);
  }

  getUserById(userId: number): Observable<Users> {
    return this.httpClient.get<UserListItem>(`${this.baseURL}/${userId}`).pipe(map(toUsers));
  }

  updateUser(userId: number, user: Users): Observable<Object> {
    // Le backend attend un UserCreateRequest : rôles en liste de noms, et
    // mot de passe absent (pas vide, sinon @Size le rejette) pour le conserver.
    const body: Partial<CreateUserRequest> = {
      username: user.username,
      name: user.name,
      roles: (user.role || []).map(r => r.roleName)
    };
    if (user.password) {
      body.password = user.password;
    }
    return this.httpClient.put(`${this.baseURL}/${userId}`, body);
  }

}
