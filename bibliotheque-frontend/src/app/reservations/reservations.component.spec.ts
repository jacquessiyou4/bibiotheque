import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { ReservationsComponent } from './reservations.component';
import { ReservationFormComponent } from './reservation-form/reservation-form.component';
import { ReservationListComponent } from './reservation-list/reservation-list.component';
import { ReservationService } from '../_service/reservation.service';
import { BooksService } from '../_service/books.service';
import { UsersService } from '../_service/users.service';
import { UserAuthService } from '../_service/user-auth.service';
import { TranslationService } from '../_service/translation.service';
import { TranslatePipe } from '../_i18n/translate.pipe';
import { Reservation } from '../_model/reservation';
import { Books } from '../_model/books';
import { Users } from '../_model/users';

describe('ReservationsComponent (intégration)', () => {
  let component: ReservationsComponent;
  let fixture: ComponentFixture<ReservationsComponent>;
  let reservationService: jasmine.SpyObj<ReservationService>;
  let booksService: jasmine.SpyObj<BooksService>;
  let usersService: jasmine.SpyObj<UsersService>;
  let userAuthService: jasmine.SpyObj<UserAuthService>;

  const reservationExemple = (id: number): Reservation =>
    ({ id, livreId: 3, livreNom: 'L2', adherentId: 2, adherentNom: 'A1',
       dateReservation: '2026-09-11T10:00:00', dateExpiration: '2026-09-18T10:00:00',
       statut: 'EN_ATTENTE' }) as Reservation;
  const userAdherent = () =>
    ({ userId: 2, username: 'A1', name: 'A1', role: [{ roleName: 'User' }] }) as unknown as Users;

  beforeEach(async () => {
    reservationService = jasmine.createSpyObj('ReservationService',
      ['getReservations', 'createReservation', 'annulerReservation', 'getExpiredReservations', 'deleteReservation']);
    reservationService.getReservations.and.returnValue(of([reservationExemple(1)]));
    reservationService.getExpiredReservations.and.returnValue(of([]));

    booksService = jasmine.createSpyObj('BooksService', ['getBooksList']);
    booksService.getBooksList.and.returnValue(of([{ bookId: 3, bookName: 'L2', noOfCopies: 0 } as Books]));

    usersService = jasmine.createSpyObj('UsersService', ['getUsersList']);
    usersService.getUsersList.and.returnValue(of([userAdherent()]));

    userAuthService = jasmine.createSpyObj('UserAuthService', ['getRoles', 'getUserId']);
    userAuthService.getRoles.and.returnValue([{ roleName: 'ADHERENT' }]);
    userAuthService.getUserId.and.returnValue(2);

    await TestBed.configureTestingModule({
      imports: [CommonModule, FormsModule],
      declarations: [
        ReservationsComponent,
        ReservationFormComponent,
        ReservationListComponent,
        TranslatePipe,
      ],
      providers: [
        { provide: ReservationService, useValue: reservationService },
        { provide: BooksService, useValue: booksService },
        { provide: UsersService, useValue: usersService },
        { provide: UserAuthService, useValue: userAuthService },
        { provide: TranslationService, useValue: { translate: (key: string) => key, getLang: () => 'fr' } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReservationsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('rôles et référentiels', () => {
    it('un ADHERENT réserve pour lui-même : pas de chargement des adhérents', () => {
      expect(component.estBibliothecaire).toBeFalse();
      expect(component.monUserId).toBe(2);
      expect(usersService.getUsersList).not.toHaveBeenCalled();
      expect(component.adherents).toEqual([]);
    });

    it('un BIBLIOTHECAIRE charge la liste des adhérents', () => {
      userAuthService.getRoles.and.returnValue([{ roleName: 'BIBLIOTHECAIRE' }]);
      component.ngOnInit();

      expect(component.estBibliothecaire).toBeTrue();
      expect(usersService.getUsersList).toHaveBeenCalled();
      expect(component.adherents.length).toBe(1);
    });
  });

  describe('chargerReservations', () => {
    it('passe à l état donnees quand la liste n est pas vide', () => {
      expect(component.etat).toBe('donnees');
      expect(component.reservations.length).toBe(1);
    });

    it('passe à l état vide quand la liste est vide', () => {
      reservationService.getReservations.and.returnValue(of([]));
      component.chargerReservations();
      expect(component.etat).toBe('vide');
    });

    it('passe à l état erreur quand le serveur échoue', () => {
      reservationService.getReservations.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 0 })));
      component.chargerReservations();
      expect(component.etat).toBe('erreur');
    });

    it('recharge avec le statut filtré', () => {
      component.filtreStatut = 'EN_ATTENTE';
      component.chargerReservations();
      expect(reservationService.getReservations).toHaveBeenCalledWith('EN_ATTENTE');
    });
  });

  describe('onCreer', () => {
    it('crée la réservation, réinitialise le formulaire et recharge', () => {
      reservationService.createReservation.and.returnValue(of(reservationExemple(2)));
      component.onCreer({ livreId: 3, adherentId: 2 });

      expect(reservationService.createReservation).toHaveBeenCalledWith({ livreId: 3, adherentId: 2 });
      expect(component.creationEnCours).toBeFalse();
      expect(component.resetFormulaire).toBe(1);
      expect(component.erreurFormulaire).toBeNull();
    });

    it('affiche le message du serveur quand la création échoue', () => {
      reservationService.createReservation.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'RG-03: limite atteinte' } })));
      component.onCreer({ livreId: 3, adherentId: 2 });

      expect(reservationService.createReservation).toHaveBeenCalled();
      expect(component.erreurFormulaire).toContain('RG-03');
    });

    it('gère un serveur injoignable', () => {
      reservationService.createReservation.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 0 })));
      component.onCreer({ livreId: 3, adherentId: 2 });

      expect(component.erreurFormulaire).toContain('injoignable');
    });
  });

  describe('onAnnuler', () => {
    it('annule la réservation puis recharge', () => {
      reservationService.annulerReservation.and.returnValue(of(reservationExemple(1)));
      component.onAnnuler(1);

      expect(reservationService.annulerReservation).toHaveBeenCalledWith(1);
      expect(component.annulationEnCoursId).toBeNull();
    });

    it('affiche l erreur quand l annulation échoue', () => {
      reservationService.annulerReservation.and.returnValue(
        throwError(() => new HttpErrorResponse({ status: 409, error: { message: 'RG-05: statut' } })));
      component.onAnnuler(1);

      expect(component.annulationEnCoursId).toBeNull();
      expect(component.erreurAnnulation).toContain('RG-05');
    });
  });

  describe('INTEGRATION parent <-> enfants', () => {
    it('soumission du formulaire enfant : le parent appelle le service et recharge', () => {
      reservationService.createReservation.and.returnValue(of(reservationExemple(2)));
      const child = fixture.debugElement
        .query(By.directive(ReservationFormComponent)).componentInstance as ReservationFormComponent;

      child.livreId = 3;
      child.readonlyAdherentId = 2;
      child.onSubmit();

      expect(reservationService.createReservation).toHaveBeenCalledWith({ livreId: 3, adherentId: 2 });
      expect(reservationService.getReservations).toHaveBeenCalled();
    });

    it('annulation depuis la liste enfant : le parent appelle le service', () => {
      reservationService.annulerReservation.and.returnValue(of(reservationExemple(1)));
      spyOn(window, 'confirm').and.returnValue(true);
      const listChild = fixture.debugElement
        .query(By.directive(ReservationListComponent)).componentInstance as ReservationListComponent;

      listChild.onAnnuler(component.reservations[0]);

      expect(reservationService.annulerReservation).toHaveBeenCalledWith(1);
    });

    it('affiche les lignes de réservations quand des données arrivent', () => {
      const lignes = fixture.nativeElement.querySelectorAll('tbody tr');
      expect(lignes.length).toBe(1);
    });

    it('affiche un message quand la liste est vide', () => {
      reservationService.getReservations.and.returnValue(of([]));
      component.chargerReservations();
      fixture.detectChanges();
      expect(fixture.nativeElement.querySelector('.alert-info')).toBeTruthy();
    });
  });
});