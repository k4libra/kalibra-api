package com.kalibra.api.iam.domain.model.commands;

import com.kalibra.api.iam.domain.model.valueobjects.Email;

public record SignUpCommand(Email email, String rawPassword) { }
