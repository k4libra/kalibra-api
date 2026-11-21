package com.kalibra.api.enrollment.domain.model.aggregates;

import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.EnrollmentId;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;

import java.time.Instant;
import java.util.UUID;

public class Enrollment {

    private EnrollmentId id;
    private CourseId courseId;
    private StudentId studentId;
    private Email studentEmail;
    private InvitationId invitationId;
    private Instant enrolledAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
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

    public void setId(EnrollmentId id) {
        this.id = id;
    }

    public void setCourseId(CourseId courseId) {
        this.courseId = courseId;
    }

    public void setStudentId(StudentId studentId) {
        this.studentId = studentId;
    }

    public void setStudentEmail(Email studentEmail) {
        this.studentEmail = studentEmail;
    }

    public void setInvitationId(InvitationId invitationId) {
        this.invitationId = invitationId;
    }

    public void setEnrolledAt(Instant enrolledAt) {
        this.enrolledAt = enrolledAt;
    }
}
