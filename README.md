# SendSculpt SDKs

Welcome to the official **SendSculpt SDKs** repository! This monorepo contains the official API clients/wrappers for integrating your applications with the **SendSculpt Mailer API**.

SendSculpt makes it effortless to send beautifully sculpted emails securely and at scale. These SDKs abstract away the complexity of making raw HTTP requests, providing a standardized, strongly-typed, and language-idiomatic way to trigger your emails.

## Available SDKs

| Tech Stack | Link to Documentation | Status | Description |
|---|---|---|---|
| **[Python](./python/README.md)** | [`/python`](./python) | Available | Official Python wrapper using `requests`. |
| **[Ruby](./ruby/README.md)** | [`/ruby`](./ruby) | Available | Official Ruby client wrapper. |
| **[Node.js](./nodejs/README.md)** | [`/nodejs`](./nodejs) | Available | Official Node.js/JavaScript client using `axios`. |
| **[PHP](./php/README.md)** | [`/php`](./php) | Available | Official Vanilla PHP, Laravel, CodeIgniter & Symfony provider. |
| **[Golang](./golang/README.md)** | [`/golang`](./golang) | Available | Official Go module utilizing `net/http`. |
| **[C# (.NET)](./csharp/README.md)** | [`/csharp`](./csharp) | Available | Official .NET wrapper built with `HttpClient`. |
| **[Java](./java/README.md)** | [`/java`](./java) | Available | Official Java client for Java 11+ via `java.net.http.HttpClient`. |


Click on any directory link above to see the complete integration/setup instructions and code snippets for that specific language.

---

## The SendSculpt Mailer API Reference

Under the hood, all our SDKs interface directly with the SendSculpt Mailer. While the SDKs do the heavy lifting, it's beneficial to understand how the API accepts data.

### Endpoint
`POST /api/v1/send`

### Authentication
SendSculpt uses an API Key for authentication. Pass your key in the custom header `x-sendsculpt-key`.

### Request Payload (`Content-Type: application/json`)
SendSculpt's underlying schema validates against the following schema:

```json
{
  "to": ["user1@example.com", "user2@example.com"],
  "subject": "Hello World",
  "from_email": "noreply@yourdomain.com",
  
  // Optional Fields
  "body_html": "<h1>Say Hello!</h1>",
  "body_text": "Say Hello!",
  "cc": ["team_member@example.com"],
  "bcc": ["archive@example.com"],
  
  // Templating 
  "template_id": "uuid-of-the-template",
  "template_data": {
    "first_name": "John",
    "invoice_amount": "49.99"
  },
  
  // Advanced Routing
  "reply_to": ["support@yourdomain.com"],
  "sender_name": "The Awesome Team",
  
  // Attachments
  "attachments": [
    {
      "filename": "invoice_001.pdf",
      "file_path": "/path/to/local/invoice.pdf",
      "mime_type": "application/pdf"
    }
  ]
}
```

### Constraints and Validations
- `to`, `subject`, and `from_email` are strictly required.
- The domain in the `from_email` property must be registered and fully verified (SPF, DKIM) under your SendSculpt organization account.
- **Templates vs. Body:** You cannot provide `template_id` and `body_html` / `body_text` simultaneously.
- When passing a `template_id`, and if your template requires context variables, make sure to supply them via `template_data`. `template_data` requires a `template_id` to be present.

### Response

The SendSculpt SDK commands will return a structured JSON response upon success (HTTP 200).

```json
{
  "message_id": "000001234abcd...",
  "status": "sent"
}
```

On error, a runtime exception or specific error object is raised within the respective SDKs carrying the detailed `detail` array or string describing the HTTP 4XX/5XX API problem.

---

## Contributing
We welcome contributions to SendSculpt! Whether you spotted a bug, want an SDK for a new language (e.g., Rust), or wish to optimize an existing implementation.
1. Fork the repo.
2. Create your feature branch.
3. Submit a pull request.

## Support
Encounter issues or have questions regarding SDK integration?
Feel free to open a GitHub Issue in this repository or contact SendSculpt tech support.
