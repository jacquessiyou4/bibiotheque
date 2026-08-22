package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.LivreRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.AdherentRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.entity.Livre;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Adherent;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private LivreRepository livreRepository;

    @Mock
    private AdherentRepository adherentRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Livre livreIndisponible;
    private Adherent adherent;

    @BeforeEach
    void setUp() {
        livreIndisponible = new Livre();
        livreIndisponible.setBookId(101);
        livreIndisponible.setBookName("Livre Indisponible");
        livreIndisponible.setNoOfCopies(0);

        adherent = new Adherent();
        adherent.setUserId(1);
        adherent.setName("Adherent Test");

        when(livreRepository.findById(101)).thenReturn(Optional.of(livreIndisponible));
        when(adherentRepository.findById(1)).thenReturn(Optional.of(adherent));
        when(reservationRepository.findByLivre_BookIdAndAdherent_UserIdAndStatutIn(any(), any(), any()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void rg03_refuseLaQuatriemeReservationActive() {
        when(reservationRepository.countByAdherent_UserIdAndStatutIn(anyInt(), any()))
                .thenReturn(3L);

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);
        request.setAdherentId(1);

        assertThatThrownBy(() -> reservationService.creer(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-03");
    }

    @Test
    void rg03_autoriseLaTroisiemeReservationActive() {
        when(reservationRepository.countByAdherent_UserIdAndStatutIn(anyInt(), any()))
                .thenReturn(2L);
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(101);
        request.setAdherentId(1);

        assertThat(reservationService.creer(request).getStatut()).isEqualTo(StatutReservation.EN_ATTENTE);
    }
}
