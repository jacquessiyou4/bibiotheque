import { ErrorHandler, NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { HttpClientModule, HTTP_INTERCEPTORS } from '@angular/common/http';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { SharedModule } from './shared/shared.module';
import { HeaderComponent } from './core/layout/header/header.component';
import { HomeComponent } from './core/layout/home/home.component';
import { ForbiddenComponent } from './core/layout/forbidden/forbidden.component';
import { AuthGuard } from './core/auth/auth.guard';
import { AuthInterceptor } from './core/auth/auth.interceptor';
import { AppErrorHandler } from './core/services/error-handler.service';

/**
 * Module racine : coquille de l'application (en-tête, accueil, accès refusé)
 * et services transverses. Les écrans métier vivent dans les modules de
 * fonctionnalité chargés à la demande (voir app-routing.module.ts).
 */
@NgModule({
  declarations: [
    AppComponent,
    HeaderComponent,
    HomeComponent,
    ForbiddenComponent,
  ],
  imports: [
    BrowserModule,
    HttpClientModule,
    SharedModule,
    AppRoutingModule,
  ],
  providers: [
    { provide: ErrorHandler, useClass: AppErrorHandler },
    AuthGuard,
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
