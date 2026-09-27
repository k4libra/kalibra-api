package com.kalibra.api.curriculum.interfaces.acl;

import com.kalibra.api.shared.contracts.curriculum.CourseSummary;
import com.kalibra.api.shared.contracts.curriculum.SubtopicSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CurriculumContextFacade {

    boolean isCourseOwnedBy(UUID courseId, String holderId);

    Optional<CourseSummary> fetchCourseSummary(UUID courseId);

    List<CourseSummary> fetchCoursesByHolderId(String holderId);

    List<SubtopicSummary> fetchSubtopics(UUID courseId);
}
