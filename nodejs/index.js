const axios = require("axios");
const fs = require("fs");
const path = require("path");

class SendSculptClient {
    /**
     * Initialize the SendSculptClient.
     *
     * @param {string} apiKey - Your SendSculpt API key.
     * @param {"live" | "sandbox"} environment - The environment to use ("live" or "sandbox").
     */
    constructor(apiKey, environment = "live") {
        if (!apiKey) {
            throw new Error("API Key is required to initialize SendSculptClient.");
        }
        this.apiKey = apiKey;
        this.environment = environment;
        this.baseUrl = "https://api.sendsculpt.com/api/v1";

        this.client = axios.create({
            baseURL: this.baseUrl,
            headers: {
                "x-sendsculpt-key": this.apiKey,
                "Content-Type": "application/json"
            }
        });
    }

    /**
     * Send an email via the SendSculpt API.
     *
     * @param {Object} options - Email sending options.
     * @param {string[]} options.to - List of recipient email addresses.
     * @param {string} options.subject - Email subject line.
     * @param {string} options.fromEmail - Sender email address.
     * @param {string} [options.bodyHtml] - HTML body of the email.
     * @param {string} [options.bodyText] - Plain text body of the email.
     * @param {string[]} [options.cc] - List of CC email addresses.
     * @param {string[]} [options.bcc] - List of BCC email addresses.
     * @param {string} [options.templateId] - The ID of an existing SendSculpt template.
     * @param {Object} [options.templateData] - Context variables for Jinja2 template rendering.
     * @param {string[]} [options.replyTo] - List of Reply-To email addresses.
     * @param {Object[]} [options.attachments] - List of attachment objects containing filename, content, and mime_type.
     * @param {string} [options.senderName] - Optional friendly sender name.
     * @returns {Promise<Object>} API response including message_id.
     */
    async sendEmail(options) {
        const {
            to,
            subject,
            fromEmail, // mapped to from_email in payload
            bodyHtml,
            bodyText,
            cc,
            bcc,
            templateId,
            templateData,
            replyTo,
            attachments,
            senderName
        } = options;

        if (!to || !to.length) throw new Error('"to" is required and must be an array of emails.');
        if (!subject) throw new Error('"subject" is required.');
        if (!fromEmail) throw new Error('"fromEmail" is required.');

        if (templateData && !templateId) {
            throw new Error("templateData and templateId must be provided together.");
        }
        if (templateId && (bodyHtml || bodyText)) {
            throw new Error("templateId and bodyHtml/bodyText cannot be provided together.");
        }

        // Attachment Auto-Conversion
        let formattedAttachments = undefined;
        if (attachments && Array.isArray(attachments)) {
            formattedAttachments = attachments.map((att) => {
                const fmtAtt = { filename: att.filename, mime_type: att.mimeType || att.mime_type };

                if (att.content) {
                    fmtAtt.content = att.content; // Already base64 string
                } else if (att.buffer) {
                    fmtAtt.content = att.buffer.toString("base64");
                } else if (att.filePath) {
                    const absolutePath = path.resolve(att.filePath);
                    if (!fs.existsSync(absolutePath)) {
                        throw new Error(`Attachment file not found: ${absolutePath}`);
                    }
                    fmtAtt.content = fs.readFileSync(absolutePath, { encoding: "base64" });
                } else {
                    throw new Error("Attachment must specify 'content' (base64 string), 'buffer' (Buffer), or 'filePath' (string).");
                }

                return fmtAtt;
            });
        }

        const payload = {
            to,
            subject,
            from_email: fromEmail
        };

        if (bodyHtml) payload.body_html = bodyHtml;
        if (bodyText) payload.body_text = bodyText;
        if (cc) payload.cc = cc;
        if (bcc) payload.bcc = bcc;
        if (templateId) payload.template_id = templateId;
        if (templateData) payload.template_data = templateData;
        if (replyTo) payload.reply_to = replyTo;
        if (formattedAttachments) payload.attachments = formattedAttachments;
        if (senderName) payload.sender_name = senderName;

        payload.environment = this.environment;

        try {
            const response = await this.client.post("/send", payload);
            return response.data;
        } catch (error) {
            if (error.response) {
                throw new Error(`SendSculpt API Error [${error.response.status}]: ${JSON.stringify(error.response.data)}`);
            }
            throw error;
        }
    }
}

module.exports = SendSculptClient;
