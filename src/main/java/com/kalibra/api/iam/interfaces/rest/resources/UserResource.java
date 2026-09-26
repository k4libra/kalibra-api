package com.kalibra.api.iam.interfaces.rest.resources;

import java.util.Set;
import java.util.UUID;

public record UserResource(UUID id, String email, Set<String> roles) { }
