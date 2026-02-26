<?php

namespace SendSculpt;

class SendSculptClient {
    private string $apiKey;
    private string $environment;
    private string $baseUrl = 'https://api.sendsculpt.com/api/v1';

    /**
     * Initialize the SendSculptClient.
     *
     * @param string $apiKey Your SendSculpt API key.
     * @param string $environment The environment to run the SDK on (live or sandbox).
     */
    public function __construct(string $apiKey, string $environment = 'live') {
        if (empty($apiKey)) {
            throw new \InvalidArgumentException("API Key is required.");
        }
        $this->apiKey = $apiKey;
        $this->environment = $environment;
    }

    /**
     * Send an email via the SendSculpt API.
     *
     * @param array $options Send params including to, subject, from_email, etc.
     * @return array Response JSON decoded into an associative array.
     * @throws \Exception
     */
    public function sendEmail(array $options): array {
        if (empty($options['to']) || !is_array($options['to'])) {
            throw new \InvalidArgumentException("'to' is required and must be an array of emails.");
        }
        if (empty($options['subject'])) {
            throw new \InvalidArgumentException("'subject' is required.");
        }
        if (empty($options['from_email'])) {
            throw new \InvalidArgumentException("'from_email' is required.");
        }

        if (isset($options['template_data']) && !isset($options['template_id'])) {
            throw new \InvalidArgumentException("template_data and template_id must be provided together.");
        }
        if (isset($options['template_id']) && (isset($options['body_html']) || isset($options['body_text']))) {
            throw new \InvalidArgumentException("template_id and body_html/body_text cannot be provided together.");
        }

        if (isset($options['attachments']) && is_array($options['attachments'])) {
            $formattedAttachments = [];
            foreach ($options['attachments'] as $att) {
                $fmtAtt = [
                    'filename' => $att['filename'] ?? null,
                    'mime_type' => $att['mime_type'] ?? null,
                ];

                if (isset($att['content'])) {
                    $fmtAtt['content'] = $att['content'];
                } elseif (isset($att['content_bytes'])) {
                    $fmtAtt['content'] = base64_encode($att['content_bytes']);
                } elseif (isset($att['file_path'])) {
                    if (!file_exists($att['file_path'])) {
                        throw new \InvalidArgumentException("Attachment file not found: " . $att['file_path']);
                    }
                    $fmtAtt['content'] = base64_encode(file_get_contents($att['file_path']));
                } else {
                    throw new \InvalidArgumentException("Attachment must specify 'content' (base64 string), 'content_bytes' (raw bytes), or 'file_path' (string).");
                }
                $formattedAttachments[] = $fmtAtt;
            }
            $options['attachments'] = $formattedAttachments;
        }

        $payload = array_filter($options, function ($value) {
            return $value !== null;
        });
        
        $payload['environment'] = $this->environment;

        $url = $this->baseUrl . '/send';
        $ch = curl_init($url);

        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, [
            'x-sendsculpt-key: ' . $this->apiKey,
            'Content-Type: application/json',
            'Accept: application/json'
        ]);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($payload));

        $response = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        $error = curl_error($ch);
        curl_close($ch);

        if ($error) {
            throw new \Exception("cURL Error: " . $error);
        }

        $decodedResponse = json_decode($response, true);

        if ($httpCode >= 400) {
            $errorMsg = isset($decodedResponse['detail']) ? json_encode($decodedResponse['detail']) : $response;
            throw new \Exception("SendSculpt API Error [{$httpCode}]: " . $errorMsg);
        }

        return $decodedResponse;
    }
}
