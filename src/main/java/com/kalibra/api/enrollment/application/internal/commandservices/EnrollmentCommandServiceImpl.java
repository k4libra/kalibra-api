package com.kalibra.api.enrollment.application.internal.commandservices;

import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.events.StudentEnrolled;
import com.kalibra.api.enrollment.domain.repositories.EnrollmentRepository;
import com.kalibra.api.enrollment.domain.services.EnrollmentCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentCommandServiceImpl implements EnrollmentCommandService {

    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public EnrollmentCommandServiceImpl(
            EnrollmentRepository enrollmentRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Enrollment handle(EnrollStudentCommand command) {

        if (enrollmentRepository.existsByInvitationId(command.invitationId())) {
            return enrollmentRepository
                    .findByStudentIdAndCourseId(
                            command.studentId(),
                            command.courseId()
                    )
                    .orElseThrow();
        }

        var enrollment = Enrollment.fromAcceptedInvitation(command);

        var saved = enrollmentRepository.save(enrollment);

        eventPublisher.publishEvent(new StudentEnrolled(
                saved.getId().value(),
                saved.getCourseId().value(),
                saved.getStudentId().value()
        ));

        return saved;
    }
}