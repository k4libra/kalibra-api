package com.kalibra.api.progress.application.internal.queryservices;

import com.kalibra.api.progress.domain.model.aggregates.ExerciseAttempt;
import com.kalibra.api.progress.domain.model.queries.GetAttemptHistoryByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptHistoryLine;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptHistoryView;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseAttemptPage;
import com.kalibra.api.progress.domain.repositories.ExerciseAttemptRepository;
import com.kalibra.api.progress.domain.services.ExerciseAttemptQueryService;
import org.springframework.stereotype.Service;

@Service
public class ExerciseAttemptQueryServiceImpl
        implements ExerciseAttemptQueryService {

    private final ExerciseAttemptRepository exerciseAttemptRepository;

    public ExerciseAttemptQueryServiceImpl(
            ExerciseAttemptRepository exerciseAttemptRepository) {
        this.exerciseAttemptRepository = exerciseAttemptRepository;
    }

    @Override
    public AttemptHistoryView handle(GetAttemptHistoryByCourseQuery query) {
        var correct = (int) exerciseAttemptRepository.countByHolderIdAndCourseIdAndResult(
                query.holderId(), query.courseId(), AnswerResult.CORRECT);
        var incorrect = (int) exerciseAttemptRepository.countByHolderIdAndCourseIdAndResult(
                query.holderId(), query.courseId(), AnswerResult.INCORRECT);
        var page = pageOf(query);
        var lines = page.items()
                .stream()
                .map(this::toLine)
                .toList();
        return new AttemptHistoryView(
                correct + incorrect,
                correct,
                incorrect,
                query.filter(),
                lines,
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages()
        );
    }

    private ExerciseAttemptPage pageOf(GetAttemptHistoryByCourseQuery query) {
        return switch (query.filter()) {
            case ALL -> exerciseAttemptRepository.findAllByHolderIdAndCourseId(
                    query.holderId(), query.courseId(), query.pagination());
            case CORRECT -> exerciseAttemptRepository.findAllByHolderIdAndCourseIdAndResult(
                    query.holderId(), query.courseId(), AnswerResult.CORRECT, query.pagination());
            case INCORRECT -> exerciseAttemptRepository.findAllByHolderIdAndCourseIdAndResult(
                    query.holderId(), query.courseId(), AnswerResult.INCORRECT, query.pagination());
        };
    }

    private AttemptHistoryLine toLine(ExerciseAttempt attempt) {
        return new AttemptHistoryLine(
                attempt.getId().value(),
                attempt.getSubtopicId().value(),
                attempt.getExerciseStatement(),
                attempt.getResult(),
                attempt.getFeedback(),
                attempt.getMasteryChange(),
                attempt.getAnsweredAt()
        );
    }
}
