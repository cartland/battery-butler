require 'xcodeproj'
project_path = 'iosAppSwiftUI.xcodeproj'
project = Xcodeproj::Project.open(project_path)
test_target = project.targets.find { |t| t.name == 'iosAppSwiftUITests' }
test_target.build_configurations.each do |config|
  config.build_settings['PRODUCT_NAME'] = '$(TARGET_NAME)'
  config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '18.2'
end
project.save
