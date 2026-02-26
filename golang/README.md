# SendSculpt Golang SDK

The official Golang client for sending emails via the SendSculpt Mailer API.

## Requirements
- Go 1.18+

## Installation

```bash
go get github.com/sendsculpt/sendsculpt-sdk-go
```
*(Use local relative import if using module without publishing)*

## Quick Start

```go
package main

import (
	"fmt"
	"log"

	"github.com/sendsculpt/sendsculpt-sdk-go" // Adjust based on your module path
)

func main() {
	// Initialize the client. The second argument is the Base URL.
	// You can omit the Base URL to use the default one.
	client := sendsculpt.NewClient("your-api-key", "https://api.sendsculpt.com/api/v1")

	// Create request payload
	req := &sendsculpt.SendEmailRequest{
		To:        []string{"recipient@example.com"},
		Subject:   "Welcome to SendSculpt!",
		FromEmail: "noreply@yourdomain.com",
		BodyHTML:  sendsculpt.StringPtr("<h1>Hello!</h1><p>This is a test email.</p>"),
	}

	// Send email
	response, err := client.SendEmail(req)
	if err != nil {
		log.Fatalf("Failed to send email: %v", err)
	}

	fmt.Printf("Email queued successfully! Message ID: %s\n", response.MessageID)
}
```

## Advanced Usage (With Templates)

```go
package main

import (
	"fmt"
	"log"

	"github.com/sendsculpt/sendsculpt-sdk-go"
)

func sendInvoiceEmail(client *sendsculpt.Client) {
	req := &sendsculpt.SendEmailRequest{
		To:        []string{"john@doe.com"},
		Subject:   "Your Monthly Invoice",
		FromEmail: "billing@yourdomain.com",
		TemplateID: sendsculpt.StringPtr("uuid-of-your-template"),
		TemplateData: map[string]interface{}{
			"first_name":     "John",
			"invoice_amount": "49.99",
		},
		CC: []string{"finance@yourdomain.com"},
		Attachments: []sendsculpt.Attachment{
			{
				Filename: "invoice.pdf",
				FilePath: "/path/to/invoice.pdf", // Auto-converted to Base64
				MimeType: "application/pdf",
			},
		},
	}

	res, err := client.SendEmail(req)
	if err != nil {
		log.Fatalf("Error: %v", err)
	}

	fmt.Println("Message ID:", res.MessageID)
}
```
