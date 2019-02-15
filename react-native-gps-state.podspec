require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name         = "react-native-gps-state"
  s.version      = package["version"]
  s.summary      = package["description"]
  s.description  = package["description"]
  s.license      = "MIT"
  s.author       = package["author"]
  s.homepage     = "https://github.com/neuberoliveira/react-native-gps-state"
  s.requires_arc = true
  s.platform     = :ios, '9.0'

  s.source       = { :git => "https://github.com/neuberoliveira/react-native-gps-state.git", :tag => "#{s.version}" }

  s.source_files = "ios/**/*.{h,m}"

  s.dependency "React"
end
