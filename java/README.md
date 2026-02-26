# SendSculpt Java SDK

The official Java client for sending emails via the SendSculpt Mailer API.

## Requirements
- Java 11+
- `jackson-databind` (for JSON serialization)

## Installation

### Maven
Include the Jackson dependency in your `pom.xml` since it's required for JSON serialization. Alternatively, import the SDK via JitPack, or add `com.sendsculpt` classes to your project.

```xml
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.15.2</version> 
</dependency>
```

## Quick Start

```java
import com.sendsculpt.SendEmailRequest;
import com.sendsculpt.SendEmailResponse;
import com.sendsculpt.SendSculptClient;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        // Initialize client. Options are:
        // 1. SendSculptClient(String apiKey) (defaults to live production)
        // 2. SendSculptClient(String apiKey, String environment) (for sandbox environments)
        
        SendSculptClient client = new SendSculptClient("your-api-key", "sandbox");

        SendEmailRequest request = new SendEmailRequest();
        request.setTo(Arrays.asList("recipient@example.com"));
        request.setSubject("Welcome to SendSculpt! (Java Test)");
        request.setFromEmail("noreply@yourdomain.com");
        request.setBodyHtml("<h1>Hello!</h1><p>Test sent from Java 11 HttpRequest.</p>");

        try {
            SendEmailResponse response = client.sendEmail(request);
            System.out.println("Email sent successfully: " + response.getMessageId());
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }
}
```

## Advanced Usage (With Templates & Custom Data)

```java
import java.util.HashMap;
import java.util.Map;
import com.sendsculpt.SendEmailRequest;
import com.sendsculpt.SendSculptClient;

public class TestTemplates {
    public static void main(String[] args) {
        SendSculptClient client = new SendSculptClient("your-api-key");

        SendEmailRequest request = new SendEmailRequest();
        request.setTo(Arrays.asList("john@doe.com"));
        request.setSubject("Your Purchase Invoice");
        request.setFromEmail("billing@yourdomain.com");
        request.setTemplateId("your-template-id");

        Map<String, Object> data = new HashMap<>();
        data.put("first_name", "John");
        data.put("amount", "49.99");
        request.setTemplateData(data);
        
        SendEmailRequest.Attachment attachment = new SendEmailRequest.Attachment();
        attachment.setFilename("invoice.pdf");
        attachment.setFilePath("/path/to/local/invoice.pdf"); // Auto-converted to Base64
        attachment.setMimeType("application/pdf");
        
        request.setAttachments(Arrays.asList(attachment));
        try {
            var response = client.sendEmail(request);
            System.out.println("Message ID: " + response.getMessageId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

```
