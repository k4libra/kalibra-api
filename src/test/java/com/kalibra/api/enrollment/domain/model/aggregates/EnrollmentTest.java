package com.kalibra.api.enrollment.domain.model.aggregates;

import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentTest {

    @Test
    void shouldEnrollTheStudentOfTheAcceptedInvitation() {
        var command = new EnrollStudentCommand(new InvitationId(UUID.randomUUID()),
                new CourseId(UUID.randomUUID()), new StudentId(UUID.randomUUID()), new Email("ana@kalibra.pe"));

        var enrollment = Enrollment.fromAcceptedInvitation(command);

        assertThat(enrollment.getId()).isNotNull();
        assertThat(enrollment.getInvitationId()).isEqualTo(command.invitationId());
        assertThat(enrollment.getCourseId()).isEqualTo(command.courseId());
        assertThat(enrollment.getStudentId()).isEqualTo(command.studentId());
        assertThat(enrollment.getStudentEmail()).isEqualTo(command.studentEmail());
        assertThat(enrollment.getEnrolledAt()).isNotNull();
    }
}
