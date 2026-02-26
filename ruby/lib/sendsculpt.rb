require 'net/http'
require 'uri'
require 'json'
require 'base64'

module Sendsculpt
  class Client
    def initialize(api_key, base_url = 'https://api.sendsculpt.com/api/v1')
      @api_key = api_key
      @base_url = base_url
    end

    def send_email(options = {})
      if options[:template_id] && (options[:body_html] || options[:body_text])
        raise ArgumentError, 'template_id and body_html/body_text cannot be provided together.'
      end

      # Format attachments
      formatted_attachments = nil
      if options[:attachments] && options[:attachments].is_a?(Array)
        formatted_attachments = options[:attachments].map do |att|
          fmt_att = {
            filename: att[:filename],
            mime_type: att[:mime_type]
          }

          if att[:content]
            fmt_att[:content] = att[:content]
          elsif att[:content_bytes]
            fmt_att[:content] = Base64.strict_encode64(att[:content_bytes])
          elsif att[:file_path]
            unless File.exist?(att[:file_path])
              raise Errno::ENOENT, "Attachment file not found: #{att[:file_path]}"
            end
            fmt_att[:content] = Base64.strict_encode64(File.read(att[:file_path]))
          else
            raise ArgumentError, "Attachment must specify 'content' (base64 string), 'content_bytes' (bytes), or 'file_path' (string)."
          end
          fmt_att
        end
      end

      payload = {
        to: options[:to],
        subject: options[:subject],
        from_email: options[:from_email]
      }

      payload[:body_html] = options[:body_html] if options[:body_html]
      payload[:body_text] = options[:body_text] if options[:body_text]
      payload[:cc] = options[:cc] if options[:cc]
      payload[:bcc] = options[:bcc] if options[:bcc]
      payload[:template_id] = options[:template_id] if options[:template_id]
      payload[:template_data] = options[:template_data] if options[:template_data]
      payload[:reply_to] = options[:reply_to] if options[:reply_to]
      payload[:attachments] = formatted_attachments if formatted_attachments
      payload[:sender_name] = options[:sender_name] if options[:sender_name]

      uri = URI.parse("#{@base_url}/send")
      http = Net::HTTP.new(uri.host, uri.port)
      http.use_ssl = (uri.scheme == 'https')

      request = Net::HTTP::Post.new(uri.request_uri)
      request['Content-Type'] = 'application/json'
      request['x-sendsculpt-key'] = @api_key
      request.body = payload.to_json

      response = http.request(request)

      if response.code.to_i >= 400
        raise "API Error: #{response.code} - #{response.body}"
      end

      JSON.parse(response.body)
    end
  end
end
