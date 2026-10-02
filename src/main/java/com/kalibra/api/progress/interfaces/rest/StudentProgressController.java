package com.kalibra.api.progress.interfaces.rest;

import com.kalibra.api.progress.domain.model.queries.GetStudentProgressByCourseQuery;
import com.kalibra.api.progress.domain.model.valueobjects.CourseId;
import com.kalibra.api.progress.domain.services.SubtopicMasteryQueryService;
import com.kalibra.api.progress.interfaces.rest.resources.StudentProgressResource;
import com.kalibra.api.progress.interfaces.rest.transform.StudentProgressAssembler;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/progress")
public class StudentProgressController {

    private final SubtopicMasteryQueryService queryService;
    private final StudentProgressAssembler assembler;

    public StudentProgressController(
            SubtopicMasteryQueryService queryService,
            StudentProgressAssembler assembler) {
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @GetMapping
    public StudentProgressResource getProgress(
            @PathVariable UUID courseId,
            Authentication authentication) {

        var query = new GetStudentProgressByCourseQuery(
                authentication.getName(),
                new CourseId(courseId)
        );

        return assembler.toResource(
                queryService.handle(query)
        );
    }
}