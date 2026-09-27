package com.kalibra.api.enrollment.application.internal.outboundservices.acl;

import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.interfaces.acl.CurriculumContextFacade;
import com.kalibra.api.shared.contracts.curriculum.CourseSummary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("enrollmentExternalCurriculumService")
public class ExternalCurriculumService {

    private final CurriculumContextFacade curriculumContextFacade;

    public ExternalCurriculumService(
            CurriculumContextFacade curriculumContextFacade
    ) {
        this.curriculumContextFacade = curriculumContextFacade;
    }

    public boolean isCourseOwnedBy(CourseId courseId, String holderId) {
        return curriculumContextFacade.isCourseOwnedBy(
                courseId.value(),
                holderId
        );
    }

    public Optional<CourseSummary> fetchCourseSummary(CourseId courseId) {
        return curriculumContextFacade.fetchCourseSummary(
                courseId.value()
        );
    }

    public List<CourseSummary> fetchCoursesByHolderId(String holderId) {
        return curriculumContextFacade.fetchCoursesByHolderId(holderId);
    }
}