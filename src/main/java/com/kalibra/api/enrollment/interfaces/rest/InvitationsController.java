package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.commands.AcceptInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.CancelInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.RejectInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.ResendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.queries.GetPendingInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationStatus;
import com.kalibra.api.enrollment.domain.services.InvitationCommandService;
import com.kalibra.api.enrollment.domain.services.InvitationQueryService;
import com.kalibra.api.enrollment.interfaces.rest.resources.InvitationResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.PendingInvitationResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.SendInvitationResource;
import com.kalibra.api.enrollment.interfaces.rest.transform.InvitationAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Invitations", description = "Course invitations sent by teachers and answered by students")
@RequestMapping("/api/v1/invitations")
public class InvitationsController {

    private final InvitationCommandService commandService;
    private final InvitationQueryService queryService;
    private final InvitationAssembler assembler;

    public InvitationsController(InvitationCommandService commandService,
                                 InvitationQueryService queryService,
                                 InvitationAssembler assembler) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.assembler = assembler;
    }

    @Operation(summary = "Invite a student to my course",
            description = "Invites a registered student, by email, to a course of the authenticated teacher. The invitation stays pending for 3 days.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitation sent"),
            @ApiResponse(responseCode = "400", description = "Missing course or email", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "The course does not exist or belongs to another teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The student already has a pending invitation or is already enrolled", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "The email does not belong to a registered student", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @PostMapping
    public ResponseEntity<InvitationResource> send(@Valid @RequestBody SendInvitationResource resource,
                                                   Authentication authentication) {
        var invitation = commandService.handle(assembler.toCommand(resource, authentication.getName()));
        return created(invitation);
    }

    @Operation(summary = "List my pending invitations",
            description = "Returns the invitations the authenticated student can still accept or reject, with the course and the teacher who sent them.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pending invitations of the student"),
            @ApiResponse(responseCode = "400", description = "Unsupported status filter", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a student", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<PendingInvitationResource>> getAll(
            @Parameter(description = "Only PENDING is supported")
            @RequestParam(defaultValue = "PENDING") String status,
            Authentication authentication) {
        if (!InvitationStatus.PENDING.name().equalsIgnoreCase(status)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending invitations can be listed");
        }
        var invitations = queryService.handle(new GetPendingInvitationsByHolderIdQuery(authentication.getName()));
        return ResponseEntity.ok(invitations.stream().map(assembler::toResource).toList());
    }

    @Operation(summary = "Cancel an invitation", description = "Cancels a pending invitation sent by the authenticated teacher.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitation canceled"),
            @ApiResponse(responseCode = "404", description = "The invitation does not exist or was sent by another teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The invitation is no longer pending", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @PostMapping("/{id}/cancellations")
    public ResponseEntity<InvitationResource> cancel(@PathVariable UUID id, Authentication authentication) {
        return created(commandService.handle(new CancelInvitationCommand(authentication.getName(), new InvitationId(id))));
    }

    @Operation(summary = "Resend an invitation",
            description = "Sends again a canceled or expired invitation of the authenticated teacher: it is pending again for 3 more days.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitation sent again"),
            @ApiResponse(responseCode = "404", description = "The invitation does not exist or was sent by another teacher", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The invitation is neither canceled nor expired, or the student already has a pending invitation or is enrolled", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @PostMapping("/{id}/renewals")
    public ResponseEntity<InvitationResource> renew(@PathVariable UUID id, Authentication authentication) {
        return created(commandService.handle(new ResendInvitationCommand(authentication.getName(), new InvitationId(id))));
    }

    @Operation(summary = "Accept an invitation", description = "Accepts a pending invitation of the authenticated student, who is then enrolled in the course.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitation accepted"),
            @ApiResponse(responseCode = "404", description = "The invitation does not exist or is addressed to another student", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The invitation is no longer pending or has expired", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a student", content = @Content)
    })
    @PostMapping("/{id}/acceptances")
    public ResponseEntity<InvitationResource> accept(@PathVariable UUID id, Authentication authentication) {
        return created(commandService.handle(new AcceptInvitationCommand(authentication.getName(), new InvitationId(id))));
    }

    @Operation(summary = "Reject an invitation", description = "Rejects a pending invitation of the authenticated student, without enrolling them.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Invitation rejected"),
            @ApiResponse(responseCode = "404", description = "The invitation does not exist or is addressed to another student", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The invitation is no longer pending or has expired", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a student", content = @Content)
    })
    @PostMapping("/{id}/rejections")
    public ResponseEntity<InvitationResource> reject(@PathVariable UUID id, Authentication authentication) {
        return created(commandService.handle(new RejectInvitationCommand(authentication.getName(), new InvitationId(id))));
    }

    private ResponseEntity<InvitationResource> created(Invitation invitation) {
        return ResponseEntity.created(URI.create("/api/v1/invitations/" + invitation.getId().value()))
                .body(assembler.toResource(invitation));
    }
}
