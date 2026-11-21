package com.kalibra.api.curriculum.application.internal.commandservices;

import com.kalibra.api.curriculum.application.internal.outboundservices.acl.ExternalExerciseGenerationService;
import com.kalibra.api.curriculum.domain.model.commands.GenerateExerciseForStudentCommand;
import com.kalibra.api.curriculum.domain.model.valueobjects.CurricularContent;
import com.kalibra.api.curriculum.domain.repositories.GeneratedExerciseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class GeneratedExercisePoolRefillTask {

    private static final Logger log = LoggerFactory.getLogger(GeneratedExercisePoolRefillTask.class);

    private final ExternalExerciseGenerationService generationService;
    private final GeneratedExerciseRepository repository;

    public GeneratedExercisePoolRefillTask(ExternalExerciseGenerationService generationService,
                                           GeneratedExerciseRepository repository) {
        this.generationService = generationService;
        this.repository = repository;
    }

    @Async("exercisePoolRefillExecutor")
    public CompletableFuture<Void> refill(GenerateExerciseForStudentCommand command, CurricularContent context) {
        try {
            var attempts = generationService.generate(command, context);
            repository.saveAll(attempts);
        } catch (Exception failure) {
            log.warn("Exercise pool refill failed for course {}, subtopic {}, difficulty {}",
                    command.courseId().value(), command.subtopicId().value(), command.targetDifficulty().orElseThrow(), failure);
        }
        return CompletableFuture.completedFuture(null);
    }
}
