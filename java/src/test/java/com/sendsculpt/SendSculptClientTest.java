package com.sendsculpt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendSculptClientTest {

    private SendSculptClient client;

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockHttpResponse;

    @BeforeEach
    void setUp() throws Exception {
        client = new SendSculptClient("test-api-key");
        
        java.lang.reflect.Field httpClientField = SendSculptClient.class.getDeclaredField("httpClient");
        httpClientField.setAccessible(true);
        httpClientField.set(client, mockHttpClient);
    }

    @Test
    void testMissingApiKeyThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new SendSculptClient(null));
        assertThrows(IllegalArgumentException.class, () -> new SendSculptClient(""));
    }

    @Test
    void testSendEmailSuccessfully() throws Exception {
        String successJsonResponse = "{\"message_id\":\"test-msg-id\",\"status\":\"sent\"}";
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(successJsonResponse);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        SendEmailRequest req = new SendEmailRequest();
        req.setTo(List.of("recipient@example.com"));
        req.setSubject("Test Subject");
        req.setFromEmail("noreply@example.com");
        req.setBodyText("Hello World");

        SendEmailResponse response = client.sendEmail(req);

        assertEquals("test-msg-id", response.getMessageId());
        assertEquals("sent", response.getStatus());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        
        HttpRequest capturedReq = requestCaptor.getValue();
        assertEquals("https://api.sendsculpt.com/api/v1/send", capturedReq.uri().toString());
        assertEquals("POST", capturedReq.method());
        assertEquals("test-api-key", capturedReq.headers().firstValue("x-sendsculpt-key").orElse(""));
    }

    @Test
    void testValidationFailsMissingFields() {
        SendEmailRequest req = new SendEmailRequest();
        req.setSubject("Test");
        req.setFromEmail("noreply@example.com");
        
        Exception e = assertThrows(IllegalArgumentException.class, () -> client.sendEmail(req));
        assertTrue(e.getMessage().contains("To"));
    }

    @Test
    void testValidationFailsTemplateIdAndBodyConflict() {
        SendEmailRequest req = new SendEmailRequest();
        req.setTo(List.of("test@example.com"));
        req.setSubject("Test");
        req.setFromEmail("noreply@example.com");
        req.setTemplateId("uuid");
        req.setBodyText("Conflict");

        assertThrows(IllegalArgumentException.class, () -> client.sendEmail(req));
    }

    @Test
    void testValidationFailsTemplateDataWithoutId() {
        SendEmailRequest req = new SendEmailRequest();
        req.setTo(List.of("test@example.com"));
        req.setSubject("Test");
        req.setFromEmail("noreply@example.com");
        req.setTemplateData(Map.of("key", "value"));

        assertThrows(IllegalArgumentException.class, () -> client.sendEmail(req));
    }

    @Test
    void testAttachmentsEncoding(@TempDir Path tempDir) throws Exception {
        String successJsonResponse = "{\"message_id\":\"msg\",\"status\":\"sent\"}";
        when(mockHttpResponse.statusCode()).thenReturn(200);
        when(mockHttpResponse.body()).thenReturn(successJsonResponse);
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        // Temp file attachment
        Path tempFile = tempDir.resolve("test.txt");
        Files.writeString(tempFile, "hello from java");

        SendEmailRequest req = new SendEmailRequest();
        req.setTo(List.of("recipient@example.com"));
        req.setSubject("Test Subject");
        req.setFromEmail("noreply@example.com");

        // Raw bytes attachment
        SendEmailRequest.Attachment att1 = new SendEmailRequest.Attachment();
        att1.setFilename("att1.txt");
        att1.setMimeType("text/plain");
        att1.setContentBytes("raw bytes".getBytes());

        // File path attachment
        SendEmailRequest.Attachment att2 = new SendEmailRequest.Attachment();
        att2.setFilename("test.txt");
        att2.setMimeType("text/plain");
        att2.setFilePath(tempFile.toAbsolutePath().toString());

        req.setAttachments(List.of(att1, att2));

        client.sendEmail(req);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(mockHttpClient).send(requestCaptor.capture(), any());
    }

    @Test
    void testMissingFilePathAttachment() {
        SendEmailRequest req = new SendEmailRequest();
        req.setTo(List.of("test@example.com"));
        req.setSubject("Test");
        req.setFromEmail("noreply@example.com");

        SendEmailRequest.Attachment att2 = new SendEmailRequest.Attachment();
        att2.setFilename("missing.txt");
        att2.setMimeType("text/plain");
        att2.setFilePath("/path/does/not/exist.txt");
        
        req.setAttachments(List.of(att2));

        Exception e = assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class, () -> {
            // Jackson tries to call getContent() during object mapper serialization
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writeValueAsString(req);
        });
        assertTrue(e.getMessage().contains("NoSuchFileException"));
    }

    @Test
    void testApiErrorResponseThrowsException() throws Exception {
        when(mockHttpResponse.statusCode()).thenReturn(400);
        when(mockHttpResponse.body()).thenReturn("{\"detail\":[\"Error string\"]}");
        when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mockHttpResponse);

        SendEmailRequest req = new SendEmailRequest();
        req.setTo(List.of("recipient@example.com"));
        req.setSubject("Test Subject");
        req.setFromEmail("noreply@example.com");

        Exception e = assertThrows(RuntimeException.class, () -> client.sendEmail(req));
        assertTrue(e.getMessage().contains("SendSculpt API Error [400]"));
    }
}
