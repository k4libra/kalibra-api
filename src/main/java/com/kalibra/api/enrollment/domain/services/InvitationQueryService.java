package com.kalibra.api.enrollment.domain.services;

import com.kalibra.api.enrollment.domain.model.aggregates.Invitation;
import com.kalibra.api.enrollment.domain.model.queries.*;
import com.kalibra.api.enrollment.domain.model.valueobjects.CourseInvitationsGroup;
import com.kalibra.api.enrollment.domain.model.valueobjects.PendingInvitationView;

import java.util.List;
import java.util.Optional;

public interface InvitationQueryService {

    List<PendingInvitationView> handle(
            GetPendingInvitationsByHolderIdQuery query
    );

    List<CourseInvitationsGroup> handle(
            GetSentInvitationsByHolderIdQuery query
    );

    Optional<Invitation> handle(
            GetAcceptedInvitationByStudentAndCourseQuery query
    );
}