import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class UserAuthService {

  constructor() { }

  public setRoles(roles: { roleName: string }[]) {
    localStorage.setItem('roles', JSON.stringify(roles));
  }

  public getRoles(): { roleName: string }[] | null {
    const raw = localStorage.getItem('roles');
    return raw ? JSON.parse(raw) : null;
  }

  public setToken(jwtToken: string) {
    localStorage.setItem('jwtToken', jwtToken);
  }

  public getToken(): string | null {
    return localStorage.getItem('jwtToken');
  }

  public setUserId(userId: number) {
    localStorage.setItem('userId', JSON.stringify(userId));
  }

  public getUserId(): number | null {
    const raw = localStorage.getItem('userId');
    return raw ? JSON.parse(raw) : null;
  }

  public setName(name: string) {
    localStorage.setItem('name', JSON.stringify(name));
  }

  public getName(): string | null {
    const raw = localStorage.getItem('name');
    return raw ? JSON.parse(raw) : null;
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