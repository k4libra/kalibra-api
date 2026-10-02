package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.model.commands.RequestPracticeExerciseCommand;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.model.valueobjects.SubtopicId;
import com.kalibra.api.progress.domain.services.ExerciseAttemptCommandService;
import com.kalibra.api.progress.interfaces.rest.resources.PracticeExerciseResource;
import com.kalibra.api.progress.interfaces.rest.resources.RequestPracticeExerciseResource;
import com.kalibra.api.progress.interfaces.rest.transform.ExerciseAttemptAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/practice-exercises")
public class PracticeExercisesController {

    private final ExerciseAttemptCommandService exerciseAttemptCommandService;
    private final ExerciseAttemptAssembler exerciseAttemptAssembler;

    public PracticeExercisesController(
            ExerciseAttemptCommandService exerciseAttemptCommandService,
            ExerciseAttemptAssembler exerciseAttemptAssembler) {
        this.exerciseAttemptCommandService = exerciseAttemptCommandService;
        this.exerciseAttemptAssembler = exerciseAttemptAssembler;
    }

    @PostMapping
    public ResponseEntity<PracticeExerciseResource> request(
            @RequestBody RequestPracticeExerciseResource resource,
            Authentication authentication) {

        var command = new RequestPracticeExerciseCommand(
                authentication.getName(),
                new CourseId(resource.courseId()),
                new SubtopicId(resource.subtopicId())
        );

        return exerciseAttemptCommandService.handle(command)
                .map(exerciseAttemptAssembler::toResource)
                .map(result -> ResponseEntity
                        .created(URI.create(
                                "/api/v1/practice-exercises/" + result.exerciseId()))
                        .body(result))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}