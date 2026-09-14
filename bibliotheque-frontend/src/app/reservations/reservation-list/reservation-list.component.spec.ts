import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { ReservationListComponent } from './reservation-list.component';
import { TranslatePipe } from '../../_i18n/translate.pipe';
import { TranslationService } from '../../_service/translation.service';
import { Reservation } from '../../_model/reservation';

describe('ReservationListComponent', () => {
  let component: ReservationListComponent;
  let fixture: ComponentFixture<ReservationListComponent>;

  const reservation = (id: number, statut: string, dateExpiration: string) =>
    ({ id, livreNom: 'Livre ' + id, adherentNom: 'A1',
       dateExpiration, statut } as Reservation);

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CommonModule],
      declarations: [ReservationListComponent, TranslatePipe],
      providers: [
        { provide: TranslationService, useValue: { translate: (key: string) => key } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReservationListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('tri et pagination', () => {
    it('trie les réservations par date d expiration croissante', () => {
      component.reservations = [
        reservation(1, 'EN_ATTENTE', '2026-09-20T10:00:00'),
        reservation(2, 'EN_ATTENTE', '2026-09-12T10:00:00'),
        reservation(3, 'EN_ATTENTE', '2026-09-15T10:00:00'),
      ];
      expect(component.reservationsTriees.map((r) => r.id)).toEqual([2, 3, 1]);
    });

    it('inverse le tri sur bascule', () => {
      component.reservations = [
        reservation(1, 'EN_ATTENTE', '2026-09-20T10:00:00'),
        reservation(2, 'EN_ATTENTE', '2026-09-12T10:00:00'),
      ];
      component.toggleTri();
      expect(component.reservationsTriees.map((r) => r.id)).toEqual([1, 2]);
    });

    it('page sur la première page quand la liste change', () => {
      component.reservations = [reservation(1, 'EN_ATTENTE', '2026-09-20T10:00:00')];
      component.page = 3;
      component.ngOnChanges({ reservations: {} } as any);
      expect(component.page).toBe(1);
    });

    it('calcule le nombre de pages', () => {
      component.reservations = Array.from({ length: 25 }, (_, i) =>
        reservation(i + 1, 'EN_ATTENTE', '2026-09-20T10:00:00'));
      expect(component.totalPages).toBe(3);
      expect(component.reservationsPage.length).toBe(10);
      component.pageSuivante();
      expect(component.page).toBe(2);
    });

    it('ne dépasse pas les bornes de pagination', () => {
      component.reservations = Array.from({ length: 12 }, (_, i) =>
        reservation(i + 1, 'EN_ATTENTE', '2026-09-20T10:00:00'));
      component.pagePrecedente();
      expect(component.page).toBe(1);
      component.page = 2;
      component.pageSuivante();
      expect(component.page).toBe(2);
    });
  });

  describe('peutAnnuler et badge', () => {
    it('autorise l annulation seulement pour EN_ATTENTE et DISPONIBLE', () => {
      expect(component.peutAnnuler(reservation(1, 'EN_ATTENTE', 'x'))).toBeTrue();
      expect(component.peutAnnuler(reservation(2, 'DISPONIBLE', 'x'))).toBeTrue();
      expect(component.peutAnnuler(reservation(3, 'ANNULEE', 'x'))).toBeFalse();
      expect(component.peutAnnuler(reservation(4, 'EXPIREE', 'x'))).toBeFalse();
      expect(component.peutAnnuler(reservation(5, 'HONOREE', 'x'))).toBeFalse();
    });

    it('renvoie la classe de badge correspondant au statut', () => {
      expect(component.badgeClasse('EN_ATTENTE')).toContain('bg-warning');
      expect(component.badgeClasse('DISPONIBLE')).toContain('bg-success');
      expect(component.badgeClasse('ANNULEE')).toContain('bg-secondary');
      expect(component.badgeClasse('EXPIREE')).toContain('bg-dark');
      expect(component.badgeClasse('HONOREE')).toContain('bg-primary');
      expect(component.badgeClasse('INCONNU')).toContain('bg-secondary');
    });
  });

  describe('onSupprimer', () => {
    it('émet l id quand isBibliothecaire est true', () => {
      component.isBibliothecaire = true;
      let emitted: number | undefined;
      component.delete.subscribe((id) => emitted = id);

      component.onSupprimer(reservation(12, 'EXPIREE', 'x'));

      expect(emitted).toBe(12);
    });
  });

  describe('onAnnuler', () => {
    it('émet l id après confirmation', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      let emitted: number | undefined;
      component.cancel.subscribe((id) => emitted = id);

      component.onAnnuler(reservation(7, 'EN_ATTENTE', 'x'));

      expect(emitted).toBe(7);
    });

    it('n émet rien si l utilisateur décline la confirmation', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      let emitted = false;
      component.cancel.subscribe(() => emitted = true);

      component.onAnnuler(reservation(7, 'EN_ATTENTE', 'x'));

      expect(emitted).toBeFalse();
    });
  });

  describe('TEMPLATE', () => {
    it('affiche une ligne par réservation de la page courante', () => {
      component.reservations = [
        reservation(1, 'EN_ATTENTE', '2026-09-20T10:00:00'),
        reservation(2, 'ANNULEE', '2026-09-19T10:00:00'),
      ];
      fixture.detectChanges();

      const lignes = fixture.nativeElement.querySelectorAll('tbody tr');
      expect(lignes.length).toBe(2);
    });

    it('affiche le bouton annuler uniquement pour les statuts annulables', () => {
      component.reservations = [
        reservation(1, 'EN_ATTENTE', '2026-09-20T10:00:00'),
        reservation(2, 'ANNULEE', '2026-09-19T10:00:00'),
      ];
      fixture.detectChanges();

      const boutons = fixture.nativeElement.querySelectorAll('button');
      expect(boutons.length).toBe(1);
    });

    it('affiche le bouton supprimer quand isBibliothecaire est true', () => {
      component.isBibliothecaire = true;
      component.reservations = [
        reservation(1, 'EXPIREE', '2026-09-10T10:00:00'),
      ];
      fixture.detectChanges();

      const boutons = fixture.nativeElement.querySelectorAll('button');
      expect(boutons.length).toBe(1);
    });

    it('n affiche pas le bouton supprimer pour un adherent', () => {
      component.isBibliothecaire = false;
      component.reservations = [
        reservation(1, 'EN_ATTENTE', '2026-09-20T10:00:00'),
      ];
      fixture.detectChanges();

      const boutons = fixture.nativeElement.querySelectorAll('button');
      expect(boutons.length).toBe(1);
    });

    it('affiche la pagination quand il y a plusieurs pages', () => {
      component.reservations = Array.from({ length: 15 }, (_, i) =>
        reservation(i + 1, 'EN_ATTENTE', '2026-09-20T10:00:00'));
      fixture.detectChanges();

      const nav = fixture.nativeElement.querySelector('nav[aria-label]');
      expect(nav).toBeTruthy();
    });
  });
});