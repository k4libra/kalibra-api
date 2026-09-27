package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.queries.GetSentInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;
import com.kalibra.api.enrollment.domain.services.InvitationQueryService;
import com.kalibra.api.enrollment.interfaces.rest.resources.CourseInvitationGroupResource;
import com.kalibra.api.enrollment.interfaces.rest.transform.InvitationAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@Tag(name = "Invitations", description = "Course invitations sent by teachers and answered by students")
@RequestMapping("/api/v1/course-invitation-groups")
public class CourseInvitationGroupsController {

    private final InvitationQueryService queryService;
    private final InvitationAssembler assembler;

    public CourseInvitationGroupsController(InvitationQueryService queryService, InvitationAssembler assembler) {
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "List my sent invitations by course",
            description = "Returns every course of the authenticated teacher with the invitations sent to it, their status and validity. "
                    + "The optional status keeps only the invitations in that status; a course without invitations comes with an empty list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitations grouped by course"),
            @ApiResponse(responseCode = "400", description = "Unsupported status filter", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<CourseInvitationGroupResource>> getAll(
            @Parameter(description = "Keeps only the invitations in this status; all of them when omitted",
                    schema = @Schema(allowableValues = {"PENDING", "ACCEPTED", "REJECTED", "EXPIRED", "CANCELED"}))
            @RequestParam(required = false) String status,
            Authentication authentication) {
        var query = new GetSentInvitationsByHolderIdQuery(authentication.getName(), toStatus(status));
        var groups = queryService.handle(query);
        return ResponseEntity.ok(groups.stream().map(assembler::toResource).toList());
    }

    private Optional<InvitationStatus> toStatus(String status) {
        if (status == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(InvitationStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException unsupported) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported invitation status: " + status);
        }
    }
}
