import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ReservationService } from './reservation.service';
import { Reservation, ReservationRequest } from '../../shared/models/reservation';

describe('ReservationService', () => {
  let service: ReservationService;
  let http: HttpTestingController;

  const baseURL = 'http://localhost:8080/api/v1/reservations';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(ReservationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('getReservations envoie un GET sur /api/reservations', () => {
    const attendu: Reservation[] = [{ id: 1 } as Reservation];

    service.getReservations().subscribe((res) => expect(res).toEqual(attendu));

    const req = http.expectOne(baseURL);
    expect(req.request.method).toBe('GET');
    req.flush(attendu);
  });

  it('getReservations ajoute les query params statut et adherentId quand fournis', () => {
    service.getReservations('EN_ATTENTE', 5).subscribe();

    const req = http.expectOne((r) => r.method === 'GET' &&
      r.params.get('statut') === 'EN_ATTENTE' &&
      r.params.get('adherentId') === '5');
    expect(req.request.params.get('statut')).toBe('EN_ATTENTE');
    expect(req.request.params.get('adherentId')).toBe('5');
    req.flush([]);
  });

  it('getReservationsByAdherent filtre par les réservations de l adhérent', () => {
    service.getReservationsByAdherent(7).subscribe();

    const req = http.expectOne((r) => r.method === 'GET' && r.params.get('adherentId') === '7');
    expect(req.request.params.get('adherentId')).toBe('7');
    req.flush([]);
  });

  it('createReservation envoie un POST avec le corps de la demande', () => {
    const request: ReservationRequest = { livreId: 3, adherentId: 7 };
    const cree: Reservation = { id: 9, livreId: 3, adherentId: 7 } as Reservation;

    service.createReservation(request).subscribe((res) => expect(res).toEqual(cree));

    const req = http.expectOne(baseURL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(cree);
  });

  it('annulerReservation envoie un PATCH sur /{id}/annuler', () => {
    const annulee: Reservation = { id: 9, statut: 'ANNULEE' } as Reservation;

    service.annulerReservation(9).subscribe((res) => expect(res).toEqual(annulee));

    const req = http.expectOne(`${baseURL}/9/annuler`);
    expect(req.request.method).toBe('PATCH');
    req.flush(annulee);
  });

  it('getReservationById envoie un GET sur /{id}', () => {
    const attendu: Reservation = { id: 5, livreId: 2, statut: 'EN_ATTENTE' } as Reservation;

    service.getReservationById(5).subscribe((res) => expect(res).toEqual(attendu));

    const req = http.expectOne(`${baseURL}/5`);
    expect(req.request.method).toBe('GET');
    req.flush(attendu);
  });

  it('getExpiredReservations envoie un GET sur /expirees', () => {
    const attendu: Reservation[] = [{ id: 10, statut: 'EXPIREE' } as Reservation];

    service.getExpiredReservations().subscribe((res) => expect(res).toEqual(attendu));

    const req = http.expectOne(`${baseURL}/expirees`);
    expect(req.request.method).toBe('GET');
    req.flush(attendu);
  });

  it('deleteReservation envoie un DELETE sur /{id}', () => {
    service.deleteReservation(4).subscribe();

    const req = http.expectOne(`${baseURL}/4`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });
});