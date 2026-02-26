require 'spec_helper'
require_relative '../lib/sendsculpt'
require 'base64'
require 'tempfile'

RSpec.describe Sendsculpt::Client do
  let(:api_key) { 'test-api-key' }
  let(:environment) { 'sandbox' }
  subject { described_class.new(api_key, environment) }

  # Test requests always go to the default base url now that it's hardcoded
  let(:base_url) { 'https://api.sendsculpt.com/api/v1' }

  before do
    WebMock.disable_net_connect!(allow_localhost: true)
  end

  describe '#send_email' do
    let(:success_response) { { message_id: 'test-msg-id', status: 'sent' }.to_json }

    it 'successfully sends an email via the API' do
      stub_request(:post, "#{base_url}/send")
        .with(
          body: hash_including({
            "to" => ["recipient@example.com"],
            "subject" => "Test Subject",
            "from_email" => "noreply@example.com"
          }),
          headers: {
            'Content-Type' => 'application/json',
            'x-sendsculpt-key' => api_key
          }
        )
        .to_return(status: 200, body: success_response)

      response = subject.send_email(
        to: ['recipient@example.com'],
        subject: 'Test Subject',
        from_email: 'noreply@example.com'
      )

      expect(response['message_id']).to eq('test-msg-id')
      expect(response['status']).to eq('sent')
    end

    it 'raises error if template_id and body given' do
      expect {
        subject.send_email(
          to: ['recipient@example.com'],
          subject: 'Test Subject',
          from_email: 'noreply@example.com',
          template_id: 'uuid',
          body_html: '<h1>Hello</h1>'
        )
      }.to raise_error(ArgumentError, /cannot be provided together/)
    end

    it 'encodes attachments correctly' do
      stub_request(:post, "#{base_url}/send").to_return(status: 200, body: success_response)

      att_content = Base64.strict_encode64('test_content')

      response = subject.send_email(
        to: ['recipient@example.com'],
        subject: 'Test Subject',
        from_email: 'noreply@example.com',
        attachments: [{
          filename: 'test.txt',
          content: att_content,
          mime_type: 'text/plain'
        }]
      )

      expect(response['message_id']).to eq('test-msg-id')
    end

    it 'reads and encodes filepath attachments correctly' do
      stub = stub_request(:post, "#{base_url}/send").to_return(status: 200, body: success_response)

      Tempfile.create('testfile') do |f|
        f.write('hello from ruby')
        f.rewind

        response = subject.send_email(
          to: ['recipient@example.com'],
          subject: 'Test Subject',
          from_email: 'noreply@example.com',
          attachments: [{
            filename: 'test.txt',
            file_path: f.path,
            mime_type: 'text/plain'
          }]
        )

        expect(response['message_id']).to eq('test-msg-id')
        
        expected_base64 = Base64.strict_encode64('hello from ruby')
        expect(stub.with(body: /#{expected_base64}/)).to have_been_made.once
      end
    end

    it 'raises error if file_path attachment does not exist' do
      expect {
        subject.send_email(
          to: ['recipient@example.com'],
          subject: 'Test Subject',
          from_email: 'noreply@example.com',
          attachments: [{
            filename: 'missing.txt',
            file_path: '/path/does/not/exist.txt',
            mime_type: 'text/plain'
          }]
        )
      }.to raise_error(Errno::ENOENT)
    end
  end
end
