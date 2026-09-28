package com.kalibra.api.progress.application.internal.queryservices;

import com.kalibra.api.curriculum.interfaces.acl.CurriculumContextFacade;
import com.kalibra.api.progress.domain.model.queries.GetPracticeSubtopicsByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryLevel;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeSubtopicView;
import com.kalibra.api.progress.domain.repositories.SubtopicMasteryRepository;
import com.kalibra.api.progress.domain.services.ExerciseAttemptQueryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExerciseAttemptQueryServiceImpl
        implements ExerciseAttemptQueryService {

    private final CurriculumContextFacade curriculumContextFacade;
    private final SubtopicMasteryRepository subtopicMasteryRepository;

    public ExerciseAttemptQueryServiceImpl(
            CurriculumContextFacade curriculumContextFacade,
            SubtopicMasteryRepository subtopicMasteryRepository) {
        this.curriculumContextFacade = curriculumContextFacade;
        this.subtopicMasteryRepository = subtopicMasteryRepository;
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
                                    new com.kalibra.api.progress.domain.model.valueobjects.SubtopicId(
                                            subtopic.subtopicId()
                                    )
                            );

                    var level = mastery
                            .map(value -> value.getLevel())
                            .orElse(MasteryLevel.NO_DATA);

                    return new PracticeSubtopicView(
                            subtopic.subtopicId(),
                            subtopic.name(),
                            level
                    );
                })
                .toList();
    }
}