import requests
import base64
import os
from typing import List, Optional, Dict, Any, Union


class SendSculptClient:
    """
    Client for interacting with the SendSculpt Mailer API.
    """

    def __init__(self, api_key: str, environment: str = "live"):
        """
        Initialize the SendSculptClient.

        :param api_key: Your SendSculpt API key.
        :param environment: The environment to use ("live" or "sandbox").
        """
        self.api_key = api_key
        self.environment = environment
        self.base_url = "https://api.sendsculpt.com/api/v1"
        self.headers = {
            "x-sendsculpt-key": self.api_key,
            "Content-Type": "application/json",
        }

    def send_email(
        self,
        to: List[str],
        subject: str,
        from_email: str,
        body_html: Optional[str] = None,
        body_text: Optional[str] = None,
        cc: Optional[List[str]] = None,
        bcc: Optional[List[str]] = None,
        template_id: Optional[str] = None,
        template_data: Optional[Dict[str, Any]] = None,
        reply_to: Optional[List[str]] = None,
        attachments: Optional[List[Dict[str, str]]] = None,
        sender_name: Optional[str] = None,
    ) -> Dict[str, Any]:
        """
        Send an email via the SendSculpt API.

        :param to: List of recipient email addresses.
        :param subject: Email subject line.
        :param from_email: Sender email address (must be a registered/verified domain).
        :param body_html: HTML body of the email.
        :param body_text: Plain text body of the email.
        :param cc: List of CC email addresses.
        :param bcc: List of BCC email addresses.
        :param template_id: The ID of an existing SendSculpt template.
        :param template_data: Context variables for Jinja2 template rendering.
        :param reply_to: List of Reply-To email addresses.
        :param attachments: List of attachment dictionaries containing filename, content (base64 str), and mime_type.
        :param sender_name: Optional friendly sender name.
        :return: Response dictionary from the API (e.g., {"message_id": "...", "status": "sent"}).
        """

        # Validation
        if template_data and not template_id:
            raise ValueError("template_data and template_id must be provided together.")
        if template_id and (body_html or body_text):
            raise ValueError(
                "template_id and body_html/body_text cannot be provided together."
            )

        # Attachment Formatting
        formatted_attachments = None
        if attachments is not None:
            formatted_attachments = []
            for att in attachments:
                fmt_att = {
                    "filename": att.get("filename"),
                    "mime_type": att.get("mime_type"),
                }

                # If they provided raw base64 content
                if "content" in att:
                    fmt_att["content"] = att["content"]
                # If they provided raw bytes
                elif "content_bytes" in att:
                    fmt_att["content"] = base64.b64encode(att["content_bytes"]).decode(
                        "utf-8"
                    )
                # If they provided a file path
                elif "file_path" in att:
                    if not os.path.exists(att["file_path"]):
                        raise FileNotFoundError(
                            f"Attachment file not found: {att['file_path']}"
                        )
                    with open(att["file_path"], "rb") as f:
                        fmt_att["content"] = base64.b64encode(f.read()).decode("utf-8")
                else:
                    raise ValueError(
                        "Attachment must specify 'content' (base64 string), 'content_bytes' (bytes), or 'file_path' (string)."
                    )

                formatted_attachments.append(fmt_att)

        # Build payload
        payload = {
            "to": to,
            "subject": subject,
            "from_email": from_email,
        }

        if body_html is not None:
            payload["body_html"] = body_html
        if body_text is not None:
            payload["body_text"] = body_text
        if cc is not None:
            payload["cc"] = cc
        if bcc is not None:
            payload["bcc"] = bcc
        if template_id is not None:
            payload["template_id"] = template_id
        if template_data is not None:
            payload["template_data"] = template_data
        if reply_to is not None:
            payload["reply_to"] = reply_to
        if formatted_attachments is not None:
            payload["attachments"] = formatted_attachments
        if sender_name is not None:
            payload["sender_name"] = sender_name

        payload["environment"] = self.environment

        endpoint = f"{self.base_url}/send"
        response = requests.post(endpoint, json=payload, headers=self.headers)

        response.raise_for_status()
        return response.json()
