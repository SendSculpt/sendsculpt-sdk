# SendSculpt .NET / C# SDK

The official .NET / C# client for sending emails via the SendSculpt Mailer API.

## Requirements
- .NET Standard 2.0+ / .NET 6+
- `System.Text.Json`

## Installation
*Currently, you can include `SendSculptClient.cs` directly into your project. When published, use NuGet:*

```bash
dotnet add package SendSculpt.Sdk
```

## Quick Start
```csharp
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using SendSculpt;

class Program
{
    static async Task Main(string[] args)
    {
        // Initialize client. You can set the environment for testing/sandbox.
        var client = new SendSculptClient("your-api-key", "sandbox");

        var request = new SendEmailRequest
        {
            To = new List<string> { "recipient@example.com" },
            Subject = "Welcome to SendSculpt!",
            FromEmail = "noreply@yourdomain.com",
            BodyHtml = "<h1>Hello!</h1><p>This is a test from C#</p>"
        };

        try
        {
            var response = await client.SendEmailAsync(request);
            Console.WriteLine($"Email sent successfully: {response.MessageId}");
        }
        catch (Exception ex)
        {
            Console.WriteLine($"Error sending email: {ex.Message}");
        }
    }
}
```

## Advanced Usage (With Templates & Attachments)
```csharp
using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using SendSculpt;

class Program
{
    static async Task Main(string[] args)
    {
        var client = new SendSculptClient("your-api-key");

        var request = new SendEmailRequest
        {
            To = new List<string> { "recipient@example.com" },
            Subject = "Monthly Invoice",
            FromEmail = "billing@yourdomain.com",
            TemplateId = "your-template-id",
            TemplateData = new Dictionary<string, object>
            {
                { "first_name", "John" },
                { "amount", 49.99 }
            },
            Attachments = new List<Attachment>
            {
                new Attachment
                {
                    Filename = "invoice.pdf",
                    FilePath = "/path/to/local/invoice.pdf", // Auto-converted to Base64
                    MimeType = "application/pdf"
                }
            }
        };

        var response = await client.SendEmailAsync(request);
        Console.WriteLine($"Successfully queued. Message Id: {response.MessageId}");
    }
}
```
