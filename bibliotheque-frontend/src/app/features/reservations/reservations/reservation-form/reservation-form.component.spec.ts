import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { ReservationFormComponent } from './reservation-form.component';
import { ReservationService } from '../../../../core/api/reservation.service';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { TranslationService } from '../../../../core/services/translation.service';
import { Books } from '../../../../shared/models/books';
import { Users } from '../../../../shared/models/users';
import { Reservation, ReservationRequest } from '../../../../shared/models/reservation';

describe('ReservationFormComponent', () => {
  let component: ReservationFormComponent;
  let fixture: ComponentFixture<ReservationFormComponent>;
  let reservationService: jasmine.SpyObj<ReservationService>;

  const livres: Books[] = [
    { bookId: 3, bookName: 'L2', noOfCopies: 0 } as Books,
    { bookId: 4, bookName: 'L3', noOfCopies: 0 } as Books,
  ];
  const adherents: Users[] = [
    { userId: 2, username: 'A1', name: 'A1' } as Users,
    { userId: 3, username: 'A2', name: 'A2' } as Users,
  ];

  beforeEach(async () => {
    reservationService = jasmine.createSpyObj('ReservationService', ['getReservationsByAdherent']);
    reservationService.getReservationsByAdherent.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [ReservationFormComponent, TranslatePipe],
      providers: [
        { provide: ReservationService, useValue: reservationService },
        {
          provide: TranslationService,
          useValue: { translate: (key: string) => key, getLang: () => 'fr' },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReservationFormComponent);
    component = fixture.componentInstance;
    component.livres = livres;
    component.adherents = adherents;
    fixture.detectChanges();
  });

  describe('formValide', () => {
    it('est faux tant que le livre n est pas choisi', () => {
      component.adherentId = 2;
      expect(component.formValide).toBeFalse();
    });

    it('est vrai quand livre et adhérent sont choisis (bibliothécaire)', () => {
      component.livreId = 3;
      component.adherentId = 2;
      expect(component.formValide).toBeTrue();
    });

    it('est vrai avec un adhérent imposé (ADHERENT réserve pour lui-même)', () => {
      component.livreId = 3;
      component.readonlyAdherentId = 2;
      component.adherentId = null;
      expect(component.formValide).toBeTrue();
    });

    it('est faux quand l adhérent imposé est absent', () => {
      component.livreId = 3;
      component.readonlyAdherentId = null;
      component.adherentId = null;
      expect(component.formValide).toBeFalse();
    });
  });

  describe('onSubmit', () => {
    it('émet la demande avec l adhérent choisi (bibliothécaire)', () => {
      let emitted: ReservationRequest | undefined;
      component.create.subscribe((r) => emitted = r);
      component.livreId = 3;
      component.adherentId = 2;

      component.onSubmit();

      expect(emitted).toEqual({ livreId: 3, adherentId: 2 });
    });

    it('émet la demande avec l adhérent imposé, jamais un autre (RS-04)', () => {
      let emitted: ReservationRequest | undefined;
      component.create.subscribe((r) => emitted = r);
      component.livreId = 3;
      component.readonlyAdherentId = 2;
      component.adherentId = 99;

      component.onSubmit();

      expect(emitted).toEqual({ livreId: 3, adherentId: 2 });
    });

    it('n émet rien si le formulaire est invalide', () => {
      let emitted = false;
      component.create.subscribe(() => emitted = true);
      component.livreId = null;
      component.onSubmit();
      expect(emitted).toBeFalse();
    });

    it('n émet rien pendant une soumission en cours', () => {
      let emitted = false;
      component.create.subscribe(() => emitted = true);
      component.livreId = 3;
      component.readonlyAdherentId = 2;
      component.submitting = true;
      component.onSubmit();
      expect(emitted).toBeFalse();
    });
  });

  describe('onAdherentChange / quota', () => {
    it('compte uniquement les réservations actives de l adhérent', () => {
      const reservation = (statut: string) => ({ statut } as Reservation);
      reservationService.getReservationsByAdherent.and.returnValue(of([
        reservation('EN_ATTENTE'),
        reservation('DISPONIBLE'),
        reservation('ANNULEE'),
        reservation('EXPIREE'),
      ]));

      component.adherentId = 2;
      component.onAdherentChange();

      expect(component.activeCount).toBe(2);
      expect(component.loadingCount).toBeFalse();
    });

    it('détecte le quota atteint à 3 réservations actives', () => {
      component.activeCount = 3;
      expect(component.quotaAtteint).toBeTrue();
    });

    it('ne considère pas le quota atteint si le compteur est null', () => {
      component.activeCount = null;
      expect(component.quotaAtteint).toBeFalse();
    });
  });

  describe('TEMPLATE (intégration composant + vue)', () => {
    it('liste les livres dans le select', () => {
      const options = fixture.nativeElement.querySelectorAll('#livreId option') as NodeListOf<HTMLElement>;
      expect(options.length).toBe(livres.length + 1);
    });

    it('affiche un champ adhérent en lecture seule pour soi-même (RS-04)', () => {
      component.readonlyAdherentId = 2;
      fixture.detectChanges();

      const select = fixture.nativeElement.querySelector('#adherentId') as HTMLInputElement;
      expect(select.tagName).toBe('INPUT');
      expect(select.readOnly).toBeTrue();
    });

    it('affiche un select adhérent pour le bibliothécaire', () => {
      component.readonlyAdherentId = null;
      fixture.detectChanges();

      const select = fixture.nativeElement.querySelector('#adherentId') as HTMLElement;
      expect(select.tagName).toBe('SELECT');
    });

    it('désactive le bouton tant que le formulaire est incomplet', () => {
      const bouton = fixture.nativeElement.querySelector('button[type=submit]') as HTMLButtonElement;
      expect(bouton.disabled).toBeTrue();

      component.livreId = 3;
      component.readonlyAdherentId = 2;
      fixture.detectChanges();
      expect(bouton.disabled).toBeFalse();
    });
  });
});