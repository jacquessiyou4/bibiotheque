import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { NgForm } from '@angular/forms';
import { Observable } from 'rxjs';
import { Users } from '../_model/users';
import { UserAuthService } from './user-auth.service';
import { apiUrl, keycloakClient, keycloakRealm, keycloakUrl } from './api-config';

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

  public roleMatch(allowedRoles: any): boolean {
    const userRoles: any = this.userAuthService.getRoles();

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

  getUsersList(): Observable<Users[]> {
    return this.httpClient.get<Users[]>(`${this.baseURL}`);
  }

  createUser(user: Users): Observable<Object> {
    return this.httpClient.post(`${this.baseURL}`, user);
  }

  getUserById(userId: number): Observable<Users> {
    return this.httpClient.get<Users>(`${this.baseURL}/${userId}`);
  }

  updateUser(userId: number, user: Users): Observable<Object> {
    return this.httpClient.put(`${this.baseURL}/${userId}`, user);
  }

}
