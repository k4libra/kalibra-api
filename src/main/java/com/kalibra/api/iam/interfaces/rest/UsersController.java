package com.kalibra.api.iam.interfaces.rest;

import com.kalibra.api.iam.domain.model.queries.GetUserByIdQuery;
import com.kalibra.api.iam.domain.services.UserQueryService;
import com.kalibra.api.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.kalibra.api.iam.interfaces.rest.transform.UserAssembler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Users", description = "The signed-in user")
@RequestMapping("/api/v1/users")
public class UsersController {

    private final UserQueryService userQueryService;
    private final UserAssembler assembler;

    public UsersController(UserQueryService userQueryService, UserAssembler assembler) {
        this.userQueryService = userQueryService;
        this.assembler = assembler;
    }

    // Lets a client restore its session from the httpOnly cookie after a reload,
    // since JavaScript can never read the token itself.
    @Operation(summary = "Get the signed-in user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signed-in user"),
            @ApiResponse(responseCode = "401", description = "No valid session")
    })
    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResource> getMe(Authentication authentication) {
        return userQueryService.handle(new GetUserByIdQuery(UUID.fromString(authentication.getName())))
                .map(assembler::toAuthenticatedResource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }
}
