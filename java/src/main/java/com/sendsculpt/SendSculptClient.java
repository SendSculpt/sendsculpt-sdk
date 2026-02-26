package com.sendsculpt;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SendSculptClient {
    private final String apiKey;
    private final String environment;
    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Initialize the SendSculptClient.
     *
     * @param apiKey Your SendSculpt API key.
     * @param environment The environment to use (live or sandbox).
     */
    public SendSculptClient(String apiKey, String environment) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API Key cannot be null or empty.");
        }
        this.apiKey = apiKey;
        this.environment = environment != null && !environment.trim().isEmpty() ? environment : "live";
        this.baseUrl = "https://api.sendsculpt.com/api/v1";

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        this.objectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

    public SendSculptClient(String apiKey) {
        this(apiKey, "live");
    }

    /**
     * Send an email via the SendSculpt API.
     *
     * @param request The SendEmailRequest object holding the paylaod.
     * @return The SendEmailResponse holding the status and message_id.
     * @throws Exception On any serialization or IO error, or if the API returns a non-200 status code.
     */
    public SendEmailResponse sendEmail(SendEmailRequest request) throws Exception {
        validateRequest(request);
        
        request.setEnvironment(this.environment);

        String jsonBody = objectMapper.writeValueAsString(request);
        String endpoint = this.baseUrl + "/send";

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofMinutes(1))
                .header("x-sendsculpt-key", this.apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException(String.format("SendSculpt API Error [%d]: %s", response.statusCode(), response.body()));
        }

        return objectMapper.readValue(response.body(), SendEmailResponse.class);
    }

    private void validateRequest(SendEmailRequest req) {
        if (req.getTo() == null || req.getTo().isEmpty()) {
            throw new IllegalArgumentException("'To' field is required and cannot be empty.");
        }
        if (req.getSubject() == null || req.getSubject().trim().isEmpty()) {
            throw new IllegalArgumentException("'Subject' field is required.");
        }
        if (req.getFromEmail() == null || req.getFromEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("'FromEmail' field is required.");
        }

        if (req.getTemplateData() != null && !req.getTemplateData().isEmpty() && (req.getTemplateId() == null || req.getTemplateId().trim().isEmpty())) {
            throw new IllegalArgumentException("templateData and templateId must be provided together.");
        }

        if (req.getTemplateId() != null && !req.getTemplateId().trim().isEmpty()) {
            if ((req.getBodyHtml() != null && !req.getBodyHtml().trim().isEmpty()) || (req.getBodyText() != null && !req.getBodyText().trim().isEmpty())) {
                throw new IllegalArgumentException("templateId and bodyHtml/bodyText cannot be provided together.");
            }
        }
    }
}
