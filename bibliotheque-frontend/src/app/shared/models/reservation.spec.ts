import { Reservation, ReservationRequest, ReservationStatus } from './reservation';

describe('ReservationModel', () => {
  it('instancie une réservation et mappe tous ses champs', () => {
    const reservation = new Reservation();
    reservation.id = 1;
    reservation.livreId = 3;
    reservation.livreNom = 'L2';
    reservation.adherentId = 2;
    reservation.adherentNom = 'A1';
    reservation.dateReservation = '2026-09-11T10:00:00';
    reservation.dateExpiration = '2026-09-18T10:00:00';
    reservation.statut = 'EN_ATTENTE';

    expect(reservation.id).toBe(1);
    expect(reservation.livreId).toBe(3);
    expect(reservation.livreNom).toBe('L2');
    expect(reservation.adherentId).toBe(2);
    expect(reservation.adherentNom).toBe('A1');
    expect(reservation.dateReservation).toBe('2026-09-11T10:00:00');
    expect(reservation.dateExpiration).toBe('2026-09-18T10:00:00');
    expect(reservation.statut).toBe('EN_ATTENTE');
  });

  it('expose les cinq statuts possibles d une réservation', () => {
    const statuts: ReservationStatus[] = ['EN_ATTENTE', 'DISPONIBLE', 'ANNULEE', 'EXPIREE', 'HONOREE'];
    const reservation = new Reservation();
    statuts.forEach((statut) => {
      reservation.statut = statut;
      expect(reservation.statut).toBe(statut);
    });
  });

  it('crée une ReservationRequest contenant livreId et adherentId', () => {
    const request = new ReservationRequest();
    request.livreId = 3;
    request.adherentId = 7;
    expect(request.livreId).toBe(3);
    expect(request.adherentId).toBe(7);
  });
});