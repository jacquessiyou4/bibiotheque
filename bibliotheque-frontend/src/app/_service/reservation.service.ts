import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Reservation, ReservationRequest, StatutReservation } from '../_model/reservation';
import { apiUrl } from './api-config';

@Injectable({
  providedIn: 'root'
})
export class ReservationService {

  private baseURL = `${apiUrl()}/api/reservations`;

  constructor(private httpClient: HttpClient) { }

  getReservations(statut?: StatutReservation, adherentId?: number): Observable<Reservation[]> {
    let params: any = {};
    if (statut) { params.statut = statut; }
    if (adherentId) { params.adherentId = adherentId; }
    return this.httpClient.get<Reservation[]>(this.baseURL, { params });
  }

  getReservationsByAdherent(adherentId: number): Observable<Reservation[]> {
    return this.getReservations(undefined, adherentId);
  }

  createReservation(request: ReservationRequest): Observable<Reservation> {
    return this.httpClient.post<Reservation>(this.baseURL, request);
  }

  annulerReservation(id: number): Observable<Reservation> {
    return this.httpClient.patch<Reservation>(`${this.baseURL}/${id}/annuler`, {});
  }
}
