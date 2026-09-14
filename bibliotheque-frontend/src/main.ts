import { enableProdMode } from '@angular/core';
import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';

import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

if (environment.production) {
  enableProdMode();
}

platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => {
    // Pas de console en production : un message visible plutôt qu'une page blanche.
    if (!environment.production) {
      console.error(err);
    }
    const message = document.createElement('p');
    message.textContent = 'L\'application n\'a pas pu démarrer. Rechargez la page.';
    document.body.appendChild(message);
  });
