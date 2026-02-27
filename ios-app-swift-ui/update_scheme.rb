require 'xcodeproj'

project_path = 'iosAppSwiftUI.xcodeproj'
project = Xcodeproj::Project.open(project_path)

test_target = project.targets.find { |t| t.name == 'iosAppSwiftUITests' }

scheme_path = Xcodeproj::XCScheme.shared_data_dir(project_path) + 'iosAppSwiftUI.xcscheme'
scheme = Xcodeproj::XCScheme.new(scheme_path)

found = false
scheme.test_action.testables.each do |testable|
  if testable.buildable_references.any? { |ref| ref.target_name == 'iosAppSwiftUITests' }
    found = true
  end
end

if !found
  test_ref = Xcodeproj::XCScheme::BuildableReference.new(test_target)
  testable = Xcodeproj::XCScheme::TestAction::TestableReference.new(test_ref)
  scheme.test_action.add_testable(testable)
  scheme.save!
  puts "Added test target to scheme."
end
