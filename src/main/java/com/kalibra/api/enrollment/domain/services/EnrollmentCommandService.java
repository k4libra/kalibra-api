package com.kalibra.api.enrollment.domain.services;

import com.kalibra.api.enrollment.domain.model.aggregates.Enrollment;
import com.kalibra.api.enrollment.domain.model.commands.EnrollStudentCommand;

public interface EnrollmentCommandService {

    Enrollment handle(EnrollStudentCommand command);
}