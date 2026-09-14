import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { Observable, throwError } from 'rxjs';
import { UserAuthService } from '../_service/user-auth.service';
import { NotificationService } from '../_service/notification.service';
import { Injectable } from '@angular/core';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(
    private userAuthService: UserAuthService,
    private router:Router,
    private notificationService: NotificationService
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const noAuth = req.headers.get('No-Auth') === 'True';
    let headers: Record<string, string> = {};

    if (!noAuth) {
      const token = this.userAuthService.getToken();
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      // Ajouter un X-Request-ID unique pour corréler les logs frontend → backend
      // (pas sur les appels No-Auth vers Keycloak, qui ne l'autorise pas en CORS).
      headers['X-Request-ID'] = this.generateRequestId();
    }

    // No-Auth n'est qu'un marqueur interne : il ne doit pas partir sur le
    // réseau, un en-tête inconnu fait échouer la pré-requête CORS.
    const base = noAuth ? req.clone({ headers: req.headers.delete('No-Auth') }) : req;
    const cloned = base.clone({ setHeaders: headers });

    return next.handle(cloned).pipe(
        catchError(
            (err:HttpErrorResponse) => {
                if(err.status === 401) {
                    // Jeton expiré ou refusé : la session locale n'est plus valable.
                    // On prévient l'utilisateur (message du backend, ex. « Votre session
                    // a expiré ») seulement s'il était connecté : un mauvais mot de
                    // passe au login (requête No-Auth) a son propre message.
                    const sessionPerdue = !noAuth && !!this.userAuthService.getToken();
                    this.userAuthService.clear();
                    if (sessionPerdue) {
                        this.notificationService.showWarning(
                            err.error?.message || 'Votre session a expiré. Veuillez vous reconnecter.');
                    }
                    this.router.navigate(['/login']);
                } else if(err.status === 403) {
                    this.router.navigate(['/forbidden']);
                }
                return throwError(() => err);
            }
        )
    );
  }

  private generateRequestId(): string {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
      const r = (Math.random() * 16) | 0;
      const v = c === 'x' ? r : (r & 0x3) | 0x8;
      return v.toString(16);
    });
  }
}
