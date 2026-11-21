package com.kalibra.api.iam.domain.model.aggregates;

import com.kalibra.api.iam.domain.model.valueobjects.ClientApplication;
import com.kalibra.api.iam.domain.model.valueobjects.Email;
import com.kalibra.api.iam.domain.model.valueobjects.HashedPassword;
import com.kalibra.api.iam.domain.model.valueobjects.Role;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public class User {

    private UUID id;
    private Email email;
    private String firstName;
    private String lastName;
    private HashedPassword hashedPassword;
    private Set<Role> roles;
    private Instant createdAt;

    // public: required by MapStruct, which generates its mapper impl in a different package.
    public User() {
    }

    private User(Email email, String firstName, String lastName, HashedPassword hashedPassword, Role role) {
        this.id = UUID.randomUUID();
        this.email = email;
        this.firstName = requireName(firstName, "firstName");
        this.lastName = requireName(lastName, "lastName");
        this.hashedPassword = hashedPassword;
        this.roles = EnumSet.of(Role.REGISTERED_USER, role);
        this.createdAt = Instant.now();
    }

    public static User register(Email email, String firstName, String lastName,
                                HashedPassword hashedPassword, ClientApplication application) {
        return new User(email, firstName, lastName, hashedPassword, roleFor(application));
    }

    // Administrators never self-register: they are provisioned by an operator (or the dev seeder)
    // and hold only REGISTERED_USER + ADMINISTRATOR, so teacher and student endpoints stay closed to them.
    public static User registerAdministrator(Email email, String firstName, String lastName,
                                             HashedPassword hashedPassword) {
        return new User(email, firstName, lastName, hashedPassword, Role.ADMINISTRATOR);
    }

    private static String requireName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.strip();
    }

    // WEB_PLATFORM serves teachers and administrators; MOBILE_APP serves students only.
    public boolean canUse(ClientApplication application) {
        return switch (application) {
            case MOBILE_APP -> hasRole(Role.STUDENT);
            case WEB_PLATFORM -> hasRole(Role.TEACHER) || hasRole(Role.ADMINISTRATOR);
        };
    }

    private static Role roleFor(ClientApplication application) {
        return switch (application) {
            case MOBILE_APP -> Role.STUDENT;
            case WEB_PLATFORM -> Role.TEACHER;
        };
    }

    // Additive: grants ADMINISTRATOR without ever removing REGISTERED_USER, so an
    // operator keeps using the app normally while also holding elevated access.
    public void grantAdministratorRole() {
        this.roles.add(Role.ADMINISTRATOR);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public UUID getId() {
        return id;
    }

    public Email getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public HashedPassword getHashedPassword() {
        return hashedPassword;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setEmail(Email email) {
        this.email = email;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setHashedPassword(HashedPassword hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
