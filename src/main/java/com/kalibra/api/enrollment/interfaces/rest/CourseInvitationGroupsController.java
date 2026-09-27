package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.queries.GetSentInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.services.InvitationQueryService;
import com.kalibra.api.enrollment.interfaces.rest.resources.CourseInvitationGroupResource;
import com.kalibra.api.enrollment.interfaces.rest.transform.InvitationAssembler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/course-invitation-groups")
public class CourseInvitationGroupsController {

    private final InvitationQueryService invitationQueryService;
    private final InvitationAssembler invitationAssembler;

    public CourseInvitationGroupsController(
            InvitationQueryService invitationQueryService,
            InvitationAssembler invitationAssembler
    ) {
        this.invitationQueryService = invitationQueryService;
        this.invitationAssembler = invitationAssembler;
    }

    @GetMapping
    public ResponseEntity<List<CourseInvitationGroupResource>> getAll(
            Authentication authentication
    ) {
        var groups = invitationQueryService.handle(
                new GetSentInvitationsByHolderIdQuery(
                        authentication.getName()
                )
        );

        var resources = groups.stream()
                .map(invitationAssembler::toResource)
                .toList();

        return ResponseEntity.ok(resources);
    }
}