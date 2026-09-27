package com.kalibra.api.enrollment.domain.model.aggregates;

import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.valueobjects.*;

import java.time.Instant;
import java.util.UUID;

public class Enrollment {

    private EnrollmentId id;
    private CourseId courseId;
    private StudentId studentId;
    private Email studentEmail;
    private InvitationId invitationId;
    private Instant enrolledAt;

    public Enrollment() {
    }

    public static Enrollment fromAcceptedInvitation(EnrollStudentCommand command) {
        var enrollment = new Enrollment();

        enrollment.id = new EnrollmentId(UUID.randomUUID());
        enrollment.courseId = command.courseId();
        enrollment.studentId = command.studentId();
        enrollment.studentEmail = command.studentEmail();
        enrollment.invitationId = command.invitationId();
        enrollment.enrolledAt = Instant.now();

        return enrollment;
    }

    public EnrollmentId getId() {
        return id;
    }

    public CourseId getCourseId() {
        return courseId;
    }

    public StudentId getStudentId() {
        return studentId;
    }

    public Email getStudentEmail() {
        return studentEmail;
    }

    public InvitationId getInvitationId() {
        return invitationId;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }
}