require 'xcodeproj'

project_path = 'iosAppSwiftUI.xcodeproj'
project = Xcodeproj::Project.open(project_path)

test_target = project.targets.find { |t| t.name == 'iosAppSwiftUITests' }
app_target = project.targets.find { |t| t.name == 'iosAppSwiftUI' }

scheme = Xcodeproj::XCScheme.new
scheme.add_build_target(app_target)
scheme.add_test_target(test_target)
scheme.save_as(project_path, 'iosAppSwiftUITests')
puts "Created scheme iosAppSwiftUITests"
