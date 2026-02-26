# SendSculpt Ruby SDK

The official Ruby client for sending emails via the SendSculpt Mailer API.

## Requirements
- Ruby 2.6+

## Installation

Add this line to your application's Gemfile:

```ruby
gem 'sendsculpt-sdk'
```

And then execute:

```bash
bundle install
```

Or install it yourself as:

```bash
gem install sendsculpt-sdk
```

*(Note: If not published, you can require the local `lib/sendsculpt.rb` file.)*

## Quick Start

```ruby
require 'sendsculpt'

# Initialize the client with your API Key
client = Sendsculpt::Client.new('your-api-key')

begin
  response = client.send_email(
    to: ['recipient@example.com'],
    subject: 'Welcome to SendSculpt!',
    from_email: 'noreply@yourdomain.com',
    body_html: '<p>Hello! This is a test email from the SendSculpt Ruby SDK.</p>'
  )
  puts "Success: #{response}"
rescue => e
  puts "Failed to send: #{e.message}"
end
```

## Advanced Usage (With Templates & Attachments)

The Ruby SDK provides a convenience layer for files. You don't need to read and base64-encode files manually—just provide a `file_path`, and the SDK will do it for you!

```ruby
require 'sendsculpt'

# For local development or sandbox testing, pass the environment as the second argument:
client = Sendsculpt::Client.new('your-api-key', 'sandbox')

begin
  response = client.send_email(
    to: ['john@doe.com'],
    subject: 'Your Monthly Invoice',
    from_email: 'billing@yourdomain.com',
    template_id: 'your-template-uuid',
    template_data: {
      first_name: 'John',
      invoice_amount: '49.99'
    },
    cc: ['finance@yourdomain.com'],
    attachments: [
      {
        filename: 'invoice.pdf',
        file_path: '/path/to/local/invoice.pdf', # Auto-converted to Base64!
        mime_type: 'application/pdf'
      }
    ]
  )
  puts "Email queued successfully. Message ID: #{response['message_id']}"
rescue => e
  puts e.message
end
```
