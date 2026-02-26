package sendsculpt

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestNewClient(t *testing.T) {
	client := NewClient("test-key")
	if client.APIKey != "test-key" {
		t.Errorf("Expected APIKey to be test-key, got %s", client.APIKey)
	}
	if client.BaseURL != "https://api.sendsculpt.com/api/v1" {
		t.Errorf("Expected BaseURL to be default, got %s", client.BaseURL)
	}
	if client.Environment != "live" {
		t.Errorf("Expected Environment to be default live, got %s", client.Environment)
	}

	clientCustom := NewClient("test-key", "sandbox")
	if clientCustom.Environment != "sandbox" {
		t.Errorf("Expected Environment to be sandbox, got %s", clientCustom.Environment)
	}
}

func TestSendEmailSuccess(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Header.Get("x-sendsculpt-key") != "test-key" {
			t.Errorf("Expected API Key in header")
		}
		if r.Header.Get("Content-Type") != "application/json" {
			t.Errorf("Expected Content-Type application/json")
		}

		var req SendEmailRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			t.Errorf("Failed to decode request body: %v", err)
		}
		if len(req.To) != 1 || req.To[0] != "recipient@example.com" {
			t.Errorf("Expected To to be recipient@example.com")
		}

		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		fmt.Fprintln(w, `{"message_id": "test-msg-id", "status": "sent"}`)
	}))
	defer server.Close()

	client := NewClient("test-key", "sandbox")
	client.BaseURL = server.URL

	req := &SendEmailRequest{
		To:        []string{"recipient@example.com"},
		Subject:   "Test",
		FromEmail: "noreply@example.com",
		BodyHTML:  StringPtr("<p>Test</p>"),
	}

	res, err := client.SendEmail(req)
	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}

	if res.MessageID != "test-msg-id" {
		t.Errorf("Expected message_id test-msg-id, got %s", res.MessageID)
	}
}

func TestSendEmailValidations(t *testing.T) {
	client := NewClient("test-key", "sandbox")
	client.BaseURL = "http://dummy"

	tests := []struct {
		name        string
		req         *SendEmailRequest
		expectedErr string
	}{
		{
			name: "'To' field missing",
			req: &SendEmailRequest{
				Subject:   "S",
				FromEmail: "f@e.com",
			},
			expectedErr: "'To' field is required",
		},
		{
			name: "'Subject' field missing",
			req: &SendEmailRequest{
				To:        []string{"t@e.com"},
				FromEmail: "f@e.com",
			},
			expectedErr: "'Subject' field is required",
		},
		{
			name: "'FromEmail' field missing",
			req: &SendEmailRequest{
				To:      []string{"t@e.com"},
				Subject: "S",
			},
			expectedErr: "'FromEmail' field is required",
		},
		{
			name: "TemplateData without TemplateID",
			req: &SendEmailRequest{
				To:           []string{"t@e.com"},
				Subject:      "S",
				FromEmail:    "f@e.com",
				TemplateData: map[string]interface{}{"k": "v"},
			},
			expectedErr: "TemplateData and TemplateID must be provided together",
		},
		{
			name: "TemplateID and BodyHTML together",
			req: &SendEmailRequest{
				To:         []string{"t@e.com"},
				Subject:    "S",
				FromEmail:  "f@e.com",
				TemplateID: StringPtr("uuid"),
				BodyHTML:   StringPtr("<p></p>"),
			},
			expectedErr: "TemplateID and BodyHTML/BodyText cannot be provided together",
		},
		{
			name: "Missing attachment content",
			req: &SendEmailRequest{
				To:        []string{"t@e.com"},
				Subject:   "S",
				FromEmail: "f@e.com",
				Attachments: []Attachment{
					{Filename: "test.txt", MimeType: "text/plain"},
				},
			},
			expectedErr: "Attachment must specify Content, ContentBytes, or FilePath",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			_, err := client.SendEmail(tt.req)
			if err == nil {
				t.Fatalf("Expected error %s, got none", tt.expectedErr)
			}
			if err.Error() != tt.expectedErr {
				t.Errorf("Expected error: %s, got: %s", tt.expectedErr, err.Error())
			}
		})
	}
}

func TestSendEmailAttachments(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var req SendEmailRequest
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			t.Fatalf("Failed to decode: %v", err)
		}

		if len(req.Attachments) != 2 {
			t.Fatalf("Expected 2 attachments, got %d", len(req.Attachments))
		}

		att1Base64 := base64.StdEncoding.EncodeToString([]byte("raw bytes"))
		if req.Attachments[0].Content != att1Base64 {
			t.Errorf("Attachment 1 content mismatch")
		}

		att2Base64 := base64.StdEncoding.EncodeToString([]byte("file bytes"))
		if req.Attachments[1].Content != att2Base64 {
			t.Errorf("Attachment 2 content mismatch")
		}

		w.WriteHeader(http.StatusOK)
		fmt.Fprintln(w, `{"message_id": "msg", "status": "sent"}`)
	}))
	defer server.Close()

	// Create temp file for attachment 2
	tmpDir, err := os.MkdirTemp("", "sculpt")
	if err != nil {
		t.Fatal(err)
	}
	defer os.RemoveAll(tmpDir)

	tmpFile := filepath.Join(tmpDir, "test.txt")
	err = os.WriteFile(tmpFile, []byte("file bytes"), 0644)
	if err != nil {
		t.Fatal(err)
	}

	client := NewClient("test-key", "sandbox")
	client.BaseURL = server.URL

	req := &SendEmailRequest{
		To:        []string{"t@e.com"},
		Subject:   "S",
		FromEmail: "f@e.com",
		Attachments: []Attachment{
			{
				Filename:     "att1.txt",
				ContentBytes: []byte("raw bytes"),
				MimeType:     "text/plain",
			},
			{
				Filename: "att2.txt",
				FilePath: tmpFile,
				MimeType: "text/plain",
			},
		},
	}

	_, err = client.SendEmail(req)
	if err != nil {
		t.Fatalf("Expected no error, got %v", err)
	}
}

func TestSendEmailAPIError(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintln(w, `{"detail": ["Missing field"]}`)
	}))
	defer server.Close()

	client := NewClient("test-key", "sandbox")
	client.BaseURL = server.URL

	req := &SendEmailRequest{
		To:        []string{"t@e.com"},
		Subject:   "S",
		FromEmail: "f@e.com",
	}

	_, err := client.SendEmail(req)
	if err == nil {
		t.Fatalf("Expected error, got none")
	}

	expectedPrefix := "SendSculpt API Error [400]"
	if !strings.HasPrefix(err.Error(), expectedPrefix) {
		t.Errorf("Expected error to start with %s, got %s", expectedPrefix, err.Error())
	}
}
