package com.sendsculpt;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class SendEmailRequest {
    private List<String> to;
    private String subject;
    private String fromEmail;
    private String bodyHtml;
    private String bodyText;
    private List<String> cc;
    private List<String> bcc;
    private String templateId;
    private Map<String, Object> templateData;
    private List<String> replyTo;
    private List<Attachment> attachments;
    private String senderName;

    public static class Attachment {
        private String filename;
        private String content; // Base64 encoded inside JSON payload
        
        @JsonIgnore
        private byte[] contentBytes;
        
        @JsonIgnore
        private String filePath;
        
        private String mimeType;

        // Getters & Setters
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        @JsonProperty("content")
        public String getContent() throws Exception { 
            if (this.content != null && !this.content.trim().isEmpty()) {
                return this.content;
            }
            if (this.contentBytes != null && this.contentBytes.length > 0) {
                return Base64.getEncoder().encodeToString(this.contentBytes);
            }
            if (this.filePath != null && !this.filePath.trim().isEmpty()) {
                byte[] fileContent = Files.readAllBytes(Path.of(this.filePath));
                return Base64.getEncoder().encodeToString(fileContent);
            }
            throw new IllegalArgumentException("Attachment must specify content, contentBytes, or filePath");
        }
        
        public void setContent(String content) { this.content = content; }
        public byte[] getContentBytes() { return contentBytes; }
        public void setContentBytes(byte[] contentBytes) { this.contentBytes = contentBytes; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    }

    // Getters
    public List<String> getTo() { return to; }
    public String getSubject() { return subject; }
    public String getFromEmail() { return fromEmail; }
    public String getBodyHtml() { return bodyHtml; }
    public String getBodyText() { return bodyText; }
    public List<String> getCc() { return cc; }
    public List<String> getBcc() { return bcc; }
    public String getTemplateId() { return templateId; }
    public Map<String, Object> getTemplateData() { return templateData; }
    public List<String> getReplyTo() { return replyTo; }
    public List<Attachment> getAttachments() { return attachments; }
    public String getSenderName() { return senderName; }

    // Setters
    public void setTo(List<String> to) { this.to = to; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setFromEmail(String fromEmail) { this.fromEmail = fromEmail; }
    public void setBodyHtml(String bodyHtml) { this.bodyHtml = bodyHtml; }
    public void setBodyText(String bodyText) { this.bodyText = bodyText; }
    public void setCc(List<String> cc) { this.cc = cc; }
    public void setBcc(List<String> bcc) { this.bcc = bcc; }
    public void setTemplateId(String templateId) { this.templateId = templateId; }
    public void setTemplateData(Map<String, Object> templateData) { this.templateData = templateData; }
    public void setReplyTo(List<String> replyTo) { this.replyTo = replyTo; }
    public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
}
