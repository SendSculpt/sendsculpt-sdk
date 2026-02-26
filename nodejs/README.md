# SendSculpt Node.js SDK

The official Node.js client for sending emails via the SendSculpt Mailer API.

## Requirements
- Node.js 14+

## Installation
You can easily install the SDK alongside its dependencies:
```bash
npm install @sendsculpt/sdk
```
*(Note: If not published, you can install directly via path or link.)*

## Quick Start

```javascript
const SendSculptClient = require('@sendsculpt/sdk');

// Initialize the client with your API Key
const client = new SendSculptClient('your-api-key');

async function triggerEmail() {
  try {
    const response = await client.sendEmail({
      to: ['recipient@example.com'],
      subject: 'Welcome to SendSculpt!',
      fromEmail: 'noreply@yourdomain.com',
      bodyHtml: '<p>Hello! This is a test email from the SendSculpt Node.js SDK.</p>'
    });
    console.log('Success:', response);
  } catch (error) {
    console.error('Failed to send:', error.message);
  }
}

triggerEmail();
```

## Advanced Usage (With Templates & Attachments)

```javascript
const SendSculptClient = require('@sendsculpt/sdk');

const client = new SendSculptClient('your-api-key');

async function sendMonthlyInvoice() {
  try {
    const response = await client.sendEmail({
      to: ['john@doe.com'],
      subject: 'Your Monthly Invoice',
      fromEmail: 'billing@yourdomain.com',
      templateId: 'your-template-uuid',
      templateData: {
        first_name: 'John',
        invoice_amount: '49.99'
      },
      cc: ['finance@yourdomain.com'],
      attachments: [
        {
          filename: 'invoice.pdf',
          filePath: '/path/to/invoice.pdf', // Automatically converted to base64
          mime_type: 'application/pdf'
        }
      ]
    });
    console.log('Email queued successfully. Message ID:', response.message_id);
  } catch (error) {
    console.error(error.message);
  }
}

sendMonthlyInvoice();
```

## Local Development
If you are running tests or sandbox development, initialize the client with the `environment` set to `"sandbox"`:

```javascript
const client = new SendSculptClient('your-api-key', 'sandbox');
```
