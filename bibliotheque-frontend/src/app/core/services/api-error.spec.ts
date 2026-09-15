import { HttpErrorResponse } from '@angular/common/http';
import { messageErreur } from './api-error';

describe('messageErreur', () => {
  it('serveur injoignable (statut 0) : message dédié', () => {
    expect(messageErreur(new HttpErrorResponse({ status: 0 }), 'repli'))
      .toContain('injoignable');
  });

  it('corps problem+json : detail suivi de la référence de requête', () => {
    const err = new HttpErrorResponse({
      status: 400,
      error: { detail: 'Le livre "L2" n\'est plus disponible.', code: 'BOOK_UNAVAILABLE',
        requestId: '3f2a9c1e-0000-4000-8000-000000000000' }
    });

    expect(messageErreur(err, 'repli')).toBe('Le livre "L2" n\'est plus disponible. (réf. 3f2a9c1e)');
  });

  it('ancien corps avec « message » seulement : message sans référence', () => {
    const err = new HttpErrorResponse({ status: 409, error: { message: 'RG-02' } });

    expect(messageErreur(err, 'repli')).toBe('RG-02');
  });

  it('corps texte : renvoyé tel quel', () => {
    expect(messageErreur(new HttpErrorResponse({ status: 500, error: 'Panne' }), 'repli')).toBe('Panne');
  });

  it('objet sans texte ou erreur sans corps : message de repli', () => {
    expect(messageErreur(new HttpErrorResponse({ status: 500, error: {} }), 'repli')).toBe('repli');
    expect(messageErreur(new Error('JS'), 'repli')).toBe('repli');
    expect(messageErreur(null, 'repli')).toBe('repli');
  });
});
