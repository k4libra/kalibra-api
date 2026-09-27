package com.kalibra.api.shared.contracts.iam;

import java.util.Set;
import java.util.UUID;

public record UserSummary(UUID userId, String email, Set<String> roles) { }
