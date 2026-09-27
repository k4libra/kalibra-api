package com.kalibra.api.iam.domain.model.aggregates;

import com.kalibra.api.iam.domain.model.valueobjects.ClientApplication;
import com.kalibra.api.iam.domain.model.valueobjects.Email;
import com.kalibra.api.iam.domain.model.valueobjects.HashedPassword;
import com.kalibra.api.iam.domain.model.valueobjects.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    private static final Email EMAIL = new Email("ada@kalibra.com");
    private static final HashedPassword PASSWORD = new HashedPassword("hashed");

    @Test
    void shouldRegisterStudentRoleWhenApplicationIsMobile() {
        // Act
        var user = User.register(EMAIL, PASSWORD, ClientApplication.MOBILE_APP);

        // Assert
        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo(EMAIL);
        assertThat(user.getHashedPassword()).isEqualTo(PASSWORD);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.REGISTERED_USER, Role.STUDENT);
    }

    @Test
    void shouldRegisterTeacherRoleWhenApplicationIsWeb() {
        // Act
        var user = User.register(EMAIL, PASSWORD, ClientApplication.WEB_PLATFORM);

        // Assert
        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.REGISTERED_USER, Role.TEACHER);
    }

    @Test
    void shouldReportRolesThroughHasRole() {
        // Arrange
        var user = User.register(EMAIL, PASSWORD, ClientApplication.MOBILE_APP);

        // Act & Assert
        assertThat(user.hasRole(Role.STUDENT)).isTrue();
        assertThat(user.hasRole(Role.TEACHER)).isFalse();
    }

    @Test
    void shouldKeepExistingRolesWhenGrantingAdministrator() {
        // Arrange
        var user = User.register(EMAIL, PASSWORD, ClientApplication.WEB_PLATFORM);

        // Act
        user.grantAdministratorRole();

        // Assert
        assertThat(user.getRoles())
                .containsExactlyInAnyOrder(Role.REGISTERED_USER, Role.TEACHER, Role.ADMINISTRATOR);
    }
}
