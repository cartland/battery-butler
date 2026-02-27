require 'xcodeproj'

project_path = 'iosAppSwiftUI.xcodeproj'
project = Xcodeproj::Project.open(project_path)

test_target_name = 'iosAppSwiftUITests'
app_target = project.targets.find { |t| t.name == 'iosAppSwiftUI' }

test_target = project.targets.find { |t| t.name == test_target_name }
if test_target.nil?
  puts "Creating test target..."
  test_target = project.new_target(:unit_test_bundle, test_target_name, :ios)
  
  test_target.add_dependency(app_target)
  
  project.build_configurations.each do |config|
    test_config = test_target.build_settings(config.name)
    test_config['TEST_HOST'] = "$(BUILT_PRODUCTS_DIR)/BatteryButler.app/BatteryButler"
    test_config['BUNDLE_LOADER'] = "$(TEST_HOST)"
    test_config['PRODUCT_BUNDLE_IDENTIFIER'] = "com.chriscartland.batterybutler.iosAppSwiftUITests"
    test_config['INFOPLIST_FILE'] = "iosAppSwiftUITests/Info.plist"
    test_config['SWIFT_VERSION'] = "5.0"
    test_config['IPHONEOS_DEPLOYMENT_TARGET'] = "16.0"
  end
end

puts "Adding SnapshotTesting package..."
pkg_url = 'https://github.com/pointfreeco/swift-snapshot-testing.git'
pkg_ref = project.root_object.package_references.find { |pkg| pkg.repositoryURL == pkg_url }
if pkg_ref.nil?
  pkg_ref = project.new(Xcodeproj::Project::Object::XCRemoteSwiftPackageReference)
  pkg_ref.repositoryURL = pkg_url
  pkg_ref.requirement = {
    "kind" => "upToNextMajorVersion",
    "minimumVersion" => "1.17.0"
  }
  project.root_object.package_references << pkg_ref
end

dep = test_target.package_product_dependencies.find { |d| d.product_name == 'SnapshotTestingInline' }
if dep.nil?
  dep = project.new(Xcodeproj::Project::Object::XCSwiftPackageProductDependency)
  dep.product_name = 'SnapshotTesting'
  dep.package = pkg_ref
  test_target.package_product_dependencies << dep
end

tests_group = project.main_group.find_subpath(test_target_name, false)
if tests_group.nil?
  tests_group = project.main_group.new_group(test_target_name, test_target_name)
end

project.save
puts "Done."
