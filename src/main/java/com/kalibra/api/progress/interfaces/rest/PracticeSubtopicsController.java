package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.model.queries.GetPracticeSubtopicsByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.interfaces.rest.resources.PracticeSubtopicResource;
import com.kalibra.api.progress.interfaces.rest.transform.PracticeSubtopicAssembler;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/practice-subtopics")
public class PracticeSubtopicsController {

    private final ExerciseAttemptQueryService queryService;
    private final PracticeSubtopicAssembler assembler;

    public PracticeSubtopicsController(
            ExerciseAttemptQueryService queryService,
            PracticeSubtopicAssembler assembler) {
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @GetMapping
    public List<PracticeSubtopicResource> getAll(
            @PathVariable UUID courseId,
            Authentication authentication) {

        var query = new GetPracticeSubtopicsByCourseQuery(
                authentication.getName(),
                new CourseId(courseId)
        );

        return queryService.handle(query)
                .stream()
                .map(assembler::toResource)
                .toList();
    }
}