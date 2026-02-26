const SendSculptClient = require("../index.js");
const nock = require("nock");
const fs = require("fs");
const path = require("path");
const os = require("os");

describe("SendSculptClient", () => {
    const API_KEY = "test-api-key";
    const BASE_URL = "https://api.sendsculpt.com/api/v1";
    let client;

    beforeEach(() => {
        client = new SendSculptClient(API_KEY, "sandbox");
        nock.cleanAll();
    });

    test("throws if missing API key", () => {
        expect(() => {
            new SendSculptClient();
        }).toThrow("API Key is required to initialize SendSculptClient.");
    });

    test("successfully sends an email via the API", async () => {
        const payload = {
            message_id: "test-msg-id"
        };

        nock(BASE_URL)
            .post("/send", (body) => {
                expect(body.to).toEqual(["recipient@example.com"]);
                expect(body.subject).toBe("Test Subject");
                expect(body.from_email).toBe("noreply@example.com");
                expect(body.body_text).toBe("Hello World");
                return true;
            })
            .reply(200, payload);

        const response = await client.sendEmail({
            to: ["recipient@example.com"],
            subject: "Test Subject",
            fromEmail: "noreply@example.com",
            bodyText: "Hello World"
        });

        expect(response.message_id).toBe("test-msg-id");
    });

    test("validates templateId vs templateData", async () => {
        await expect(
            client.sendEmail({
                to: ["recipient@example.com"],
                subject: "Test Subject",
                fromEmail: "noreply@example.com",
                templateData: { foo: "bar" }
            })
        ).rejects.toThrow("templateData and templateId must be provided together.");

        await expect(
            client.sendEmail({
                to: ["recipient@example.com"],
                subject: "Test Subject",
                fromEmail: "noreply@example.com",
                templateId: "uuid",
                bodyText: "Hello"
            })
        ).rejects.toThrow("templateId and bodyHtml/bodyText cannot be provided together.");
    });

    test("encodes raw content attachment correctly", async () => {
        const payload = { message_id: "test-msg-id" };
        const rawContent = Buffer.from("test content").toString("base64");

        nock(BASE_URL)
            .post("/send", (body) => {
                expect(body.attachments[0].filename).toBe("test.txt");
                expect(body.attachments[0].content).toBe(rawContent);
                expect(body.attachments[0].mime_type).toBe("text/plain");
                return true;
            })
            .reply(200, payload);

        const response = await client.sendEmail({
            to: ["recipient@example.com"],
            subject: "Test Subject",
            fromEmail: "noreply@example.com",
            bodyText: "Hello World",
            attachments: [
                {
                    filename: "test.txt",
                    content: rawContent,
                    mimeType: "text/plain"
                }
            ]
        });

        expect(response.message_id).toBe("test-msg-id");
    });

    test("reads and encodes filePath attachment correctly", async () => {
        const payload = { message_id: "test-msg-id" };

        const testFile = path.join(os.tmpdir(), "test_attachment.txt");
        fs.writeFileSync(testFile, "hello from test");

        const expectedBase64 = Buffer.from("hello from test").toString("base64");

        nock(BASE_URL)
            .post("/send", (body) => {
                expect(body.attachments[0].filename).toBe("test.txt");
                expect(body.attachments[0].content).toBe(expectedBase64);
                return true;
            })
            .reply(200, payload);

        const response = await client.sendEmail({
            to: ["recipient@example.com"],
            subject: "Test Subject",
            fromEmail: "noreply@example.com",
            bodyText: "Hello World",
            attachments: [
                {
                    filename: "test.txt",
                    filePath: testFile,
                    mimeType: "text/plain"
                }
            ]
        });

        expect(response.message_id).toBe("test-msg-id");

        fs.unlinkSync(testFile);
    });

    test("throws if filePath attachment does not exist", async () => {
        await expect(
            client.sendEmail({
                to: ["recipient@example.com"],
                subject: "Test",
                fromEmail: "noreply@example.com",
                bodyText: "Hello",
                attachments: [
                    {
                        filename: "missing.txt",
                        filePath: "/path/that/does/not/exist.txt",
                        mimeType: "text/plain"
                    }
                ]
            })
        ).rejects.toThrow("Attachment file not found:");
    });
});
