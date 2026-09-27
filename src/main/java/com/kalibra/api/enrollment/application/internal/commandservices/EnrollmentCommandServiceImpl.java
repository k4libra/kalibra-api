package com.kalibra.api.enrollment.application.internal.commandservices;

import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.repositories.EnrollmentRepository;
import com.kalibra.api.enrollment.domain.services.EnrollmentCommandService;
import org.springframework.stereotype.Service;

@Service
public class EnrollmentCommandServiceImpl implements EnrollmentCommandService {

    private final EnrollmentRepository enrollmentRepository;

    public EnrollmentCommandServiceImpl(
            EnrollmentRepository enrollmentRepository
    ) {
        this.enrollmentRepository = enrollmentRepository;
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

        return enrollmentRepository.save(enrollment);
    }
}