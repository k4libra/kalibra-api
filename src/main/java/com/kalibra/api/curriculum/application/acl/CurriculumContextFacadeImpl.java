package com.kalibra.api.curriculum.application.acl;

import com.kalibra.api.curriculum.domain.exceptions.SubtopicWithoutIngestedMaterialException;
import com.kalibra.api.curriculum.domain.model.aggregates.Course;
import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.entities.ExerciseOption;
import com.kalibra.api.curriculum.domain.model.queries.GetCourseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCoursesByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetGeneratedExerciseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetVerificationApprovalByCourseQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.ExerciseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.MasteryProbability;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.services.CourseQueryService;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseCommandService;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseQueryService;
import com.kalibra.api.curriculum.interfaces.acl.CurriculumContextFacade;
import com.kalibra.api.shared.contracts.curriculum.CourseSummary;
import com.kalibra.api.shared.contracts.curriculum.ExerciseAnswerKey;
import com.kalibra.api.shared.contracts.curriculum.ExerciseSnapshot;
import com.kalibra.api.shared.contracts.curriculum.PracticeExerciseRequest;
import com.kalibra.api.shared.contracts.curriculum.SubtopicSummary;
import com.kalibra.api.shared.contracts.curriculum.SubtopicVerificationStats;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CurriculumContextFacadeImpl implements CurriculumContextFacade {

    private final CourseQueryService courseQueryService;
    private final GeneratedExerciseCommandService generatedExerciseCommandService;
    private final GeneratedExerciseQueryService generatedExerciseQueryService;

    public CurriculumContextFacadeImpl(CourseQueryService courseQueryService,
                                       GeneratedExerciseCommandService generatedExerciseCommandService,
                                       GeneratedExerciseQueryService generatedExerciseQueryService) {
        this.courseQueryService = courseQueryService;
        this.generatedExerciseCommandService = generatedExerciseCommandService;
        this.generatedExerciseQueryService = generatedExerciseQueryService;
    }

    @Override
    public boolean isCourseOwnedBy(UUID courseId, String holderId) {
        return findCourse(courseId).map(course -> course.isOwnedBy(holderId)).orElse(false);
    }

    @Override
    public Optional<CourseSummary> fetchCourseSummary(UUID courseId) {
        return findCourse(courseId).map(this::toSummary);
    }

    @Override
    public List<CourseSummary> fetchCoursesByHolderId(String holderId) {
        return courseQueryService.handle(new GetCoursesByHolderIdQuery(holderId)).stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public List<SubtopicSummary> fetchSubtopics(UUID courseId) {
        return findCourse(courseId)
                .map(course -> course.getSubtopics().stream()
                        .map(subtopic -> new SubtopicSummary(
                                subtopic.getId().value(), subtopic.getName(), subtopic.getDisplayOrder()))
                        .toList())
                .orElse(List.of());
    }

    @Override
    public Optional<ExerciseSnapshot> provideExerciseForStudent(PracticeExerciseRequest request) {
        if (request == null || request.courseId() == null || request.subtopicId() == null) {
            return Optional.empty();
        }
        var mastery = request.masteryProbability() == null
                ? Optional.<MasteryProbability>empty()
                : request.masteryProbability().map(MasteryProbability::new);
        var command = new GenerateExerciseForStudentCommand(
                new CourseId(request.courseId()), new SubtopicId(request.subtopicId()), mastery);
        try {
            return generatedExerciseCommandService.handle(command)
                    .filter(GeneratedExercise::isAvailableToStudents)
                    .map(this::toSnapshot);
        } catch (SubtopicWithoutIngestedMaterialException withoutAnchor) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ExerciseAnswerKey> fetchAnswerKey(UUID exerciseId) {
        if (exerciseId == null) {
            return Optional.empty();
        }
        return generatedExerciseQueryService.handle(new GetGeneratedExerciseByIdQuery(new ExerciseId(exerciseId)))
                .filter(GeneratedExercise::isAvailableToStudents)
                .map(exercise -> new ExerciseAnswerKey(exercise.getId().value(), exercise.getSubtopicId().value(),
                        exercise.getStatement(), exercise.correctOption().getKey(), exercise.getExplanation()));
    }

    @Override
    public List<SubtopicVerificationStats> fetchVerificationStats(UUID courseId) {
        if (courseId == null) {
            return List.of();
        }
        return generatedExerciseQueryService.handle(new GetVerificationApprovalByCourseQuery(new CourseId(courseId)))
                .bySubtopic().stream()
                .map(count -> new SubtopicVerificationStats(
                        count.subtopicId(), count.subtopicName(), count.generated(), count.approved()))
                .toList();
    }

    private Optional<Course> findCourse(UUID courseId) {
        if (courseId == null) {
            return Optional.empty();
        }
        return courseQueryService.handle(new GetCourseByIdQuery(new CourseId(courseId)));
    }

    private CourseSummary toSummary(Course course) {
        return new CourseSummary(course.getId().value(), course.getName(), course.getCode().value(), course.getHolderId());
    }

    private ExerciseSnapshot toSnapshot(GeneratedExercise exercise) {
        return new ExerciseSnapshot(exercise.getId().value(), exercise.getSubtopicId().value(), exercise.getStatement(),
                exercise.getOptions().stream()
                        .sorted(Comparator.comparing(ExerciseOption::getKey))
                        .map(ExerciseOption::getText)
                        .toList());
    }
}
