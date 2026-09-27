package com.kalibra.api.enrollment.interfaces.rest;

import com.kalibra.api.enrollment.domain.model.queries.GetSentInvitationsByHolderIdQuery;
import com.kalibra.api.enrollment.domain.services.InvitationQueryService;
import com.kalibra.api.enrollment.interfaces.rest.resources.CourseInvitationGroupResource;
import com.kalibra.api.enrollment.interfaces.rest.transform.InvitationAssembler;
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
            description = "Returns every course of the authenticated teacher with the invitations sent to it, their status and validity. A course without invitations comes with an empty list.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invitations grouped by course"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT cookie", content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated user is not a teacher", content = @Content)
    })
    @GetMapping
    public ResponseEntity<List<CourseInvitationGroupResource>> getAll(Authentication authentication) {
        var groups = queryService.handle(new GetSentInvitationsByHolderIdQuery(authentication.getName()));
        return ResponseEntity.ok(groups.stream().map(assembler::toResource).toList());
    }
}
