package com.kalibra.api.iam.domain.model.commands;

import com.kalibra.api.iam.domain.model.valueobjects.Email;

public record SignInCommand(Email email, String rawPassword) { }
