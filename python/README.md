# SendSculpt Python SDK

The official Python client for sending emails via the SendSculpt Mailer API.

## Requirements
- Python 3.7+
- `requests` library

## Installation
You can easily install the required dependencies:
```bash
pip install requests
```

## Quick Start

```python
from sendsculpt import SendSculptClient

# Initialize the client with your API Key
client = SendSculptClient(api_key="your-api-key")

# Send a basic email
response = client.send_email(
    to=["recipient@example.com"],
    subject="Welcome to SendSculpt!",
    from_email="noreply@yourdomain.com",
    body_text="Hello! This is a test email from the SendSculpt Python SDK."
)

print(response)
```

## Advanced Usage (With Templates)

```python
from sendsculpt import SendSculptClient

client = SendSculptClient(api_key="your-api-key")

response = client.send_email(
    to=["john@doe.com"],
    subject="Your Invoice",
    from_email="billing@yourdomain.com",
    template_id="uuid-of-your-template",
    template_data={
        "first_name": "John",
        "invoice_amount": "49.99"
    },
    cc=["accounts@doe.com"],
    attachments=[
        {
            "filename": "invoice.pdf",
            "file_path": "/path/to/local/invoice.pdf", # Auto-converted to Base64
            "mime_type": "application/pdf"
        }
    ]
)
print("Email queued successfully:", response['message_id'])
```

## Local Development
If you are running tests or sandbox development, initialize the client with the `environment` set to `"sandbox"`:

```python
client = SendSculptClient(
    api_key="your-api-key", 
    environment="sandbox"
)
```
