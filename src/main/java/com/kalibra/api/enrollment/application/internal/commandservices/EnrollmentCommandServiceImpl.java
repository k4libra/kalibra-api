package com.kalibra.api.enrollment.application.internal.commandservices;

import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.events.StudentEnrolled;
import com.kalibra.api.enrollment.domain.repositories.EnrollmentRepository;
import com.kalibra.api.enrollment.domain.services.EnrollmentCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class EnrollmentCommandServiceImpl implements EnrollmentCommandService {

    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate requiresNew;

    public EnrollmentCommandServiceImpl(EnrollmentRepository enrollmentRepository,
                                        ApplicationEventPublisher eventPublisher,
                                        PlatformTransactionManager transactionManager) {
        this.enrollmentRepository = enrollmentRepository;
        this.eventPublisher = eventPublisher;
        // REQUIRES_NEW: this runs from an AFTER_COMMIT listener, where joining the finished
        // transaction would silently drop the writes.
        this.requiresNew = new TransactionTemplate(transactionManager);
        this.requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    // Idempotent: the event handler and the facade's self-healing may both enroll the same
    // student; the one that loses the race gets the enrollment the other one saved.
    @Override
    public Enrollment handle(EnrollStudentCommand command) {
        try {
            return requiresNew.execute(status -> enrollmentRepository
                    .findByStudentIdAndCourseId(command.studentId(), command.courseId())
                    .orElseGet(() -> enroll(command)));
        } catch (DataIntegrityViolationException concurrentEnrollment) {
            return requiresNew.execute(status -> enrollmentRepository
                    .findByStudentIdAndCourseId(command.studentId(), command.courseId())
                    .orElseThrow(() -> concurrentEnrollment));
        }
    }

    private Enrollment enroll(EnrollStudentCommand command) {
        var saved = enrollmentRepository.save(Enrollment.fromAcceptedInvitation(command));
        eventPublisher.publishEvent(new StudentEnrolled(
                saved.getId().value(), saved.getCourseId().value(), saved.getStudentId().value()));
        return saved;
    }
}
