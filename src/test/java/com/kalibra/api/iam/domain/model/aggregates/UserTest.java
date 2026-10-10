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
        var user = User.register(EMAIL, "Ada", "Lovelace", PASSWORD, ClientApplication.MOBILE_APP);

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
        var user = User.register(EMAIL, "Ada", "Lovelace", PASSWORD, ClientApplication.WEB_PLATFORM);

        // Assert
        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.REGISTERED_USER, Role.TEACHER);
    }

    @Test
    void shouldReportRolesThroughHasRole() {
        // Arrange
        var user = User.register(EMAIL, "Ada", "Lovelace", PASSWORD, ClientApplication.MOBILE_APP);

        // Act & Assert
        assertThat(user.hasRole(Role.STUDENT)).isTrue();
        assertThat(user.hasRole(Role.TEACHER)).isFalse();
    }

    @Test
    void shouldKeepExistingRolesWhenGrantingAdministrator() {
        // Arrange
        var user = User.register(EMAIL, "Ada", "Lovelace", PASSWORD, ClientApplication.WEB_PLATFORM);

        // Act
        user.grantAdministratorRole();

        // Assert
        assertThat(user.getRoles())
                .containsExactlyInAnyOrder(Role.REGISTERED_USER, Role.TEACHER, Role.ADMINISTRATOR);
    }

    @Test
    void shouldRegisterAdministratorsWithoutTeacherOrStudentRoles() {
        var admin = User.registerAdministrator(EMAIL, "Admin", "Test", PASSWORD);

        assertThat(admin.getRoles()).containsExactlyInAnyOrder(Role.REGISTERED_USER, Role.ADMINISTRATOR);
        assertThat(admin.canUse(ClientApplication.WEB_PLATFORM)).isTrue();
        assertThat(admin.canUse(ClientApplication.MOBILE_APP)).isFalse();
    }

    @Test
    void shouldRestrictEachRoleToItsClientApplication() {
        var student = User.register(EMAIL, "Ada", "Lovelace", PASSWORD, ClientApplication.MOBILE_APP);
        var teacher = User.register(EMAIL, "Ada", "Lovelace", PASSWORD, ClientApplication.WEB_PLATFORM);

        assertThat(student.canUse(ClientApplication.MOBILE_APP)).isTrue();
        assertThat(student.canUse(ClientApplication.WEB_PLATFORM)).isFalse();
        assertThat(teacher.canUse(ClientApplication.WEB_PLATFORM)).isTrue();
        assertThat(teacher.canUse(ClientApplication.MOBILE_APP)).isFalse();
    }

    @Test
    void shouldRejectBlankNames() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        User.register(EMAIL, " ", "Lovelace", PASSWORD, ClientApplication.MOBILE_APP))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
