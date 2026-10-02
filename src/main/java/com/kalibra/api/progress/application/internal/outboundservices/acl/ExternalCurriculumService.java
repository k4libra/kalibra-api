package com.kalibra.api.progress.application.internal.outboundservices.acl;

import com.kalibra.api.curriculum.interfaces.acl.CurriculumContextFacade;
import com.kalibra.api.progress.domain.model.commands.RequestPracticeExerciseCommand;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerKey;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.progress.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.progress.domain.model.valueobjects.PracticeExerciseView;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.shared.contracts.curriculum.PracticeExerciseRequest;
import com.kalibra.api.shared.contracts.curriculum.SubtopicSummary;
import com.kalibra.api.shared.contracts.curriculum.SubtopicVerificationStats;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service("progressExternalCurriculumService")
public class ExternalCurriculumService {

    private final CurriculumContextFacade curriculumContextFacade;

    public ExternalCurriculumService(
            CurriculumContextFacade curriculumContextFacade) {
        this.curriculumContextFacade = curriculumContextFacade;
    }

    public Optional<AnswerKey> fetchAnswerKey(ExerciseId exerciseId) {
        return curriculumContextFacade
                .fetchAnswerKey(exerciseId.value())
                .map(key -> new AnswerKey(
                        key.statement(),
                        key.correctOptionKey(),
                        key.explanation(),
                        new SubtopicId(key.subtopicId())
                ));
    }

    public Optional<PracticeExerciseView> provideExercise(
            RequestPracticeExerciseCommand command,
            Optional<MasteryProbability> mastery) {
        var request = new PracticeExerciseRequest(
                command.courseId().value(),
                command.subtopicId().value(),
                mastery.map(MasteryProbability::value)
        );
        return curriculumContextFacade
                .provideExerciseForStudent(request)
                .map(snapshot -> new PracticeExerciseView(
                        snapshot.exerciseId(),
                        snapshot.subtopicId(),
                        snapshot.statement(),
                        snapshot.options()
                ));
    }

    public boolean isCourseOwnedBy(CourseId courseId, String holderId) {
        return curriculumContextFacade.isCourseOwnedBy(
                courseId.value(),
                holderId
        );
    }

    public List<SubtopicSummary> fetchSubtopics(CourseId courseId) {
        return curriculumContextFacade.fetchSubtopics(courseId.value());
    }

    public List<SubtopicVerificationStats> fetchVerificationStats(CourseId courseId) {
        return curriculumContextFacade.fetchVerificationStats(courseId.value());
    }
}
