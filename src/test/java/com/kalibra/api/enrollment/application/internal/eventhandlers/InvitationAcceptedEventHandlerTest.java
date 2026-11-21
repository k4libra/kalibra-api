package com.kalibra.api.enrollment.application.internal.eventhandlers;

import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.events.InvitationAccepted;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.services.EnrollmentCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InvitationAcceptedEventHandlerTest {

    @Mock
    EnrollmentCommandService enrollmentCommandService;

    @InjectMocks
    InvitationAcceptedEventHandler handler;

    @Test
    void shouldEnrollTheStudentOfTheAcceptedInvitation() {
        var event = new InvitationAccepted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "ana@kalibra.pe");

        handler.handle(event);

        verify(enrollmentCommandService).handle(new EnrollStudentCommand(new InvitationId(event.invitationId()),
                new CourseId(event.courseId()), new StudentId(event.studentId()), new Email(event.studentEmail())));
    }
}
