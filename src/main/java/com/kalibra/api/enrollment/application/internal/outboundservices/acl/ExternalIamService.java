package com.kalibra.api.enrollment.application.internal.outboundservices.acl;

import com.kalibra.api.enrollment.domain.model.valueobjects.Email;
import com.kalibra.api.enrollment.domain.model.valueobjects.StudentId;
import com.kalibra.api.iam.interfaces.acl.IamContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ExternalIamService {

    private final IamContextFacade iamContextFacade;

    public ExternalIamService(IamContextFacade iamContextFacade) {
        this.iamContextFacade = iamContextFacade;
    }

    public Optional<StudentId> fetchStudentIdByEmail(Email email) {
        return iamContextFacade
                .fetchStudentIdByEmail(email.value())
                .map(StudentId::new);
    }

    public Optional<String> fetchUserEmail(String holderId) {
        try {
            return iamContextFacade
                    .fetchUserSummary(UUID.fromString(holderId))
                    .map(user -> user.email());
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}