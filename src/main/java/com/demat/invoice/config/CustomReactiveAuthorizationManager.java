package com.demat.invoice.config;
import com.demat.invoice.service.KeycloakAuthorizationService;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import reactor.core.publisher.Mono;

public class CustomReactiveAuthorizationManager implements ReactiveAuthorizationManager<org.springframework.security.web.server.authorization.AuthorizationContext> {

    private final KeycloakAuthorizationService authorizationService;
    private final String resourceName;
    private final String permissionName;

    public CustomReactiveAuthorizationManager(KeycloakAuthorizationService authorizationService, String resourceName, String permissionName) {
        this.authorizationService = authorizationService;
        this.resourceName = resourceName;
        this.permissionName = permissionName;
    }

    // Implement methods to extract user ID and resource ID as needed
    private String getUserId(Authentication auth) {
        return auth.getName();
    }

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, org.springframework.security.web.server.authorization.AuthorizationContext object) {
        return authentication.flatMap(auth -> {
            String userId = getUserId(auth);
            return authorizationService.hasPermission(userId, this.resourceName, this.permissionName)
                .map(hasPermission -> new AuthorizationDecision(hasPermission));
        });
    }

    @Override
    public Mono<Void> verify(Mono<Authentication> authentication, org.springframework.security.web.server.authorization.AuthorizationContext object) {
        return ReactiveAuthorizationManager.super.verify(authentication, object);
    }
}




