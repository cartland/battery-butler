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
