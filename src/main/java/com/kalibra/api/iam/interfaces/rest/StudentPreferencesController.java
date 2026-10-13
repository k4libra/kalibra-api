package com.kalibra.api.iam.interfaces.rest;

import com.kalibra.api.iam.domain.model.aggregates.StudentPreferences;
import com.kalibra.api.iam.domain.model.queries.GetStudentPreferencesByHolderIdQuery;
import com.kalibra.api.iam.domain.services.StudentPreferencesCommandService;
import com.kalibra.api.iam.domain.services.StudentPreferencesQueryService;
import com.kalibra.api.iam.interfaces.rest.resources.StudentPreferencesResource;
import com.kalibra.api.iam.interfaces.rest.resources.UpdateStudentPreferencesResource;
import com.kalibra.api.iam.interfaces.rest.transform.StudentPreferencesAssembler;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Student Preferences", description = "Endpoints for managing student preferences")
@RequestMapping("/api/v1/student-preferences/me")
public class StudentPreferencesController {

    private final StudentPreferencesCommandService commandService;
    private final StudentPreferencesQueryService queryService;
    private final StudentPreferencesAssembler assembler;

    public StudentPreferencesController(StudentPreferencesCommandService commandService,
                                         StudentPreferencesQueryService queryService,
                                         StudentPreferencesAssembler assembler) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Get my preferences",
            description = "Returns the preferences of the authenticated user. When none are stored yet it returns "
                    + "the defaults without creating anything.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stored or default preferences"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content)
    })
    @GetMapping
    public ResponseEntity<StudentPreferencesResource> getMine(Authentication authentication) {
        var holderId = authentication.getName();
        var preferences = queryService.handle(new GetStudentPreferencesByHolderIdQuery(holderId))
                .orElseGet(() -> StudentPreferences.createDefaultFor(holderId));
        return ResponseEntity.ok(assembler.toResource(preferences));
    }

    @Operation(summary = "Update my preferences", description = "Updates the supplied fields. Reminder time is in UTC; enabling requires a supplied or stored time.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated preferences"),
            @ApiResponse(responseCode = "400", description = "Invalid preferences", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a student", content = @Content)
    })
    @PatchMapping
    public ResponseEntity<StudentPreferencesResource> update(
            @Valid @RequestBody UpdateStudentPreferencesResource resource, Authentication authentication) {
        var command = assembler.toCommand(resource, authentication.getName());
        return ResponseEntity.ok(assembler.toResource(commandService.handle(command)));
    }
}
