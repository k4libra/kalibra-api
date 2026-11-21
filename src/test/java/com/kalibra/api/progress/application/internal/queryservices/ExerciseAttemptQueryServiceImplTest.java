package com.kalibra.api.progress.application.internal.queryservices;

import com.kalibra.api.progress.ProgressFixtures;
import com.kalibra.api.progress.domain.model.queries.GetAttemptHistoryByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.AnswerResult;
import com.kalibra.api.progress.domain.model.valueobjects.AttemptResultFilter;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.ExerciseAttemptPage;
import com.kalibra.api.progress.domain.model.valueobjects.Pagination;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.repositories.ExerciseAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExerciseAttemptQueryServiceImplTest {

    @Mock
    ExerciseAttemptRepository exerciseAttemptRepository;

    @InjectMocks
    ExerciseAttemptQueryServiceImpl service;

    private final String holderId = UUID.randomUUID().toString();
    private final CourseId courseId = new CourseId(UUID.randomUUID());
    private final SubtopicId subtopicId = new SubtopicId(UUID.randomUUID());
    private final Pagination pagination = Pagination.of(0, 20);

    @BeforeEach
    void setUp() {
        lenient().when(exerciseAttemptRepository.countByHolderIdAndCourseIdAndResult(holderId, courseId, AnswerResult.CORRECT))
                .thenReturn(2L);
        lenient().when(exerciseAttemptRepository.countByHolderIdAndCourseIdAndResult(holderId, courseId, AnswerResult.INCORRECT))
                .thenReturn(1L);
    }

    @Test
    void shouldListTheWholeHistoryWithItsCounts() {
        var correct = ProgressFixtures.attempt(holderId, courseId, subtopicId, "B", 0.30, 0.52);
        var incorrect = ProgressFixtures.attempt(holderId, courseId, subtopicId, "A", 0.52, 0.44);
        when(exerciseAttemptRepository.findAllByHolderIdAndCourseId(holderId, courseId, pagination))
                .thenReturn(new ExerciseAttemptPage(List.of(correct, incorrect), 0, 20, 3, 1));

        var view = service.handle(new GetAttemptHistoryByCourseQuery(holderId, courseId, AttemptResultFilter.ALL, pagination));

        assertThat(view.allCount()).isEqualTo(3);
        assertThat(view.correctCount()).isEqualTo(2);
        assertThat(view.incorrectCount()).isEqualTo(1);
        assertThat(view.activeFilter()).isEqualTo(AttemptResultFilter.ALL);
        assertThat(view.totalElements()).isEqualTo(3);
        assertThat(view.lines()).hasSize(2);
        assertThat(view.lines().getFirst().attemptId()).isEqualTo(correct.getId().value());
        assertThat(view.lines().getFirst().result()).isEqualTo(AnswerResult.CORRECT);
        assertThat(view.lines().getFirst().answeredAt()).isEqualTo(correct.getAnsweredAt());
        assertThat(view.lines().getFirst().statement()).isEqualTo("Solve 2x + 3 = 7");
        assertThat(view.lines().get(1).result()).isEqualTo(AnswerResult.INCORRECT);
    }

    @Test
    void shouldKeepOnlyTheAnswersOfTheChosenResultAndStillCountThemAll() {
        var incorrect = ProgressFixtures.attempt(holderId, courseId, subtopicId, "A", 0.52, 0.44);
        when(exerciseAttemptRepository.findAllByHolderIdAndCourseIdAndResult(holderId, courseId, AnswerResult.INCORRECT, pagination))
                .thenReturn(new ExerciseAttemptPage(List.of(incorrect), 0, 20, 1, 1));
        when(exerciseAttemptRepository.findAllByHolderIdAndCourseIdAndResult(holderId, courseId, AnswerResult.CORRECT, pagination))
                .thenReturn(new ExerciseAttemptPage(List.of(), 0, 20, 2, 1));

        var onlyIncorrect = service.handle(new GetAttemptHistoryByCourseQuery(holderId, courseId, AttemptResultFilter.INCORRECT, pagination));
        var onlyCorrect = service.handle(new GetAttemptHistoryByCourseQuery(holderId, courseId, AttemptResultFilter.CORRECT, pagination));

        assertThat(onlyIncorrect.activeFilter()).isEqualTo(AttemptResultFilter.INCORRECT);
        assertThat(onlyIncorrect.lines()).singleElement()
                .satisfies(line -> assertThat(line.result()).isEqualTo(AnswerResult.INCORRECT));
        assertThat(onlyIncorrect.allCount()).isEqualTo(3);
        assertThat(onlyIncorrect.totalElements()).isEqualTo(1);
        assertThat(onlyCorrect.activeFilter()).isEqualTo(AttemptResultFilter.CORRECT);
        assertThat(onlyCorrect.totalElements()).isEqualTo(2);
    }

    @Test
    void shouldAnswerAnEmptyHistoryForAStudentWithoutSolvedExercises() {
        var other = UUID.randomUUID().toString();
        when(exerciseAttemptRepository.findAllByHolderIdAndCourseId(other, courseId, pagination))
                .thenReturn(new ExerciseAttemptPage(List.of(), 0, 20, 0, 0));

        var view = service.handle(new GetAttemptHistoryByCourseQuery(other, courseId, AttemptResultFilter.ALL, pagination));

        assertThat(view.allCount()).isZero();
        assertThat(view.lines()).isEmpty();
        assertThat(view.totalPages()).isZero();
    }
}
