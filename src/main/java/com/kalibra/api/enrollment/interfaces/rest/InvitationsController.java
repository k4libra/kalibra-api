package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.commands.AcceptInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.CancelInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.RejectInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.ResendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.commands.SendInvitationCommand;
import com.kalibra.api.enrollment.domain.model.queries.GetPendingInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseId;
import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.InvitationId;
import com.kalibra.api.enrollment.domain.services.InvitationCommandService;
import com.kalibra.api.enrollment.domain.services.InvitationQueryService;
import com.kalibra.api.enrollment.interfaces.rest.resources.InvitationResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.PendingInvitationResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.SendInvitationResource;
import com.kalibra.api.enrollment.interfaces.rest.transform.InvitationAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invitations")
public class InvitationsController {

    private final InvitationCommandService invitationCommandService;
    private final InvitationQueryService invitationQueryService;
    private final InvitationAssembler invitationAssembler;

    public InvitationsController(
            InvitationCommandService invitationCommandService,
            InvitationQueryService invitationQueryService,
            InvitationAssembler invitationAssembler
    ) {
        this.invitationCommandService = invitationCommandService;
        this.invitationQueryService = invitationQueryService;
        this.invitationAssembler = invitationAssembler;
    }

    @PostMapping
    public ResponseEntity<InvitationResource> send(
            @RequestBody SendInvitationResource resource,
            Authentication authentication
    ) {
        var invitation = invitationCommandService.handle(
                new SendInvitationCommand(
                        authentication.getName(),
                        new CourseId(resource.courseId()),
                        new Email(resource.studentEmail())
                )
        );

        return ResponseEntity
                .created(URI.create(
                        "/api/v1/invitations/" + invitation.getId().value()
                ))
                .body(invitationAssembler.toResource(invitation));
    }

    @GetMapping
    public ResponseEntity<List<PendingInvitationResource>> getAll(
            @RequestParam String status,
            Authentication authentication
    ) {
        var invitations = invitationQueryService.handle(
                new GetPendingInvitationsByHolderIdQuery(
                        authentication.getName()
                )
        );

        var resources = invitations.stream()
                .map(invitation -> new PendingInvitationResource(
                        invitation.invitationId(),
                        invitation.courseName(),
                        invitation.teacherEmail(),
                        invitation.sentAt(),
                        invitation.expiresAt()
                ))
                .toList();

        return ResponseEntity.ok(resources);
    }

    @PostMapping("/{id}/cancellations")
    public ResponseEntity<InvitationResource> cancel(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        var invitation = invitationCommandService.handle(
                new CancelInvitationCommand(
                        authentication.getName(),
                        new InvitationId(id)
                )
        );

        return ResponseEntity
                .created(URI.create(
                        "/api/v1/invitations/" + invitation.getId().value()
                ))
                .body(invitationAssembler.toResource(invitation));
    }

    @PostMapping("/{id}/renewals")
    public ResponseEntity<InvitationResource> renew(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        var invitation = invitationCommandService.handle(
                new ResendInvitationCommand(
                        authentication.getName(),
                        new InvitationId(id)
                )
        );

        return ResponseEntity
                .created(URI.create(
                        "/api/v1/invitations/" + invitation.getId().value()
                ))
                .body(invitationAssembler.toResource(invitation));
    }

    @PostMapping("/{id}/acceptances")
    public ResponseEntity<InvitationResource> accept(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        var invitation = invitationCommandService.handle(
                new AcceptInvitationCommand(
                        authentication.getName(),
                        new InvitationId(id)
                )
        );

        return ResponseEntity
                .created(URI.create(
                        "/api/v1/invitations/" + invitation.getId().value()
                ))
                .body(invitationAssembler.toResource(invitation));
    }

    @PostMapping("/{id}/rejections")
    public ResponseEntity<InvitationResource> reject(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        var invitation = invitationCommandService.handle(
                new RejectInvitationCommand(
                        authentication.getName(),
                        new InvitationId(id)
                )
        );

        return ResponseEntity
                .created(URI.create(
                        "/api/v1/invitations/" + invitation.getId().value()
                ))
                .body(invitationAssembler.toResource(invitation));
    }
}