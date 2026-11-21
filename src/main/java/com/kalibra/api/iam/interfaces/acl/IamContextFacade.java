package com.kalibra.api.iam.interfaces.acl;

import com.kalibra.api.shared.contracts.iam.UserSummary;

import java.util.Optional;
import java.util.UUID;

public interface IamContextFacade {

    Optional<UUID> fetchStudentIdByEmail(String email);

    Optional<UserSummary> fetchUserSummary(UUID userId);
}
