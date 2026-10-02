package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.application.internal.outboundservices.acl.ExternalExerciseGenerationService;
import com.kalibra.api.curriculum.domain.exceptions.CourseNotOwnedByTeacherException;
import com.kalibra.api.curriculum.domain.exceptions.SubtopicWithoutIngestedMaterialException;
import com.kalibra.api.curriculum.domain.model.aggregates.CurricularMaterial;
import com.kalibra.api.curriculum.domain.model.aggregates.GeneratedExercise;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExercisesForSubtopicCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.model.valueobjects.IngestionStatus;
import com.kalibra.api.curriculum.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.curriculum.domain.repositories.CourseRepository;
import com.kalibra.api.curriculum.domain.repositories.CurricularMaterialRepository;
import com.kalibra.api.curriculum.domain.repositories.GeneratedExerciseRepository;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseCommandService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GeneratedExerciseCommandServiceImpl implements GeneratedExerciseCommandService {

    private final GeneratedExerciseRepository generatedExerciseRepository;
    private final CurricularMaterialRepository curricularMaterialRepository;
    private final CourseRepository courseRepository;
    private final ExternalExerciseGenerationService externalExerciseGenerationService;

    public GeneratedExerciseCommandServiceImpl(GeneratedExerciseRepository generatedExerciseRepository,
                                               CurricularMaterialRepository curricularMaterialRepository,
                                               CourseRepository courseRepository,
                                               ExternalExerciseGenerationService externalExerciseGenerationService) {
        this.generatedExerciseRepository = generatedExerciseRepository;
        this.curricularMaterialRepository = curricularMaterialRepository;
        this.courseRepository = courseRepository;
        this.externalExerciseGenerationService = externalExerciseGenerationService;
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
        var context = anchorOf(command.courseId(), command.subtopicId())
                .orElseThrow(() -> new SubtopicWithoutIngestedMaterialException(command.subtopicId().value()));
        var attempts = externalExerciseGenerationService.generate(command, context);
        return generatedExerciseRepository.saveAll(attempts);
    }

    @Override
    public Optional<GeneratedExercise> handle(GenerateExerciseForStudentCommand command) {
        var course = courseRepository.findById(command.courseId())
                .filter(found -> found.hasSubtopic(command.subtopicId()));
        if (course.isEmpty()) {
            return Optional.empty();
        }
        var context = anchorOf(command.courseId(), command.subtopicId())
                .orElseThrow(() -> new SubtopicWithoutIngestedMaterialException(command.subtopicId().value()));
        var attempts = externalExerciseGenerationService.generate(command, context);
        return generatedExerciseRepository.saveAll(attempts).stream()
                .filter(GeneratedExercise::isAvailableToStudents)
                .findFirst();
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
