package com.kalibra.api.iam.interfaces.rest.transform;

import com.kalibra.api.iam.domain.model.aggregates.User;
import com.kalibra.api.iam.domain.model.commands.SignInCommand;
import com.kalibra.api.iam.domain.model.commands.SignUpCommand;
import com.kalibra.api.iam.domain.model.valueobjects.Email;
import com.kalibra.api.iam.domain.model.valueobjects.Role;
import com.kalibra.api.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.kalibra.api.iam.interfaces.rest.resources.SignInResource;
import com.kalibra.api.iam.interfaces.rest.resources.SignUpResource;
import com.kalibra.api.iam.interfaces.rest.resources.UserResource;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserAssembler {

    @Mapping(target = "email", source = "resource.email")
    @Mapping(target = "rawPassword", source = "resource.password")
    @Mapping(target = "application", source = "resource.application")
    SignUpCommand toCommand(SignUpResource resource);

    @Mapping(target = "email", source = "resource.email")
    @Mapping(target = "rawPassword", source = "resource.password")
    SignInCommand toCommand(SignInResource resource);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "roles", source = "user.roles")
    UserResource toResource(User user);

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "roles", source = "user.roles")
    AuthenticatedUserResource toAuthenticatedResource(User user);

    // required by MapStruct: single-field VOs need an explicit converter.
    default Email map(String value) {
        return value == null ? null : new Email(value);
    }

    default String map(Email email) {
        return email == null ? null : email.value();
    }

    default Set<String> mapRoles(Set<Role> roles) {
        return roles == null ? null : roles.stream().map(Enum::name).collect(Collectors.toSet());
    }
}
