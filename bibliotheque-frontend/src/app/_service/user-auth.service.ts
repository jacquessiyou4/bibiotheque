import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class UserAuthService {

  constructor() { }

  public setRoles(roles: { roleName: string }[]) {
    localStorage.setItem('roles', JSON.stringify(roles));
  }

  public getRoles(): { roleName: string }[] {
    return JSON.parse(localStorage.getItem('roles')!);
  }

  public setToken(jwtToken: string) {
    localStorage.setItem('jwtToken', jwtToken);
  }

  public getToken(): string {
    return localStorage.getItem('jwtToken')!;
  }

  public setUserId(userId: number) {
    localStorage.setItem('userId', JSON.stringify(userId));
  }

  public getUserId() {
    return JSON.parse(localStorage.getItem('userId')!);
  }

  public setName(userId: number) {
    localStorage.setItem('name', JSON.stringify(userId));
  }

  public getName() {
    return JSON.parse(localStorage.getItem('name')!);
  }

  public clear() {
    localStorage.clear();
  }

  /**
   * Décode la partie « payload » d'un JWT (sans en vérifier la signature :
   * la validation est faite par le backend et Keycloak).
   */
  public decodeJwt(token: string): any {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join(''));
      return JSON.parse(jsonPayload);
    } catch (e) {
      return {};
    }
  }

  public isLoggedIn() {
    return this.getRoles() && this.getToken();
  }

}