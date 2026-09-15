package com.ibizabroker.bibliotheque;

import com.ibizabroker.bibliotheque.catalogue.internal.BooksRepository;
import com.ibizabroker.bibliotheque.emprunts.internal.BorrowRepository;
import com.ibizabroker.bibliotheque.reservations.internal.ReservationRepository;
import com.ibizabroker.bibliotheque.utilisateurs.internal.UsersRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie que le contexte applicatif se charge sans base de données ni
 * Keycloak : les repositories et le décodeur JWT sont simulés, les
 * auto-configurations de persistance sont désactivées.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration",
        "app.keycloak.jwks-uri=http://localhost:9999/realms/bibliotheque/protocol/openid-connect/certs",
        "app.keycloak.issuer-local=http://localhost:9999/realms/bibliotheque",
        "app.keycloak.issuer-internal=http://localhost:9999/realms/bibliotheque"
})
class BibliothequeApplicationTests {

	@MockBean
	private ReservationRepository reservationRepository;

	@MockBean
	private BooksRepository booksRepository;

	@MockBean
	private UsersRepository usersRepository;

	@MockBean
	private BorrowRepository borrowRepository;

	@MockBean
	private JwtDecoder jwtDecoder;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		assertThat(applicationContext).isNotNull();
		assertThat(applicationContext.getBean(BooksRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(UsersRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(BorrowRepository.class)).isNotNull();
		assertThat(applicationContext.getBean(ReservationRepository.class)).isNotNull();
	}

}
