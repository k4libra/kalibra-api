package com.kalibra.api.enrollment.interfaces.rest.transform;

import com.kalibra.api.enrollment.domain.model.valueobjects.CourseRosterGroup;
import com.kalibra.api.enrollment.domain.model.valueobjects.EnrolledCourse;
import com.kalibra.api.enrollment.domain.model.valueobjects.RosterLine;
import com.kalibra.api.enrollment.interfaces.rest.resources.CourseRosterResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.EnrolledCourseResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.EnrolledSubtopicResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.RosterStudentResource;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EnrollmentAssembler {

    RosterStudentResource toResource(RosterLine rosterLine);

    CourseRosterResource toResource(CourseRosterGroup courseRosterGroup);

    EnrolledCourseResource toResource(EnrolledCourse enrolledCourse);

    EnrolledSubtopicResource toResource(EnrolledCourse.EnrolledSubtopic subtopic);
}