package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.queries.GetSentInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.services.InvitationQueryService;
import com.kalibra.api.enrollment.interfaces.rest.resources.CourseInvitationGroupResource;
import com.kalibra.api.enrollment.interfaces.rest.resources.InvitationResource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/course-invitation-groups")
public class CourseInvitationGroupsController {

    private final InvitationQueryService invitationQueryService;

    public CourseInvitationGroupsController(
            InvitationQueryService invitationQueryService
    ) {
        this.invitationQueryService = invitationQueryService;
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
                .map(group -> new CourseInvitationGroupResource(
                        group.courseId(),
                        group.courseName(),
                        group.courseCode(),
                        group.invitations().stream()
                                .map(invitation -> new InvitationResource(
                                        invitation.invitationId(),
                                        group.courseId(),
                                        invitation.invitedEmail(),
                                        invitation.status().name(),
                                        null,
                                        invitation.expiresAt()
                                ))
                                .toList()
                ))
                .toList();

        return ResponseEntity.ok(resources);
    }
}