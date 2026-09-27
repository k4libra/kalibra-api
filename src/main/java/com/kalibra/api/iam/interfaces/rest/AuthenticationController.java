package com.kalibra.api.iam.interfaces.rest;

import com.kalibra.api.iam.application.internal.outboundservices.tokens.TokenService;
import com.kalibra.api.iam.domain.exceptions.InvalidCredentialsException;
import com.kalibra.api.iam.domain.services.UserCommandService;
import com.kalibra.api.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.kalibra.api.iam.interfaces.rest.resources.SignInResource;
import com.kalibra.api.iam.interfaces.rest.resources.SignUpResource;
import com.kalibra.api.iam.interfaces.rest.resources.UserResource;
import com.kalibra.api.iam.interfaces.rest.transform.UserAssembler;
import com.kalibra.api.shared.config.JwtCookieFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Authentication", description = "Endpoints for user authentication and session management")
@RequestMapping("/api/v1/authentication")
@SecurityRequirements
public class AuthenticationController {

    private final UserCommandService userCommandService;
    private final TokenService tokenService;
    private final UserAssembler assembler;
    private final JwtCookieFactory cookieFactory;

    public AuthenticationController(UserCommandService userCommandService,
                                     TokenService tokenService,
                                     UserAssembler assembler,
                                     JwtCookieFactory cookieFactory) {
        this.userCommandService = userCommandService;
        this.tokenService = tokenService;
        this.assembler = assembler;
        this.cookieFactory = cookieFactory;
    }

    @Operation(summary = "Sign up",
            description = "Creates an account. MOBILE_APP registers a STUDENT and WEB_PLATFORM registers a TEACHER; "
                    + "every account also gets REGISTERED_USER.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Invalid email, password or application",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/sign-up")
    public ResponseEntity<UserResource> signUp(@Valid @RequestBody SignUpResource resource) {
        var command = assembler.toCommand(resource);
        var user = userCommandService.handle(command)
                .orElseThrow(() -> new IllegalStateException("Sign-up should never return empty"));
        return new ResponseEntity<>(assembler.toResource(user), HttpStatus.CREATED);
    }

    // The JWT never travels in the response body: it goes in an httpOnly cookie
    // (see JwtCookieFactory) so client-side JavaScript — and therefore XSS — can
    // never read it. The browser attaches it automatically on later requests.
    @Operation(summary = "Sign in",
            description = "Authenticates with email and password and sets the JWT in an httpOnly cookie. "
                    + "The token is never returned in the body.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated",
                    headers = @Header(name = HttpHeaders.SET_COOKIE,
                            description = "httpOnly, Secure, SameSite=Lax cookie named token carrying the JWT")),
            @ApiResponse(responseCode = "400", description = "Malformed request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401",
                    description = "Invalid credentials; the same answer for an unknown email and a wrong password",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/sign-in")
    public ResponseEntity<AuthenticatedUserResource> signIn(@Valid @RequestBody SignInResource resource, HttpServletResponse response) {
        var command = assembler.toCommand(resource);
        var user = userCommandService.handle(command)
                .orElseThrow(InvalidCredentialsException::new);
        var token = tokenService.issueFor(user.getId().toString(), user.getRoles());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.issue(token).toString());
        return ResponseEntity.ok(assembler.toAuthenticatedResource(user));
    }

    // JS cannot delete an httpOnly cookie itself, so ending a session server-side
    // is required even for plain logout (not just for forced/admin revocation).
    @Operation(summary = "Sign out", description = "Clears the JWT cookie. There is no server-side session to end.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cookie cleared",
                    headers = @Header(name = HttpHeaders.SET_COOKIE, description = "Expired token cookie (Max-Age=0)"))
    })
    @PostMapping("/sign-out")
    public ResponseEntity<Void> signOut(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString());
        return ResponseEntity.noContent().build();
    }
}
