package com.kalibra.api.curriculum.interfaces.rest;

import com.kalibra.api.curriculum.domain.model.aggregates.TeacherWorkspace;
import com.kalibra.api.curriculum.domain.model.queries.GetTeacherWorkspaceByHolderIdQuery;
import com.kalibra.api.curriculum.domain.services.TeacherWorkspaceCommandService;
import com.kalibra.api.curriculum.domain.services.TeacherWorkspaceQueryService;
import com.kalibra.api.curriculum.interfaces.rest.resources.SelectActiveCourseResource;
import com.kalibra.api.curriculum.interfaces.rest.resources.TeacherWorkspaceResource;
import com.kalibra.api.curriculum.interfaces.rest.transform.TeacherWorkspaceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Teacher Workspaces", description = "Workspace state of the teacher, such as the active course")
@RequestMapping("/api/v1/teacher-workspaces/me")
public class TeacherWorkspacesController {

    private final TeacherWorkspaceCommandService commandService;
    private final TeacherWorkspaceQueryService queryService;
    private final TeacherWorkspaceAssembler assembler;

    public TeacherWorkspacesController(TeacherWorkspaceCommandService commandService,
                                       TeacherWorkspaceQueryService queryService,
                                       TeacherWorkspaceAssembler assembler) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Get my workspace",
            description = "Returns the active course of the authenticated teacher; null when none was selected. Creates nothing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workspace of the teacher"),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @GetMapping
    public ResponseEntity<TeacherWorkspaceResource> getMine(Authentication authentication) {
        var holderId = authentication.getName();
        var workspace = queryService.handle(new GetTeacherWorkspaceByHolderIdQuery(holderId))
                .orElseGet(() -> TeacherWorkspace.createFor(holderId));
        return ResponseEntity.ok(assembler.toResource(workspace));
    }

    @Operation(summary = "Change my active course",
            description = "Sets one of my courses as the active one. Creates the workspace on first use.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workspace after the change"),
            @ApiResponse(responseCode = "400", description = "Missing courseId", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "The course does not exist or belongs to another teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @PutMapping("/active-course")
    public ResponseEntity<TeacherWorkspaceResource> selectActiveCourse(@Valid @RequestBody SelectActiveCourseResource resource,
                                                                       Authentication authentication) {
        var command = assembler.toCommand(resource, authentication.getName());
        return ResponseEntity.ok(assembler.toResource(commandService.handle(command)));
    }
}
