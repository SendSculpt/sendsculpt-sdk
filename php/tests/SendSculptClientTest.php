<?php

namespace SendSculpt;

// Mocking cURL functions in the SendSculpt namespace

$mockCurlData = [
    'requests' => [],
    'response' => '{"message_id":"test-msg-id","status":"sent"}',
    'http_code' => 200,
    'error' => '',
];

function curl_init($url = null) {
    global $mockCurlData;
    $mockCurlData['requests'][] = ['url' => $url, 'options' => []];
    return count($mockCurlData['requests']) - 1; // return index as handle
}

function curl_setopt($ch, $option, $value) {
    global $mockCurlData;
    $mockCurlData['requests'][$ch]['options'][$option] = $value;
    return true;
}

function curl_exec($ch) {
    global $mockCurlData;
    return $mockCurlData['response'];
}

function curl_getinfo($ch, $opt) {
    global $mockCurlData;
    if ($opt === CURLINFO_HTTP_CODE) return $mockCurlData['http_code'];
    return null;
}

function curl_error($ch) {
    global $mockCurlData;
    return $mockCurlData['error'];
}

function curl_close($ch) {
    // No-op
}

use PHPUnit\Framework\TestCase;

class SendSculptClientTest extends TestCase
{
    private $client;

    protected function setUp(): void
    {
        global $mockCurlData;
        $mockCurlData = [
            'requests' => [],
            'response' => '{"message_id":"test-msg-id","status":"sent"}',
            'http_code' => 200,
            'error' => '',
        ];
        
        $this->client = new SendSculptClient('test-api-key', 'sandbox');
    }

    public function testMissingApiKeyThrowsException()
    {
        $this->expectException(\InvalidArgumentException::class);
        new SendSculptClient('');
    }

    public function testSendEmailSuccessfully()
    {
        global $mockCurlData;

        $response = $this->client->sendEmail([
            'to' => ['recipient@example.com'],
            'subject' => 'Test Subject',
            'from_email' => 'noreply@example.com',
            'body_text' => 'Hello World'
        ]);

        $this->assertEquals('test-msg-id', $response['message_id']);
        
        $request = $mockCurlData['requests'][0];
        $this->assertEquals('https://api.sendsculpt.com/api/v1/send', $request['url']);
        
        // Check payload
        $payload = json_decode($request['options'][CURLOPT_POSTFIELDS], true);
        $this->assertEquals('Test Subject', $payload['subject']);
        $this->assertEquals(['recipient@example.com'], $payload['to']);
        
        // Check headers
        $headers = $request['options'][CURLOPT_HTTPHEADER];
        $this->assertContains('x-sendsculpt-key: test-api-key', $headers);
    }

    public function testValidationFailsForTemplateAndBody()
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('template_id and body_html/body_text cannot be provided together.');

        $this->client->sendEmail([
            'to' => ['recipient@example.com'],
            'subject' => 'Test',
            'from_email' => 'noreply@example.com',
            'template_id' => 'uuid',
            'body_html' => '<h1>Hello</h1>'
        ]);
    }
    
    public function testValidationFailsForTemplateDataWithoutId()
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('template_data and template_id must be provided together.');

        $this->client->sendEmail([
            'to' => ['recipient@example.com'],
            'subject' => 'Test',
            'from_email' => 'noreply@example.com',
            'template_data' => ['foo' => 'bar']
        ]);
    }

    public function testAttachmentsAreBase64Encoded()
    {
        global $mockCurlData;
        
        $this->client->sendEmail([
            'to' => ['recipient@example.com'],
            'subject' => 'Test',
            'from_email' => 'noreply@example.com',
            'attachments' => [
                [
                    'filename' => 'test.txt',
                    'content_bytes' => 'raw bytes here',
                    'mime_type' => 'text/plain'
                ]
            ]
        ]);
        
        $request = $mockCurlData['requests'][0];
        $payload = json_decode($request['options'][CURLOPT_POSTFIELDS], true);
        
        $expectedBase64 = base64_encode('raw bytes here');
        $this->assertEquals($expectedBase64, $payload['attachments'][0]['content']);
    }

    public function testFilePathAttachmentEncoding()
    {
        global $mockCurlData;
        
        $tmpFile = tempnam(sys_get_temp_dir(), 'test');
        file_put_contents($tmpFile, 'hello file');
        
        try {
            $this->client->sendEmail([
                'to' => ['recipient@example.com'],
                'subject' => 'Test',
                'from_email' => 'noreply@example.com',
                'attachments' => [
                    [
                        'filename' => 'test.txt',
                        'file_path' => $tmpFile,
                        'mime_type' => 'text/plain'
                    ]
                ]
            ]);
            
            $request = $mockCurlData['requests'][0];
            $payload = json_decode($request['options'][CURLOPT_POSTFIELDS], true);
            
            $expectedBase64 = base64_encode('hello file');
            $this->assertEquals($expectedBase64, $payload['attachments'][0]['content']);
        } finally {
            unlink($tmpFile);
        }
    }

    public function testMissingFilePathThrowsException()
    {
        $this->expectException(\InvalidArgumentException::class);
        $this->expectExceptionMessage('Attachment file not found:');
        
        $this->client->sendEmail([
            'to' => ['recipient@example.com'],
            'subject' => 'Test',
            'from_email' => 'noreply@example.com',
            'attachments' => [
                [
                    'filename' => 'missing.txt',
                    'file_path' => '/does/not/exist.txt',
                    'mime_type' => 'text/plain'
                ]
            ]
        ]);
    }
    
    public function testApiErrorThrowsException()
    {
        global $mockCurlData;
        $mockCurlData['http_code'] = 400;
        $mockCurlData['response'] = '{"detail": ["Invalid request"]}';
        
        $this->expectException(\Exception::class);
        $this->expectExceptionMessage('SendSculpt API Error [400]: ["Invalid request"]');
        
        $this->client->sendEmail([
            'to' => ['recipient@example.com'],
            'subject' => 'Test Subject',
            'from_email' => 'noreply@example.com'
        ]);
    }
}
