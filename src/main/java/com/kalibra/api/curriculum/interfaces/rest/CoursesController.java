package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.exceptions.CourseNotFoundException;
import com.kalibra.api.curriculum.domain.model.queries.GetCourseByIdQuery;
import com.kalibra.api.curriculum.domain.model.queries.GetCoursesByHolderIdQuery;
import com.kalibra.api.curriculum.domain.model.valueobjects.CourseId;
import com.kalibra.api.curriculum.domain.services.CourseCommandService;
import com.kalibra.api.curriculum.domain.services.CourseQueryService;
import com.kalibra.api.curriculum.interfaces.rest.resources.CourseResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.CreateCourseResource;
import com.kalibra.api.curriculum.interfaces.rest.transform.CourseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Courses", description = "Courses and their subtopics, owned by the teacher who creates them")
@RequestMapping("/api/v1/courses")
public class CoursesController {

    private final CourseCommandService commandService;
    private final CourseQueryService queryService;
    private final CourseAssembler assembler;

    public CoursesController(CourseCommandService commandService,
                             CourseQueryService queryService,
                             CourseAssembler assembler) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Create a course",
            description = "Creates a course owned by the authenticated teacher, with its subtopics in the given order.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Course created"),
            @ApiResponse(responseCode = "400", description = "Invalid name, code or subtopic name", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "The course has no subtopics", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CourseResource> create(@Valid @RequestBody CreateCourseResource resource,
                                                 Authentication authentication) {
        var course = commandService.handle(assembler.toCommand(resource, authentication.getName()));
        return new ResponseEntity<>(assembler.toResource(course), HttpStatus.CREATED);
    }

    @Operation(summary = "List my courses", description = "Returns only the courses created by the authenticated teacher.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Courses of the teacher"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<CourseResource>> getAll(Authentication authentication) {
        var courses = queryService.handle(new GetCoursesByHolderIdQuery(authentication.getName()));
        return ResponseEntity.ok(courses.stream().map(assembler::toResource).toList());
    }

    @Operation(summary = "Get one of my courses", description = "Returns the course with its subtopics when it belongs to the authenticated teacher.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Course with its subtopics"),
            @ApiResponse(responseCode = "404", description = "The course does not exist or belongs to another teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<CourseResource> getById(@PathVariable UUID id, Authentication authentication) {
        var course = queryService.handle(new GetCourseByIdQuery(new CourseId(id)))
                .filter(found -> found.isOwnedBy(authentication.getName()))
                .orElseThrow(() -> new CourseNotFoundException(id));
        return ResponseEntity.ok(assembler.toResource(course));
    }
}
