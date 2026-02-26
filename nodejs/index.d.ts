declare module "sendsculpt-sdk" {
    interface SendEmailOptions {
        to: string[];
        subject: string;
        fromEmail: string;
        bodyHtml?: string;
        bodyText?: string;
        cc?: string[];
        bcc?: string[];
        templateId?: string;
        templateData?: Record<string, any>;
        replyTo?: string[];
        attachments?: Attachment[];
        senderName?: string;
    }

    interface Attachment {
        filename: string;
        content?: string;
        buffer?: Buffer;
        filePath?: string;
        mimeType?: string;
    }

    interface SendEmailResponse {
        success: boolean;
        message: string;
        data?: {
            messageId: string;
            status: string;
            queuedAt: string;
        };
        error?: {
            code: string;
            message: string;
        };
    }

    export class SendSculptClient {
        constructor(apiKey: string, environment?: string);
        sendEmail(options: SendEmailOptions): Promise<SendEmailResponse>;
    }
}