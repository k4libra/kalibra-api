package com.kalibra.api.iam.application.internal.commandservices;

import com.kalibra.api.iam.application.internal.outboundservices.hashing.HashingService;
import com.kalibra.api.iam.domain.exceptions.ApplicationAccessDeniedException;
import com.kalibra.api.iam.domain.exceptions.EmailAlreadyRegisteredException;
import com.kalibra.api.iam.domain.model.aggregates.User;
import com.kalibra.api.iam.domain.model.commands.SignInCommand;
import com.kalibra.api.iam.domain.model.commands.SignUpCommand;
import com.kalibra.api.iam.domain.model.events.UserRegistered;
import com.kalibra.api.iam.domain.model.valueobjects.HashedPassword;
import com.kalibra.api.iam.domain.repositories.UserRepository;
import com.kalibra.api.iam.domain.services.UserCommandService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository userRepository;
    private final HashingService hashingService;
    private final ApplicationEventPublisher eventPublisher;

    public UserCommandServiceImpl(UserRepository userRepository,
                                   HashingService hashingService,
                                   ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.hashingService = hashingService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<User> handle(SignUpCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyRegisteredException(command.email().value());
        }
        var hashed = new HashedPassword(hashingService.hash(command.rawPassword()));
        var user = User.register(command.email(), command.firstName(), command.lastName(), hashed, command.application());
        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException concurrentSignUp) {
            throw new EmailAlreadyRegisteredException(command.email().value());
        }

        eventPublisher.publishEvent(new UserRegistered(saved.getId(), saved.getId().toString()));
        return Optional.of(saved);
    }

    @Override
    public Optional<User> handle(SignInCommand command) {
        var user = userRepository.findByEmail(command.email())
                .filter(candidate -> hashingService.matches(command.rawPassword(), candidate.getHashedPassword().value()));
        // Checked only after the password matched, so the 403 never reveals which emails exist.
        if (user.isPresent() && command.application() != null && !user.get().canUse(command.application())) {
            throw new ApplicationAccessDeniedException(command.application().name());
        }
        return user;
    }
}
