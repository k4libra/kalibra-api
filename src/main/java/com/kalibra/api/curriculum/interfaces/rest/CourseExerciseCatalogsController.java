package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.model.queries.GetExerciseCatalogByHolderIdQuery;
import com.kalibra.api.curriculum.domain.services.GeneratedExerciseQueryService;
import com.kalibra.api.curriculum.interfaces.rest.resources.CourseExerciseCatalogResource;
import com.kalibra.api.curriculum.interfaces.rest.transform.GeneratedExerciseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Generated Exercises", description = "Exercises generated from the curricular material and their verification result")
@RequestMapping("/api/v1/course-exercise-catalogs")
public class CourseExerciseCatalogsController {

    private final GeneratedExerciseQueryService queryService;
    private final GeneratedExerciseAssembler assembler;

    public CourseExerciseCatalogsController(GeneratedExerciseQueryService queryService, GeneratedExerciseAssembler assembler) {
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "List my generated exercises by course and subtopic",
            description = "Returns every course of the authenticated teacher with, per subtopic, how many exercises were generated, approved and discarded. "
                    + "A course without generated exercises comes with its subtopics at zero.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exercise counts grouped by course and subtopic"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<CourseExerciseCatalogResource>> getAll(Authentication authentication) {
        var catalogs = queryService.handle(new GetExerciseCatalogByHolderIdQuery(authentication.getName()));
        return ResponseEntity.ok(catalogs.stream().map(assembler::toResource).toList());
    }
}
