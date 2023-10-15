package com.demat.invoice.service;
import com.demat.invoice.beans.*;
import org.apache.http.HttpHeaders;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Service
public class KeycloakAuthorizationService {

    private final WebClient keycloakWebClient;
    private final WebClient tokenWebClient;
    private final String adminClientId;
    private final String adminClientSecret;

    private final String clientId;

    private String accessToken;

    public KeycloakAuthorizationService(WebClient.Builder webClientBuilder) {
        this.keycloakWebClient = webClientBuilder.baseUrl("http://localhost:9080/admin/realms/jhipster")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .filter(authorizationHeaderFilter()) // Add authorization filter
            .build();
        this.tokenWebClient = WebClient.builder()
            .baseUrl("http://localhost:9080/realms/master/protocol/openid-connect/token")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .build();

        // Define your client ID and client secret here
        this.adminClientId = "admin-rest-client";
        this.adminClientSecret = "nkQ0MOnpHE7ZKaU3RmtNzwxQmYQ2F9qE";
        this.clientId = "6e8deddb-b4d6-4e2e-b389-b397d3f74fcd";
    }

    private Mono<String> getAccessToken() {
        // Define the client ID and client secret for your application
        String clientId = "admin-rest-client";
        String clientSecret = "nkQ0MOnpHE7ZKaU3RmtNzwxQmYQ2F9qE";

        // Create a request to obtain an access token using client credentials
        return tokenWebClient.post()
            .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                .with("client_id", clientId)
                .with("client_secret", clientSecret))
            .retrieve()
            .bodyToMono(AccessTokenResponse.class)
            .map(tokenResponse -> "Bearer " + tokenResponse.getToken())
            .switchIfEmpty(Mono.error(new RuntimeException("Failed to obtain an access token from Keycloak.")));
    }

    private Mono<List<Resource>> getResourceIdByResourceName(String clientId, String resourceName) {
        return keycloakWebClient.get()
            .uri("/clients/" + clientId + "/authz/resource-server/resource?name=" + resourceName)
            .retrieve()
            .bodyToFlux(Resource.class)
            .collectList();// Use bodyToFlux to receive multiple Role objects
    }

    private Mono<List<Permission>> getPermissionsByResourceId(String clientId, String resourceId) {
        return keycloakWebClient.get()
            .uri("/clients/" + clientId + "/authz/resource-server/resource/" + resourceId+"/permissions")
            .retrieve()
            .bodyToFlux(Permission.class)
            .collectList();// Use bodyToFlux to receive multiple Role objects
    }

    private Mono<List<Policy>> getPoliciesByPermissionId(String clientId, String permissionId) {
        return keycloakWebClient.get()
            .uri("/clients/" + clientId + "/authz/resource-server/policy/" + permissionId+"/associatedPolicies")
            .retrieve()
            .bodyToFlux(Policy.class)
            .collectList();// Use bodyToFlux to receive multiple Role objects
    }

    public Mono<Boolean> hasPermission(String username, String resourceName, String permissionName) {
        return getResourceIdByResourceName(this.clientId, resourceName)
            .flatMap(resources -> {
                if(resources == null || resources.isEmpty()) {
                    return Mono.just(false);
                } else {
                    return getPermissionsByResourceId(this.clientId, resources.get(0).get_id())
                        .flatMap(permissions -> {
                            Optional<Permission> foundPermission = permissions.stream()
                                .filter(permission -> permissionName.equals(permission.getName()))
                                .findFirst();

                            // Check if a permission with the given name was found
                            if (foundPermission.isPresent()) {
                                Permission permission = foundPermission.get();
                                return getUserRoles(username, this.clientId)
                                    .flatMap(userRoles -> {
                                        return getPoliciesByPermissionId(this.clientId, permission.getId())
                                            .flatMapMany(policies -> Flux.fromIterable(policies)
                                                .flatMap(policy -> {
                                                    return getPolicyRolesWithPolicyId(this.clientId, policy.getId())
                                                        .map(policyWithRoles -> {
                                                            policy.setConfig(policyWithRoles.getConfig()); // Set the config attribute
                                                            return policy;
                                                        });
                                                }))
                                            .collectList()
                                            .flatMap(policyList -> {
                                                boolean hasPermission = policyList.stream()
                                                    .anyMatch(policy -> policy.getConfig() != null &&
                                                        policy.getConfig().getRoles().stream()
                                                            .anyMatch(userRoles::contains));
                                                return Mono.just(hasPermission);
                                            });

                                    });
                            } else {
                                return Mono.just(false);
                            }

                        });
                }


            });


    }

    // Step 2: Get User Roles
    private Mono<List<String>> getUserRoles(String userId, String clientId) {
        return keycloakWebClient.get()
            .uri("/users/" + userId + "/role-mappings/clients/" + clientId)
            .retrieve()
            .bodyToFlux(Role.class) // Use bodyToFlux to receive multiple Role objects


            .map(Role::getId) // Extract the ID from each Role object
            .collectList(); // Collect all IDs into a List<String>
    }

    private ExchangeFilterFunction authorizationHeaderFilter() {
        return (clientRequest, next) -> {
            // Use the getAccessToken method to obtain the access token as a Mono
            return getAccessToken()
                .flatMap(accessToken -> {
                    // Create a new ClientRequest with the Authorization header
                    ClientRequest newRequest = ClientRequest.from(clientRequest)
                        .header(HttpHeaders.AUTHORIZATION, accessToken)
                        .build();

                    return next.exchange(newRequest);
                });
        };
    }

    private boolean isAccessTokenExpired() {
        return true; // Replace with your logic
    }

    // Step 3: Get Associated Policies to Specific Permission
    private Mono<com.demat.invoice.beans.Policy> getPolicyRolesWithPolicyId(String clientId, String policyId) {
        return keycloakWebClient.get()
            .uri("/clients/"+clientId+"/authz/resource-server/policy/"+policyId)
            .retrieve()
            .bodyToMono(com.demat.invoice.beans.Policy.class);
    }

}
