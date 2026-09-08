package com.controlplane.backend.repository;

import com.controlplane.backend.entity.User;
import com.controlplane.backend.entity.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against an embedded H2 database in tests so repository behavior can
 * be exercised without requiring Docker or a live PostgreSQL instance.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findByUsernameOrEmailMatchesEitherField() {
        User user = User.builder()
                .username("jane.doe")
                .email("jane.doe@example.com")
                .password("hashed")
                .firstName("Jane")
                .lastName("Doe")
                .status(UserStatus.ACTIVE)
                .build();
        entityManager.persistAndFlush(user);

        Optional<User> byUsername = userRepository.findByUsernameOrEmail("jane.doe");
        Optional<User> byEmail = userRepository.findByUsernameOrEmail("JANE.DOE@EXAMPLE.COM");

        assertThat(byUsername).isPresent();
        assertThat(byEmail).isPresent();
        assertThat(byUsername.get().getId()).isEqualTo(byEmail.get().getId());
    }

    @Test
    void existsByUsernameAndEmailReflectPersistedState() {
        User user = User.builder()
                .username("existing.user")
                .email("existing.user@example.com")
                .password("hashed")
                .firstName("Existing")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .build();
        entityManager.persistAndFlush(user);

        assertThat(userRepository.existsByUsername("existing.user")).isTrue();
        assertThat(userRepository.existsByEmail("existing.user@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("nobody")).isFalse();
    }

    @Test
    void findByUsernameOrEmailReturnsEmptyForUnknownIdentifier() {
        assertThat(userRepository.findByUsernameOrEmail("does-not-exist")).isEmpty();
    }
}
