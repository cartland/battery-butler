require 'xcodeproj'
require 'set'

project_path = 'iosAppSwiftUI.xcodeproj'
project = Xcodeproj::Project.open(project_path)
target = project.targets.find { |t| t.name == 'iosAppSwiftUI' }

features_group = project.main_group.find_subpath('Features', false)
if features_group.nil?
  puts "Features group not found!"
  exit 1
end

# Sync subdirectories inside Features
Dir.glob('Features/*').each do |dir|
  next unless File.directory?(dir)
  group_name = File.basename(dir)
  
  group = features_group.find_subpath(group_name, false) || features_group.new_group(group_name, group_name)
  
  # Find all Swift files on disk
  disk_files = Dir.glob("#{dir}/*.swift").map { |f| File.basename(f) }.to_set
  
  # Find all Swift files in group
  group_files = group.files.map { |f| f.path }.to_set
  
  # Add missing
  (disk_files - group_files).each do |filename|
    file_ref = group.new_file(filename)
    target.add_file_references([file_ref]) unless target.source_build_phase.files_references.include?(file_ref)
    puts "Added #{group_name}/#{filename}"
  end
  
  # We won't strictly remove missing right now unless needed, 
  # but if there are files in group not on disk, we should remove them!
  group.files.each do |f|
    if !disk_files.include?(f.path)
      puts "Removing missing file reference #{group_name}/#{f.path}"
      f.remove_from_project
    end
  end
end

project.save
puts "Synced Features folder with Xcode project."

# Sync subdirectories inside Core
core_group = project.main_group.find_subpath('Core', false) || project.main_group.new_group('Core', 'Core')

Dir.glob('Core/*').each do |dir|
  next unless File.directory?(dir)
  group_name = File.basename(dir)

  group = core_group.find_subpath(group_name, false) || core_group.new_group(group_name, group_name)

  disk_files = Dir.glob("#{dir}/*.swift").map { |f| File.basename(f) }.to_set
  group_files = group.files.map { |f| f.path }.to_set

  (disk_files - group_files).each do |filename|
    file_ref = group.new_file(filename)
    target.add_file_references([file_ref]) unless target.source_build_phase.files_references.include?(file_ref)
    puts "Added Core/#{group_name}/#{filename}"
  end

  group.files.each do |f|
    if !disk_files.include?(f.path)
      puts "Removing missing file reference Core/#{group_name}/#{f.path}"
      f.remove_from_project
    end
  end
end

# Also sync loose files in Core/ (like ViewModelWrapper.swift)
core_disk_files = Dir.glob("Core/*.swift").map { |f| File.basename(f) }.to_set
core_group_files = core_group.files.map { |f| f.path }.to_set

(core_disk_files - core_group_files).each do |filename|
  file_ref = core_group.new_file(filename)
  target.add_file_references([file_ref]) unless target.source_build_phase.files_references.include?(file_ref)
  puts "Added Core/#{filename}"
end

core_group.files.each do |f|
  if f.path.end_with?('.swift') && !core_disk_files.include?(f.path)
    puts "Removing missing file reference Core/#{f.path}"
    f.remove_from_project
  end
end

project.save
puts "Synced Core folder with Xcode project."

# Also sync the tests directory
test_target = project.targets.find { |t| t.name == 'iosAppSwiftUITests' }
if test_target
  tests_group = project.main_group.find_subpath('iosAppSwiftUITests', false) || project.main_group.new_group('iosAppSwiftUITests', 'iosAppSwiftUITests')
  
  Dir.mkdir('iosAppSwiftUITests') unless Dir.exist?('iosAppSwiftUITests')
  
  test_disk_files = Dir.glob("iosAppSwiftUITests/*.swift").map { |f| File.basename(f) }.to_set
  test_group_files = tests_group.files.map { |f| f.path }.to_set
  
  (test_disk_files - test_group_files).each do |filename|
    file_ref = tests_group.new_file(filename)
    test_target.add_file_references([file_ref]) unless test_target.source_build_phase.files_references.include?(file_ref)
    puts "Added iosAppSwiftUITests/#{filename}"
  end
  
  tests_group.files.each do |f|
    if f.path.end_with?('.swift') && !test_disk_files.include?(f.path)
      puts "Removing missing test file reference #{f.path}"
      f.remove_from_project
    end
  end
  
  project.save
  puts "Synced tests folder with Xcode project."
end
