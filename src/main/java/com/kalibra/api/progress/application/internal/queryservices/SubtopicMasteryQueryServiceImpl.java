package com.kalibra.api.progress.application.internal.queryservices;

import com.kalibra.api.progress.domain.model.queries.GetStudentProgressByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.StudentProgressReport;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.progress.domain.services.SubtopicMasteryQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SubtopicMasteryQueryServiceImpl
        implements SubtopicMasteryQueryService {

    private final SubtopicMasteryRepository subtopicMasteryRepository;

    public SubtopicMasteryQueryServiceImpl(
            SubtopicMasteryRepository subtopicMasteryRepository) {
        this.subtopicMasteryRepository = subtopicMasteryRepository;
    }

    @Override
    public StudentProgressReport handle(
            GetStudentProgressByCourseQuery query) {

        var masteries =
                subtopicMasteryRepository.findAllByHolderIdAndCourseId(
                        query.holderId(),
                        query.courseId()
                );

        return new StudentProgressReport(
                UUID.fromString(query.holderId()),
                query.courseId().value(),
                !masteries.isEmpty(),
                List.of(),
                List.of()
        );
    }
}