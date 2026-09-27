package com.kalibra.api.iam.application.acl;

import com.kalibra.api.iam.domain.model.aggregates.User;
import com.kalibra.api.iam.domain.model.queries.GetUserByEmailQuery;
import com.kalibra.api.iam.domain.model.queries.GetUserByIdQuery;
import com.kalibra.api.iam.domain.model.valueobjects.Email;
import com.kalibra.api.iam.domain.model.valueobjects.Role;
import com.kalibra.api.iam.domain.services.UserQueryService;
import com.kalibra.api.iam.interfaces.acl.IamContextFacade;
import com.kalibra.api.shared.contracts.iam.UserSummary;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class IamContextFacadeImpl implements IamContextFacade {

    private final UserQueryService userQueryService;

    public IamContextFacadeImpl(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @Override
    public Optional<UUID> fetchStudentIdByEmail(String email) {
        try {
            return userQueryService.handle(new GetUserByEmailQuery(new Email(email)))
                    .filter(user -> user.hasRole(Role.STUDENT))
                    .map(User::getId);
        } catch (IllegalArgumentException malformedEmail) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserSummary> fetchUserSummary(UUID userId) {
        return userQueryService.handle(new GetUserByIdQuery(userId))
                .map(user -> new UserSummary(user.getId(), user.getEmail().value(), roleNames(user)));
    }

    private Set<String> roleNames(User user) {
        return user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());
    }
}
