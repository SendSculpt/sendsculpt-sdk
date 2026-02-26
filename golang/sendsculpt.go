package sendsculpt

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"os"
	"strings"
	"time"
)

// Client handles communication with the SendSculpt API.
type Client struct {
	BaseURL    string
	APIKey     string
	HTTPClient *http.Client
}

// Attachment represents an email attachment.
type Attachment struct {
	Filename     string `json:"filename"`
	Content      string `json:"content"` // Base64 encoded string
	ContentBytes []byte `json:"-"`       // Raw bytes, will be automatically encoded
	FilePath     string `json:"-"`       // Path to local file, will be automatically read and encoded
	MimeType     string `json:"mime_type"`
}

// SendEmailRequest represents the payload for sending an email.
type SendEmailRequest struct {
	To           []string               `json:"to"`
	Subject      string                 `json:"subject"`
	FromEmail    string                 `json:"from_email"`
	BodyHTML     *string                `json:"body_html,omitempty"`
	BodyText     *string                `json:"body_text,omitempty"`
	CC           []string               `json:"cc,omitempty"`
	BCC          []string               `json:"bcc,omitempty"`
	TemplateID   *string                `json:"template_id,omitempty"`
	TemplateData map[string]interface{} `json:"template_data,omitempty"`
	ReplyTo      []string               `json:"reply_to,omitempty"`
	Attachments  []Attachment           `json:"attachments,omitempty"`
	SenderName   *string                `json:"sender_name,omitempty"`
}

// SendEmailResponse represents the successful API response.
type SendEmailResponse struct {
	MessageID string `json:"message_id"`
	Status    string `json:"status"`
}

// NewClient creates a new SendSculpt API client.
func NewClient(apiKey string, baseURL ...string) *Client {
	url := "https://api.sendsculpt.com/api/v1"
	if len(baseURL) > 0 {
		url = strings.TrimRight(baseURL[0], "/")
	}

	return &Client{
		BaseURL: url,
		APIKey:  apiKey,
		HTTPClient: &http.Client{
			Timeout: time.Minute,
		},
	}
}

// SendEmail sends an email via the SendSculpt API.
func (c *Client) SendEmail(req *SendEmailRequest) (*SendEmailResponse, error) {
	if req.To == nil || len(req.To) == 0 {
		return nil, errors.New("'To' field is required")
	}
	if req.Subject == "" {
		return nil, errors.New("'Subject' field is required")
	}
	if req.FromEmail == "" {
		return nil, errors.New("'FromEmail' field is required")
	}

	if req.TemplateData != nil && req.TemplateID == nil {
		return nil, errors.New("TemplateData and TemplateID must be provided together")
	}
	if req.TemplateID != nil && (req.BodyHTML != nil || req.BodyText != nil) {
		return nil, errors.New("TemplateID and BodyHTML/BodyText cannot be provided together")
	}

	for i, att := range req.Attachments {
		if att.Content == "" {
			if len(att.ContentBytes) > 0 {
				req.Attachments[i].Content = base64.StdEncoding.EncodeToString(att.ContentBytes)
			} else if att.FilePath != "" {
				b, err := os.ReadFile(att.FilePath)
				if err != nil {
					return nil, fmt.Errorf("failed to read attachment file %s: %w", att.FilePath, err)
				}
				req.Attachments[i].Content = base64.StdEncoding.EncodeToString(b)
			} else {
				return nil, errors.New("Attachment must specify Content, ContentBytes, or FilePath")
			}
		}
	}

	bodyBytes, err := json.Marshal(req)
	if err != nil {
		return nil, err
	}

	endpoint := fmt.Sprintf("%s/send", c.BaseURL)
	httpReq, err := http.NewRequest("POST", endpoint, bytes.NewBuffer(bodyBytes))
	if err != nil {
		return nil, err
	}

	httpReq.Header.Set("x-sendsculpt-key", c.APIKey)
	httpReq.Header.Set("Content-Type", "application/json")
	httpReq.Header.Set("Accept", "application/json")

	res, err := c.HTTPClient.Do(httpReq)
	if err != nil {
		return nil, err
	}
	defer res.Body.Close()

	resBody, err := io.ReadAll(res.Body)
	if err != nil {
		return nil, err
	}

	if res.StatusCode >= 400 {
		return nil, fmt.Errorf("SendSculpt API Error [%d]: %s", res.StatusCode, string(resBody))
	}

	var response SendEmailResponse
	if err := json.Unmarshal(resBody, &response); err != nil {
		return nil, err
	}

	return &response, nil
}

// StringPtr is a helper to get a pointer to a string.
func StringPtr(s string) *string {
	return &s
}
