package com.kalibra.api.iam.interfaces.rest.resources;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUserResource(UUID id, String email, Set<String> roles) { }
