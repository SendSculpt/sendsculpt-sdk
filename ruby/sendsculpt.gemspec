Gem::Specification.new do |spec|
  spec.name          = "sendsculpt-sdk"
  spec.version       = "1.0.0"
  spec.authors       = ["SendSculpt Team"]
  spec.email         = ["support@sendsculpt.com"]

  spec.summary       = "SDK for SendSculpt Mailer API"
  spec.description   = "The official Ruby client for sending emails via the SendSculpt Mailer API."
  spec.homepage      = "https://github.com/sendsculpt/sendsculpt-sdk/tree/main/ruby"
  spec.license       = "MIT"

  spec.files         = Dir["lib/**/*.rb", "README.md"]
  spec.require_paths = ["lib"]

  spec.required_ruby_version = ">= 2.6.0"

  spec.add_development_dependency "bundler", "~> 2.0"
  spec.add_development_dependency "rake", "~> 13.0"
end
