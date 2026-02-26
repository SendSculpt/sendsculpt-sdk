# SendSculpt PHP SDK

The official PHP implementation for sending emails via the SendSculpt Mailer API. This package works in vanilla PHP environments as well as within popular frameworks like Laravel, CodeIgniter, and Symfony.

## Requirements
- PHP 7.4+
- `ext-curl`
- `ext-json`

## Installation
*(Assuming availability via Packagist)*
```bash
composer require sendsculpt/sdk
```

Alternatively, you can just manually include the `SendSculptClient.php` class in your project.

---

## 1. Vanilla PHP Usage

```php
require_once 'path/to/SendSculptClient.php';

use SendSculpt\SendSculptClient;

// Initialize the client
$client = new SendSculptClient('your-api-key', 'live');

try {
    $response = $client->sendEmail([
        'to' => ['recipient@example.com'],
        'subject' => 'Welcome to SendSculpt!',
        'from_email' => 'noreply@yourdomain.com',
        'body_html' => '<b>Hello!</b> This is a test from PHP.',
        // 'cc' => ['team@yourdomain.com'],
        'attachments' => [
            [
                'filename' => 'document.txt',
                'file_path' => '/path/to/local/document.txt', // Auto-converts to Base64
                'mime_type' => 'text/plain'
            ]
        ]
    ]);

    echo "Email sent successfully! Message ID: " . $response['message_id'];
} catch (Exception $e) {
    echo "Error sending email: " . $e->getMessage();
}
```

---

## 2. Laravel Integration

### Setup
We have provided a sample Service Provider: `SendSculptServiceProvider.php`.
1. Place the `SendSculptServiceProvider.php` file in your `app/Providers` directory.
2. In `config/app.php`, add the provider to the `providers` array:

```php
'providers' => [
    // ...
    App\Providers\SendSculptServiceProvider::class,
],
```

3. In `config/services.php`, append the following:

```php
'sendsculpt' => [
    'key' => env('SENDSCULPT_API_KEY'),
    'environment' => env('SENDSCULPT_ENVIRONMENT', 'live'),
],
```

4. Update your `.env` file!

```env
SENDSCULPT_API_KEY="your-api-key"
```

### Usage in Controller

```php
namespace App\Http\Controllers;

use SendSculpt\SendSculptClient;

class EmailController extends Controller
{
    public function send(SendSculptClient $client)
    {
        $response = $client->sendEmail([
            'to' => ['john@doe.com'],
            'subject' => 'Laravel SendSculpt Test',
            'from_email' => 'test@yourdomain.com',
            'body_text' => 'It works perfectly in Laravel!'
        ]);

        return response()->json($response);
    }
}
```

---

## 3. CodeIgniter 4 Integration

For CodeIgniter 4, you can create a simple Library wrapper or inject it manually within your Controllers or BaseController.

### Example in a Controller

```php
namespace App\Controllers;

use SendSculpt\SendSculptClient;

class MailController extends BaseController
{
    public function index()
    {
        $apiKey = getenv('SENDSCULPT_API_KEY');
        $environment = getenv('SENDSCULPT_ENVIRONMENT') ?: 'live';

        $client = new SendSculptClient($apiKey, $environment);

        try {
            $response = $client->sendEmail([
                'to' => ['recipient@example.com'],
                'from_email' => 'noreply@yourdomain.com',
                'subject' => 'CodeIgniter SendSculpt integration',
                'template_id' => 'your-template-id',
                'template_data' => [
                    'name' => 'John Doe'
                ]
            ]);

            return $this->response->setJSON(['status' => 'success', 'data' => $response]);
        } catch (\Exception $e) {
            return $this->response->setStatusCode(500)->setJSON(['error' => $e->getMessage()]);
        }
    }
}
```

---

## 4. Symfony Integration

To register `SendSculptClient` as a service in Symfony, configure your `services.yaml`.

### `config/services.yaml`
```yaml
parameters:
    sendsculpt_api_key: '%env(SENDSCULPT_API_KEY)%'
    sendsculpt_environment: '%env(default:environment_default:SENDSCULPT_ENVIRONMENT)%'
    environment_default: 'live'

services:
    SendSculpt\SendSculptClient:
        arguments:
            $apiKey: '%sendsculpt_api_key%'
            $environment: '%sendsculpt_environment%'
```

### Usage in Controller

```php
namespace App\Controller;

use SendSculpt\SendSculptClient;
use Symfony\Component\HttpFoundation\JsonResponse;
use Symfony\Component\Routing\Annotation\Route;

class MailerController
{
    #[Route('/test-email', name: 'test_email')]
    public function index(SendSculptClient $client): JsonResponse
    {
        try {
            $response = $client->sendEmail([
                'to' => ['user@example.com'],
                'subject' => 'Symfony Integration',
                'from_email' => 'hello@yourdomain.com',
                'body_html' => '<h1>Hello from Symfony</h1>'
            ]);

            return new JsonResponse($response);
        } catch (\Exception $e) {
            return new JsonResponse(['error' => $e->getMessage()], 500);
        }
    }
}
```
