package com.kalibra.api.shared.contracts.iam;

import java.util.Set;
import java.util.UUID;

public record UserSummary(UUID userId, String email, String firstName, String lastName, Set<String> roles) { }
