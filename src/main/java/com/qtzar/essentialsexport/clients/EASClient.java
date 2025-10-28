package com.qtzar.essentialsexport.clients;

import com.qtzar.essentialsexport.model.essential.request.BearerTokenBody;
import com.qtzar.essentialsexport.model.essential.request.RefreshTokenBody;
import com.qtzar.essentialsexport.model.essential.response.BearerTokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class EASClient {
    private final RestClient easRestClient;

    @Value("${eas.endpoint}")
    private String endpoint;

    @Value("${eas.apiKey}")
    private String apiKey;

    @Value("${eas.username}")
    private String username;

    @Value("${eas.password}")
    private String password;

    private String authToken = "";
    private String refreshToken = "";
    private Instant authExpires = Instant.now().minus(1, ChronoUnit.MINUTES);
    private Instant refreshExpires = Instant.now().minus(1, ChronoUnit.MINUTES);

    public EASClient() {
        easRestClient = RestClient.builder()
                .baseUrl(endpoint)
                .defaultHeader("User-Agent", "Essential Export Application")
                .build();
    }

    private void checkAuth() {
        Instant now = Instant.now().plusSeconds(60);
        if (authToken.isEmpty() || authExpires.isBefore(now)) {
            getAuthToken();
        }
    }

    private void getAuthToken() {
        //log.info("EAS API HIT : getAuthToken");

        BearerTokenResponse response;

        if (!refreshToken.isEmpty() && refreshExpires.isBefore(Instant.now().minusSeconds(60))) {
            RefreshTokenBody refreshTokenBody = new RefreshTokenBody();
            refreshTokenBody.setGrantType("refresh_token");
            refreshTokenBody.setRefreshToken(refreshToken);
            response = easRestClient.post()
                    .uri("/oauth/token")
                    .body(refreshTokenBody)
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(BearerTokenResponse.class);
        } else {
            BearerTokenBody body = new BearerTokenBody();
            body.setGrantType("password");
            body.setUsername(username);
            body.setPassword(password);
            response = easRestClient.post()
                    .uri("/oauth/token")
                    .body(body)
                    .header("x-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(BearerTokenResponse.class);
        }

        assert response != null;
        authToken = "Bearer " + response.getBearerToken();
        refreshToken = response.getRefreshToken();
        authExpires = Instant.now().plus(response.getExpiresInMinutes(), ChronoUnit.MINUTES);
        refreshExpires = Instant.now().plus(response.getRefreshTokenExpiresInMinutes(), ChronoUnit.MINUTES);

    }

    /**
     * Get metadata for all classes in the specified repository, including their slots.
     * Returns a map where keys are class names and values contain slots information.
     *
     * @param repoId The repository ID to query
     * @return Map of class metadata with nested slots
     */
    public Object getClassesMetadata(String repoId) {
        checkAuth();

        try {
            return easRestClient.get()
                    .uri("/essential-utility/v3/repositories/" + repoId + "/classes/meta-data")
                    .header("Authorization",  authToken)
                    .header("x-api-key", apiKey)
                    .retrieve()
                    .body(Object.class);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
     * Get all instances as raw Map data for dynamic field access from a specific repository.
     * This bypasses Instance.java deserialization to support arbitrary EAS slots.
     *
     * @param repoId The repository ID to query
     * @param instanceType The class name
     * @param depthCount Max depth for nested objects
     * @param slotValues Caret-separated list of slots to retrieve
     * @return List of instances as Maps with all requested fields
     */
    public List<java.util.Map<String, Object>> getAllInstancesAsMap(String repoId, String instanceType, int depthCount, String slotValues) {
        List<java.util.Map<String, Object>> results = new ArrayList<>();

        String endpoint;
        if (slotValues != null) {
            endpoint = "/essential-utility/v3/repositories/" + repoId + "/classes/" + instanceType + "/instances?maxdepth=" + depthCount + "&slots=" + slotValues;
        } else {
            endpoint = "/essential-utility/v3/repositories/" + repoId + "/classes/" + instanceType + "/instances?maxdepth=" + depthCount;
        }

        String pagination = "start=0,count=100";

        while (pagination != null) {
            pagination = "&" + pagination.replace(",", "&");
            String pagedEndpoint = endpoint + pagination;

            checkAuth();

            java.util.Map<String, Object> response = easRestClient.get()
                    .uri(pagedEndpoint)
                    .header("Authorization", authToken)
                    .header("x-api-key", apiKey)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && response.containsKey("instances")) {
                @SuppressWarnings("unchecked")
                List<java.util.Map<String, Object>> instances = (List<java.util.Map<String, Object>>) response.get("instances");
                results.addAll(instances);

                // Check for next page
                pagination = (String) response.get("next_page");
            } else {
                pagination = null;
            }
        }

        return results;
    }
}