package com.kalibra.api.iam.application.internal.outboundservices.tokens;

import com.kalibra.api.iam.domain.model.valueobjects.Role;

import java.util.Set;

public interface TokenService {

    String issueFor(String holderId, Set<Role> roles);
}
