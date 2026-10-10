package com.kalibra.api.iam.domain.model.commands;

import com.kalibra.api.iam.domain.model.valueobjects.ClientApplication;
import com.kalibra.api.iam.domain.model.valueobjects.Email;

// application is optional: when present, sign-in is refused for accounts that cannot use that client.
public record SignInCommand(Email email, String rawPassword, ClientApplication application) { }
