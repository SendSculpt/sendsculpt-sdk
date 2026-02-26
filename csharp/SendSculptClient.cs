using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using System.Threading.Tasks;

namespace SendSculpt
{
    public class SendSculptClient
    {
        private readonly HttpClient _httpClient;
        private readonly string _apiKey;
        private readonly string _environment;
        private readonly string _baseUrl;

        public SendSculptClient(string apiKey, string environment = "live")
        {
            if (string.IsNullOrWhiteSpace(apiKey))
                throw new ArgumentException("API Key cannot be null or empty.", nameof(apiKey));

            _apiKey = apiKey;
            _environment = environment;
            _baseUrl = "https://api.sendsculpt.com/api/v1";
            _httpClient = new HttpClient();
        }

        public async Task<SendEmailResponse> SendEmailAsync(SendEmailRequest request)
        {
            ValidateRequest(request);

            if (request.Attachments != null)
            {
                foreach (var att in request.Attachments)
                {
                    if (string.IsNullOrEmpty(att.Content))
                    {
                        if (att.ContentBytes != null && att.ContentBytes.Length > 0)
                        {
                            att.Content = Convert.ToBase64String(att.ContentBytes);
                        }
                        else if (!string.IsNullOrEmpty(att.FilePath))
                        {
                            if (!File.Exists(att.FilePath))
                            {
                                throw new FileNotFoundException($"Attachment file not found: {att.FilePath}");
                            }
                            att.Content = Convert.ToBase64String(File.ReadAllBytes(att.FilePath));
                        }
                        else
                        {
                            throw new ArgumentException("Attachment must specify Content, ContentBytes, or FilePath.");
                        }
                    }
                }
            }

            request.Environment = _environment;

            var requestUri = $"{_baseUrl}/send";
            var jsonBody = JsonSerializer.Serialize(request, new JsonSerializerOptions
            {
                DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
                PropertyNamingPolicy = new SnakeCaseNamingPolicy()
            });

            var content = new StringContent(jsonBody, Encoding.UTF8, "application/json");
            
            using var req = new HttpRequestMessage(HttpMethod.Post, requestUri);
            req.Content = content;
            req.Headers.Add("x-sendsculpt-key", _apiKey);

            var response = await _httpClient.SendAsync(req);
            var responseString = await response.Content.ReadAsStringAsync();

            if (!response.IsSuccessStatusCode)
            {
                throw new Exception($"SendSculpt API Error [{(int)response.StatusCode}]: {responseString}");
            }

            return JsonSerializer.Deserialize<SendEmailResponse>(responseString, new JsonSerializerOptions
            {
                PropertyNamingPolicy = JsonNamingPolicy.CamelCase // Or custom if response uses snake_case, assuming standard here. Needs adjustment based on actual return from the API.
            });
        }

        private void ValidateRequest(SendEmailRequest req)
        {
            if (req.To == null || req.To.Count == 0)
                throw new ArgumentException("'To' field is required and cannot be empty.");
            if (string.IsNullOrWhiteSpace(req.Subject))
                throw new ArgumentException("'Subject' field is required.");
            if (string.IsNullOrWhiteSpace(req.FromEmail))
                throw new ArgumentException("'FromEmail' field is required.");

            if (req.TemplateData != null && string.IsNullOrWhiteSpace(req.TemplateId))
                throw new ArgumentException("TemplateData and TemplateId must be provided together.");
            
            if (!string.IsNullOrWhiteSpace(req.TemplateId) && (!string.IsNullOrWhiteSpace(req.BodyHtml) || !string.IsNullOrWhiteSpace(req.BodyText)))
                throw new ArgumentException("TemplateId and BodyHtml/BodyText cannot be provided together.");
        }
    }

    public class SendEmailRequest
    {
        [JsonPropertyName("to")]
        public List<string> To { get; set; }

        [JsonPropertyName("subject")]
        public string Subject { get; set; }

        [JsonPropertyName("from_email")]
        public string FromEmail { get; set; }

        [JsonPropertyName("body_html")]
        public string BodyHtml { get; set; }

        [JsonPropertyName("body_text")]
        public string BodyText { get; set; }

        [JsonPropertyName("cc")]
        public List<string> Cc { get; set; }

        [JsonPropertyName("bcc")]
        public List<string> Bcc { get; set; }

        [JsonPropertyName("template_id")]
        public string TemplateId { get; set; }

        [JsonPropertyName("template_data")]
        public Dictionary<string, object> TemplateData { get; set; }

        [JsonPropertyName("reply_to")]
        public List<string> ReplyTo { get; set; }

        [JsonPropertyName("attachments")]
        public List<Attachment> Attachments { get; set; }

        [JsonPropertyName("sender_name")]
        public string SenderName { get; set; }

        [JsonPropertyName("environment")]
        public string Environment { get; set; }
    }

    public class Attachment
    {
        [JsonPropertyName("filename")]
        public string Filename { get; set; }

        [JsonPropertyName("content")]
        public string Content { get; set; } // Base64 encoded

        [JsonIgnore]
        public byte[] ContentBytes { get; set; } // Raw bytes, auto-converted

        [JsonIgnore]
        public string FilePath { get; set; } // Local path, auto-converted

        [JsonPropertyName("mime_type")]
        public string MimeType { get; set; }
    }

    public class SendEmailResponse
    {
        [JsonPropertyName("message_id")]
        public string MessageId { get; set; }

        [JsonPropertyName("status")]
        public string Status { get; set; }
    }

    // Helper for snake_case serialization
    public class SnakeCaseNamingPolicy : JsonNamingPolicy
    {
        public override string ConvertName(string name)
        {
            if (string.IsNullOrEmpty(name)) return name;

            var sb = new StringBuilder();
            sb.Append(char.ToLowerInvariant(name[0]));

            for (int i = 1; i < name.Length; ++i)
            {
                char c = name[i];
                if (char.IsUpper(c))
                {
                    sb.Append('_');
                    sb.Append(char.ToLowerInvariant(c));
                }
                else
                {
                    sb.Append(c);
                }
            }
            return sb.ToString();
        }
    }
}
