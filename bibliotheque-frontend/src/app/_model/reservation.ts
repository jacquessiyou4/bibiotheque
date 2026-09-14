export type ReservationStatus = 'EN_ATTENTE' | 'DISPONIBLE' | 'ANNULEE' | 'EXPIREE' | 'HONOREE';

export class Reservation {
    id: number;
    livreId: number;
    livreNom: string;
    adherentId: number;
    adherentNom: string;
    dateReservation: string;
    dateExpiration: string;
    statut: ReservationStatus;
}

export class ReservationRequest {
    livreId: number;
    adherentId: number;
}
