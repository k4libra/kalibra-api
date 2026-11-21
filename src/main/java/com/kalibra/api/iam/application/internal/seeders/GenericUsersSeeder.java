package com.kalibra.api.iam.application.internal.seeders;

import com.kalibra.api.iam.application.internal.outboundservices.hashing.HashingService;
import com.kalibra.api.iam.domain.model.aggregates.User;
import com.kalibra.api.iam.domain.model.valueobjects.ClientApplication;
import com.kalibra.api.iam.domain.model.valueobjects.Email;
import com.kalibra.api.iam.domain.model.valueobjects.HashedPassword;
import com.kalibra.api.iam.domain.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

// Local and demo environments only: off unless KALIBRA_SEED_GENERIC_USERS=true, so production
// never ships accounts with well-known passwords. Idempotent: existing emails are skipped.
@Component
@ConditionalOnProperty(name = "kalibra.seed.generic-users", havingValue = "true")
public class GenericUsersSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GenericUsersSeeder.class);
    private static final String DOMAIN = "@upc.edu.pe";

    private enum Kind { TEACHER, STUDENT, ADMINISTRATOR }

    private record Account(String alias, String firstName, String lastName, Kind kind) {
    }

    static final List<Account> ACCOUNTS = List.of(
            new Account("profesor.test1", "Profesor", "Test Uno", Kind.TEACHER),
            new Account("profesor.test2", "Profesor", "Test Dos", Kind.TEACHER),
            new Account("estudiante.test1", "Estudiante", "Test Uno", Kind.STUDENT),
            new Account("estudiante.test2", "Estudiante", "Test Dos", Kind.STUDENT),
            new Account("estudiante.test3", "Estudiante", "Test Tres", Kind.STUDENT),
            new Account("admin.test1", "Admin", "Test Uno", Kind.ADMINISTRATOR)
    );

    private final UserRepository userRepository;
    private final HashingService hashingService;

    public GenericUsersSeeder(UserRepository userRepository, HashingService hashingService) {
        this.userRepository = userRepository;
        this.hashingService = hashingService;
    }

    @Override
    public void run(ApplicationArguments args) {
        var created = 0;
        for (var account : ACCOUNTS) {
            var email = new Email(account.alias() + DOMAIN);
            if (userRepository.existsByEmail(email)) {
                continue;
            }
            userRepository.save(toUser(account, email));
            created++;
        }
        log.info("Generic users seeder created {} account(s)", created);
    }

    // Password convention: "@" + alias without the dot, e.g. profesor.test1 -> @profesortest1.
    static String passwordFor(String alias) {
        return "@" + alias.replace(".", "");
    }

    private User toUser(Account account, Email email) {
        var hashed = new HashedPassword(hashingService.hash(passwordFor(account.alias())));
        return switch (account.kind()) {
            case TEACHER -> User.register(email, account.firstName(), account.lastName(), hashed,
                    ClientApplication.WEB_PLATFORM);
            case STUDENT -> User.register(email, account.firstName(), account.lastName(), hashed,
                    ClientApplication.MOBILE_APP);
            case ADMINISTRATOR -> User.registerAdministrator(email, account.firstName(), account.lastName(), hashed);
        };
    }
}
