package com.kalibra.api.enrollment.domain.model.aggregates;

import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.EnrollmentId;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;

import java.time.Instant;

public class Enrollment {

    private EnrollmentId id;
    private CourseId courseId;
    private StudentId studentId;
    private Email studentEmail;
    private InvitationId invitationId;
    private Instant enrolledAt;

    public static Enrollment fromAcceptedInvitation(
            EnrollStudentCommand command
    ) {
        return null;
    }
}