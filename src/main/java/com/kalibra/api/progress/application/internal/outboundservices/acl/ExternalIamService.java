package com.kalibra.api.progress.application.internal.outboundservices.acl;

import com.kalibra.api.iam.interfaces.acl.IamContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service("progressExternalIamService")
public class ExternalIamService {

    private final IamContextFacade iamContextFacade;

    public ExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    public Optional<String> fetchFullName(String holderId) {
        try {
            return iamContextFacade.fetchUserSummary(UUID.fromString(holderId))
                    .map(user -> (user.firstName() + " " + user.lastName()).strip());
        } catch (IllegalArgumentException malformedHolderId) {
            return Optional.empty();
        }
    }
}
