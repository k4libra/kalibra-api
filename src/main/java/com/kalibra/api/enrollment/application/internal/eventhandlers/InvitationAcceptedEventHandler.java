package com.kalibra.api.enrollment.application.internal.eventhandlers;

import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.events.InvitationAccepted;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.enrollment.domain.services.EnrollmentCommandService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InvitationAcceptedEventHandler {

    private final EnrollmentCommandService enrollmentCommandService;

    public InvitationAcceptedEventHandler(
            EnrollmentCommandService enrollmentCommandService
    ) {
        this.enrollmentCommandService = enrollmentCommandService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(InvitationAccepted event) {

        var command = new EnrollStudentCommand(
                new InvitationId(event.invitationId()),
                new CourseId(event.courseId()),
                new StudentId(event.studentId()),
                new Email(event.studentEmail())
        );

        enrollmentCommandService.handle(command);
    }
}