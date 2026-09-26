package com.kalibra.api.iam.domain.services;

import com.kalibra.api.iam.domain.model.aggregates.User;
import com.kalibra.api.iam.domain.model.commands.SignInCommand;
import com.kalibra.api.iam.domain.model.commands.SignUpCommand;

import java.util.Optional;

public interface UserCommandService {

    Optional<User> handle(SignUpCommand command);

    Optional<User> handle(SignInCommand command);
}
