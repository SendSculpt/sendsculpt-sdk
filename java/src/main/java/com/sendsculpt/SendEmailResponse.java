package com.sendsculpt;

public class SendEmailResponse {
    private String messageId;
    private String status;

    // Getters and Setters matching snake_case JSON logic
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
