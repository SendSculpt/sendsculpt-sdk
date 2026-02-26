import pytest
import responses
import base64
import os
from sendsculpt import SendSculptClient

API_KEY = "test-api-key"
BASE_URL = "https://api.sendsculpt.com/api/v1"


@pytest.fixture
def client():
    return SendSculptClient(api_key=API_KEY, environment="sandbox")


@responses.activate
def test_send_email_success(client):
    responses.add(
        responses.POST,
        f"{BASE_URL}/send",
        json={"message_id": "test-msg-id", "status": "sent"},
        status=200,
    )

    response = client.send_email(
        to=["recipient@example.com"],
        subject="Test Subject",
        from_email="noreply@example.com",
        body_text="Hello World",
    )

    assert response["message_id"] == "test-msg-id"
    assert response["status"] == "sent"

    # Verify request payload
    assert len(responses.calls) == 1
    req = responses.calls[0].request
    assert req.headers["x-sendsculpt-key"] == API_KEY


def test_template_validation(client):
    with pytest.raises(
        ValueError, match="template_data and template_id must be provided together"
    ):
        client.send_email(
            to=["recipient@example.com"],
            subject="Test",
            from_email="noreply@example.com",
            template_data={"foo": "bar"},
        )

    with pytest.raises(
        ValueError,
        match="template_id and body_html/body_text cannot be provided together",
    ):
        client.send_email(
            to=["recipient@example.com"],
            subject="Test",
            from_email="noreply@example.com",
            template_id="uuid",
            body_text="Hello",
        )


@responses.activate
def test_send_email_with_raw_attachment(client):
    responses.add(
        responses.POST,
        f"{BASE_URL}/send",
        json={"message_id": "test-msg-id", "status": "sent"},
        status=200,
    )

    att_content = base64.b64encode(b"test content").decode("utf-8")

    response = client.send_email(
        to=["recipient@example.com"],
        subject="Test Subject",
        from_email="noreply@example.com",
        body_text="Hello World",
        attachments=[
            {"filename": "test.txt", "content": att_content, "mime_type": "text/plain"}
        ],
    )

    assert response["message_id"] == "test-msg-id"


@responses.activate
def test_send_email_with_filepath_attachment(client, tmp_path):
    responses.add(
        responses.POST,
        f"{BASE_URL}/send",
        json={"message_id": "test-msg-id", "status": "sent"},
        status=200,
    )

    # Create dummy file
    d = tmp_path / "sub"
    d.mkdir()
    p = d / "hello.txt"
    p.write_text("hello world")

    response = client.send_email(
        to=["recipient@example.com"],
        subject="Test Subject",
        from_email="noreply@example.com",
        body_text="Hello World",
        attachments=[
            {"filename": "hello.txt", "file_path": str(p), "mime_type": "text/plain"}
        ],
    )

    assert response["message_id"] == "test-msg-id"


def test_attachment_file_not_found(client):
    with pytest.raises(FileNotFoundError):
        client.send_email(
            to=["recipient@example.com"],
            subject="Test",
            from_email="noreply@example.com",
            attachments=[
                {
                    "filename": "missing.txt",
                    "file_path": "/path/does/not/exist.txt",
                    "mime_type": "text/plain",
                }
            ],
        )
