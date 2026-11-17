package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.application.internal.outboundservices.acl.ExternalExerciseGenerationService;
import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicArchivedException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicWithoutIngestedMaterialException;
import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.DifficultyLevel;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import com.kalibra.api.curriculum.domain.repositories.GeneratedExerciseRepository;
import com.kalibra.api.curriculum.domain.services.DifficultyTargetingPolicy;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GeneratedExerciseCommandServiceImpl implements GeneratedExerciseCommandService {

    private static final Logger log = LoggerFactory.getLogger(GeneratedExerciseCommandServiceImpl.class);
    private static final int MINIMUM_UNUSED_STOCK = 3;

    private final GeneratedExerciseRepository generatedExerciseRepository;
    private final CurricularMaterialRepository curricularMaterialRepository;
    private final CourseRepository courseRepository;
    private final ExternalExerciseGenerationService externalExerciseGenerationService;
    private final GeneratedExercisePoolRefillService refillService;
    private final DifficultyTargetingPolicy difficultyTargetingPolicy = new DifficultyTargetingPolicy();

    public GeneratedExerciseCommandServiceImpl(GeneratedExerciseRepository generatedExerciseRepository,
                                               CurricularMaterialRepository curricularMaterialRepository,
                                               CourseRepository courseRepository,
                                               ExternalExerciseGenerationService externalExerciseGenerationService,
                                               GeneratedExercisePoolRefillService refillService) {
        this.generatedExerciseRepository = generatedExerciseRepository;
        this.curricularMaterialRepository = curricularMaterialRepository;
        this.courseRepository = courseRepository;
        this.externalExerciseGenerationService = externalExerciseGenerationService;
        this.refillService = refillService;
    }

    // Not @Transactional on purpose: the engine call must not hold a database transaction open;
    // saveAll persists every attempt atomically on its own.
    @Override
    public List<GeneratedExercise> handle(GenerateExercisesForSubtopicCommand command) {
        var course = courseRepository.findByIdAndHolderId(command.courseId(), command.holderId())
                .orElseThrow(() -> new CourseNotOwnedByTeacherException(command.courseId().value()));
        if (!course.hasSubtopic(command.subtopicId())) {
            throw new IllegalArgumentException("Subtopic does not belong to the course: " + command.subtopicId().value());
        }
        if (!course.hasActiveSubtopic(command.subtopicId())) {
            throw new SubtopicArchivedException();
        }
        var context = anchorOf(command.courseId(), command.subtopicId())
                .orElseThrow(() -> new SubtopicWithoutIngestedMaterialException(command.subtopicId().value()));
        var attempts = externalExerciseGenerationService.generate(command, context);
        return generatedExerciseRepository.saveAll(attempts);
    }

    @Override
    public Optional<GeneratedExercise> handle(GenerateExerciseForStudentCommand command) {
        var course = courseRepository.findById(command.courseId())
                .filter(found -> found.hasActiveSubtopic(command.subtopicId()));
        if (course.isEmpty()) {
            return Optional.empty();
        }
        var target = command.targetDifficulty()
                .orElseGet(() -> difficultyTargetingPolicy.targetFor(command.mastery().orElse(null)));
        var exercise = exerciseAt(command, target);
        if (exercise.isEmpty() && target == DifficultyLevel.HARD) {
            return exerciseAt(command, DifficultyLevel.MEDIUM);
        }
        return exercise;
    }

    private Optional<GeneratedExercise> exerciseAt(GenerateExerciseForStudentCommand command, DifficultyLevel target) {
        var targeted = new GenerateExerciseForStudentCommand(command.courseId(), command.subtopicId(), command.mastery(),
                command.excludedExerciseIds(), Optional.of(target));
        var pooled = generatedExerciseRepository.findAvailableForStudent(
                command.courseId(), command.subtopicId(), target, command.excludedExerciseIds());
        if (pooled.isPresent()) {
            refillIfLow(targeted, pooled.get());
            return pooled;
        }
        var context = anchorOf(command.courseId(), command.subtopicId())
                .orElseThrow(() -> new SubtopicWithoutIngestedMaterialException(command.subtopicId().value()));
        var attempts = externalExerciseGenerationService.generate(targeted, context);
        return generatedExerciseRepository.saveAll(attempts).stream()
                .filter(GeneratedExercise::isAvailableToStudents)
                .filter(exercise -> !command.excludedExerciseIds().contains(exercise.getId()))
                .findFirst();
    }

    private void refillIfLow(GenerateExerciseForStudentCommand command, GeneratedExercise delivered) {
        try {
            var excluded = new HashSet<>(command.excludedExerciseIds());
            excluded.add(delivered.getId());
            var remaining = generatedExerciseRepository.countAvailableForStudent(command.courseId(), command.subtopicId(),
                    command.targetDifficulty().orElseThrow(), excluded);
            if (remaining < MINIMUM_UNUSED_STOCK) {
                anchorOf(command.courseId(), command.subtopicId()).ifPresent(context -> refillService.request(command, context));
            }
        } catch (Exception failure) {
            log.warn("Exercise pool stock check or refill failed for course {}, subtopic {}",
                    command.courseId().value(), command.subtopicId().value(), failure);
        }
    }

    private Optional<CurricularContent> anchorOf(CourseId courseId, SubtopicId subtopicId) {
        var contents = curricularMaterialRepository
                .findAllByCourseIdAndSubtopicIdAndStatus(courseId, subtopicId, IngestionStatus.READY).stream()
                .map(CurricularMaterial::getContent)
                .flatMap(Optional::stream)
                .toList();
        if (contents.isEmpty()) {
            return Optional.empty();
        }
        var text = contents.stream().map(CurricularContent::normalizedText).collect(Collectors.joining("\n\n"));
        var pages = contents.stream().mapToInt(CurricularContent::pageCount).sum();
        return Optional.of(new CurricularContent(text, pages));
    }
}
