package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.ReservationRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.ReservationRequest;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Reservation;
import com.ibizabroker.bibliotheque.entity.StatutReservation;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BooksRepository booksRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private ReservationService reservationService;

    private Books livreIndisponible;
    private Users adherent;

    @BeforeEach
    void setUp() {
        livreIndisponible = new Books();
        livreIndisponible.setBookId(101);
        livreIndisponible.setBookName("Livre Indisponible");
        livreIndisponible.setNoOfCopies(0);

        adherent = new Users();
        adherent.setUserId(1);
        adherent.setName("Adherent Test");

        when(booksRepository.findById(101)).thenReturn(Optional.of(livreIndisponible));
        when(usersRepository.findById(1)).thenReturn(Optional.of(adherent));
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

    @Test
    void rg01_refuseLaReservationDUnLivreDisponible() {
        Books livreDisponible = new Books();
        livreDisponible.setBookId(202);
        livreDisponible.setBookName("Livre Disponible");
        livreDisponible.setNoOfCopies(2);
        when(booksRepository.findById(202)).thenReturn(Optional.of(livreDisponible));

        ReservationRequest request = new ReservationRequest();
        request.setLivreId(202);
        request.setAdherentId(1);

        assertThatThrownBy(() -> reservationService.creer(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("RG-01");
    }
}
