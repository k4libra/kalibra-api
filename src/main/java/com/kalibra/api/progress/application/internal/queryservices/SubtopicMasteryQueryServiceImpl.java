package com.kalibra.api.progress.application.internal.queryservices;

import com.kalibra.api.curriculum.interfaces.acl.CurriculumContextFacade;
import com.kalibra.api.progress.domain.model.aggregates.SubtopicMastery;
import com.kalibra.api.progress.domain.model.queries.GetPracticeSubtopicsByCourseQuery;
import com.kalibra.api.progress.domain.model.queries.GetStudentProgressByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.*;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.progress.domain.services.SubtopicMasteryQueryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SubtopicMasteryQueryServiceImpl
        implements SubtopicMasteryQueryService {

    private final SubtopicMasteryRepository subtopicMasteryRepository;
    private final CurriculumContextFacade curriculumContextFacade;

    public SubtopicMasteryQueryServiceImpl(
            SubtopicMasteryRepository subtopicMasteryRepository,
            CurriculumContextFacade curriculumContextFacade) {

        this.subtopicMasteryRepository = subtopicMasteryRepository;
        this.curriculumContextFacade = curriculumContextFacade;
    }

    @Override
    public List<PracticeSubtopicView> handle(
            GetPracticeSubtopicsByCourseQuery query) {

        return curriculumContextFacade
                .fetchSubtopics(query.courseId().value())
                .stream()
                .map(subtopic -> {

                    var mastery = subtopicMasteryRepository
                            .findByHolderIdAndSubtopicId(
                                    query.holderId(),
                                    new SubtopicId(subtopic.subtopicId())
                            );

                    var level = mastery
                            .map(SubtopicMastery::getLevel)
                            .orElse(MasteryLevel.NO_DATA);

                    return new PracticeSubtopicView(
                            subtopic.subtopicId(),
                            subtopic.name(),
                            level
                    );
                })
                .toList();
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