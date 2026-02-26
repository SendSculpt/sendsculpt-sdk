using System;
using System.Collections.Generic;
using System.IO;
using System.Net;
using System.Net.Http;
using System.Text.Json;
using System.Threading.Tasks;
using Moq;
using Moq.Contrib.HttpClient;
using Xunit;
using SendSculpt;

namespace SendSculpt.Tests
{
    public class SendSculptClientTests
    {
        private readonly Mock<HttpMessageHandler> _handlerMock;
        private readonly HttpClient _httpClient;
        private readonly SendSculptClient _client;

        public SendSculptClientTests()
        {
            _handlerMock = new Mock<HttpMessageHandler>();
            _httpClient = _handlerMock.CreateClient();
            _client = new SendSculptClient("test-api-key");
            
            // Overriding the _httpClient via reflection to inject the mock since it's private and has no setter
            var field = typeof(SendSculptClient).GetField("_httpClient", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance);
            field.SetValue(_client, _httpClient);
        }

        [Fact]
        public void MissingApiKey_ThrowsArgumentException()
        {
            Assert.Throws<ArgumentException>(() => new SendSculptClient(null));
            Assert.Throws<ArgumentException>(() => new SendSculptClient(""));
        }

        [Fact]
        public async Task SendEmailAsync_Success_ReturnsResponse()
        {
            // Arrange
            _handlerMock.SetupRequest(HttpMethod.Post, "https://api.sendsculpt.com/api/v1/send")
                .ReturnsResponse(HttpStatusCode.OK, "{\"message_id\":\"test-msg-id\",\"status\":\"sent\"}", "application/json");

            var request = new SendEmailRequest
            {
                To = new List<string> { "recipient@example.com" },
                Subject = "Test Subject",
                FromEmail = "noreply@example.com",
                BodyText = "Hello World"
            };

            // Act
            var response = await _client.SendEmailAsync(request);

            // Assert
            Assert.Equal("test-msg-id", response.MessageId);
            Assert.Equal("sent", response.Status);
            
            _handlerMock.VerifyRequest(HttpMethod.Post, "https://api.sendsculpt.com/api/v1/send",
                r => r.Headers.Contains("x-sendsculpt-key"));
        }

        [Fact]
        public async Task SendEmailAsync_ValidationError_MissingTo()
        {
            var request = new SendEmailRequest
            {
                Subject = "Test Subject",
                FromEmail = "noreply@example.com"
            };

            var exception = await Assert.ThrowsAsync<ArgumentException>(() => _client.SendEmailAsync(request));
            Assert.Contains("'To' field is required", exception.Message);
        }

        [Fact]
        public async Task SendEmailAsync_TemplateValidation_BothTemplateIdAndBody()
        {
            var request = new SendEmailRequest
            {
                To = new List<string> { "recipient@example.com" },
                Subject = "Test Subject",
                FromEmail = "noreply@example.com",
                TemplateId = "uuid",
                BodyHtml = "<p>Conflict</p>"
            };

            var exception = await Assert.ThrowsAsync<ArgumentException>(() => _client.SendEmailAsync(request));
            Assert.Contains("cannot be provided together", exception.Message);
        }

        [Fact]
        public async Task SendEmailAsync_Attachments_Base64Encoding()
        {
            // Arrange
            _handlerMock.SetupRequest(HttpMethod.Post, "https://api.sendsculpt.com/api/v1/send")
                .ReturnsResponse(HttpStatusCode.OK, "{\"message_id\":\"msg\",\"status\":\"sent\"}", "application/json");

            var tempFile = Path.GetTempFileName();
            await File.WriteAllTextAsync(tempFile, "hello file");

            var request = new SendEmailRequest
            {
                To = new List<string> { "recipient@example.com" },
                Subject = "Test",
                FromEmail = "noreply@example.com",
                Attachments = new List<Attachment>
                {
                    new Attachment
                    {
                        Filename = "att1.txt",
                        ContentBytes = System.Text.Encoding.UTF8.GetBytes("raw bytes"),
                        MimeType = "text/plain"
                    },
                    new Attachment
                    {
                        Filename = "test.txt",
                        FilePath = tempFile,
                        MimeType = "text/plain"
                    }
                }
            };

            // Act
            await _client.SendEmailAsync(request);

            // Assert
            var expectedBase64_1 = Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes("raw bytes"));
            var expectedBase64_2 = Convert.ToBase64String(System.Text.Encoding.UTF8.GetBytes("hello file"));

            _handlerMock.VerifyRequest(HttpMethod.Post, "https://api.sendsculpt.com/api/v1/send",
                r => r.Headers.Contains("x-sendsculpt-key"));

            // Cleanup
            File.Delete(tempFile);
        }

        [Fact]
        public async Task SendEmailAsync_MissingAttachmentFile_Throws()
        {
            var request = new SendEmailRequest
            {
                To = new List<string> { "recipient@example.com" },
                Subject = "Test",
                FromEmail = "noreply@example.com",
                Attachments = new List<Attachment>
                {
                    new Attachment
                    {
                        Filename = "missing.txt",
                        FilePath = "/path/does/not/exist.txt"
                    }
                }
            };

            var exception = await Assert.ThrowsAsync<FileNotFoundException>(() => _client.SendEmailAsync(request));
            Assert.Contains("Attachment file not found", exception.Message);
        }

        [Fact]
        public async Task SendEmailAsync_ApiError_ThrowsException()
        {
            _handlerMock.SetupRequest(HttpMethod.Post, "https://api.sendsculpt.com/api/v1/send")
                .ReturnsResponse(HttpStatusCode.BadRequest, "{\"detail\":[\"Error string\"]}", "application/json");

            var request = new SendEmailRequest
            {
                To = new List<string> { "recipient@example.com" },
                Subject = "Test Subject",
                FromEmail = "noreply@example.com"
            };

            var exception = await Assert.ThrowsAsync<Exception>(() => _client.SendEmailAsync(request));
            Assert.Contains("SendSculpt API Error [400]", exception.Message);
        }
    }
}
