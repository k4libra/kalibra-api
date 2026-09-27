package com.kalibra.api.iam.application.acl;

import com.kalibra.api.iam.domain.model.aggregates.User;
import com.kalibra.api.iam.domain.model.queries.GetUserByEmailQuery;
import com.kalibra.api.iam.domain.model.queries.GetUserByIdQuery;
import com.kalibra.api.iam.domain.model.valueobjects.ClientApplication;
import com.kalibra.api.iam.domain.model.valueobjects.Email;
import com.kalibra.api.iam.domain.model.valueobjects.HashedPassword;
import com.kalibra.api.iam.domain.services.UserQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IamContextFacadeImplTest {

    @Mock
    UserQueryService userQueryService;

    @InjectMocks
    IamContextFacadeImpl facade;

    @Test
    void shouldReturnIdWhenEmailBelongsToAStudent() {
        // Arrange
        var student = User.register(new Email("ada@kalibra.com"), new HashedPassword("hashed"), ClientApplication.MOBILE_APP);
        when(userQueryService.handle(any(GetUserByEmailQuery.class))).thenReturn(Optional.of(student));

        // Act & Assert
        assertThat(facade.fetchStudentIdByEmail("ada@kalibra.com")).contains(student.getId());
    }

    @Test
    void shouldReturnEmptyWhenEmailBelongsToATeacher() {
        // Arrange
        var teacher = User.register(new Email("bob@kalibra.com"), new HashedPassword("hashed"), ClientApplication.WEB_PLATFORM);
        when(userQueryService.handle(any(GetUserByEmailQuery.class))).thenReturn(Optional.of(teacher));

        // Act & Assert
        assertThat(facade.fetchStudentIdByEmail("bob@kalibra.com")).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenEmailIsUnknown() {
        // Arrange
        when(userQueryService.handle(any(GetUserByEmailQuery.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThat(facade.fetchStudentIdByEmail("missing@kalibra.com")).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenEmailIsMalformed() {
        assertThat(facade.fetchStudentIdByEmail("not-an-email")).isEmpty();
    }

    @Test
    void shouldReturnSummaryWithNeutralTypesWhenUserExists() {
        // Arrange
        var user = User.register(new Email("ada@kalibra.com"), new HashedPassword("hashed"), ClientApplication.MOBILE_APP);
        when(userQueryService.handle(any(GetUserByIdQuery.class))).thenReturn(Optional.of(user));

        // Act
        var summary = facade.fetchUserSummary(user.getId());

        // Assert
        assertThat(summary).isPresent();
        assertThat(summary.get().userId()).isEqualTo(user.getId());
        assertThat(summary.get().email()).isEqualTo("ada@kalibra.com");
        assertThat(summary.get().roles()).containsExactlyInAnyOrder("REGISTERED_USER", "STUDENT");
    }

    @Test
    void shouldReturnEmptySummaryWhenUserDoesNotExist() {
        // Arrange
        when(userQueryService.handle(any(GetUserByIdQuery.class))).thenReturn(Optional.empty());

        // Act & Assert
        assertThat(facade.fetchUserSummary(UUID.randomUUID())).isEmpty();
    }
}
