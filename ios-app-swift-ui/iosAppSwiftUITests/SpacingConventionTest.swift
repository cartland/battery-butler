import XCTest

/// Convention test: ensures SwiftUI views use Spacing/CornerRadius constants
/// instead of hardcoded numeric values in padding, spacing, and cornerRadius calls.
///
/// Scans Swift source files in Features/ and Core/ for patterns like:
///   .padding(16)          -> should be .padding(Spacing.standard)
///   VStack(spacing: 12)   -> should be VStack(spacing: Spacing.medium)
///   .cornerRadius(10)     -> should be .cornerRadius(CornerRadius.medium)
final class SpacingConventionTest: XCTestCase {

    func testNoHardcodedSpacingValues() throws {
        let projectRoot = findProjectRoot()
        let dirsToScan = [
            projectRoot + "/Features",
            projectRoot + "/Core"
        ]

        var violations: [String] = []

        // Files that define the constants themselves are excluded
        let excludedFiles = Set(["Spacing.swift", "IconSize.swift"])

        for dir in dirsToScan {
            let files = findSwiftFiles(in: dir)
            for file in files {
                let fileName = (file as NSString).lastPathComponent
                if excludedFiles.contains(fileName) { continue }

                guard let content = try? String(contentsOfFile: file, encoding: .utf8) else { continue }
                let lines = content.components(separatedBy: .newlines)

                for (index, line) in lines.enumerated() {
                    let lineNum = index + 1
                    let trimmed = line.trimmingCharacters(in: .whitespaces)

                    // Skip comments
                    if trimmed.hasPrefix("//") || trimmed.hasPrefix("*") || trimmed.hasPrefix("/*") {
                        continue
                    }

                    let fileViolations = checkLine(trimmed, file: fileName, lineNum: lineNum)
                    violations.append(contentsOf: fileViolations)
                }
            }
        }

        XCTAssertTrue(
            violations.isEmpty,
            "Found hardcoded spacing values. Use Spacing.* or CornerRadius.* constants instead:\n"
            + violations.joined(separator: "\n")
        )
    }

    // MARK: - Helpers

    private func findProjectRoot() -> String {
        // #file gives the path to this test file
        // Navigate up from iosAppSwiftUITests/ to the ios-app-swift-ui/ root
        let testFile = #file
        let testDir = (testFile as NSString).deletingLastPathComponent
        let projectRoot = (testDir as NSString).deletingLastPathComponent
        return projectRoot
    }

    private func findSwiftFiles(in directory: String) -> [String] {
        let fm = FileManager.default
        guard let enumerator = fm.enumerator(atPath: directory) else { return [] }

        var files: [String] = []
        while let path = enumerator.nextObject() as? String {
            if path.hasSuffix(".swift") {
                files.append(directory + "/" + path)
            }
        }
        return files
    }

    private func checkLine(_ line: String, file: String, lineNum: Int) -> [String] {
        var violations: [String] = []

        // Pattern: .padding(<number>) or .padding(.<edge>, <number>)
        // Matches: .padding(16), .padding(.top, 40), .padding(.vertical, 4)
        // Does NOT match: .padding(), .padding(Spacing.standard), .padding(.horizontal, Spacing.standard)
        if let range = line.range(of: #"\.padding\([^)]*\)"#, options: .regularExpression) {
            let match = String(line[range])
            // Check if it contains a bare number (not preceded by a word character like Spacing.*)
            if match.range(of: #"[\(,]\s*\d+\.?\d*\s*\)"#, options: .regularExpression) != nil {
                // Allow .padding(.top, 40) on LoginScreen (unique branding layout)
                if !line.contains(".top, 40") {
                    violations.append("  \(file):\(lineNum): \(match) -> use Spacing.* constant")
                }
            }
        }

        // Pattern: spacing: <number> (in VStack, HStack, LazyVStack, etc.)
        // Allow spacing: 0 (semantically "no spacing", not a design-system value)
        if line.range(of: #"spacing:\s*[1-9]\d*\.?\d*"#, options: .regularExpression) != nil {
            // Only flag if it's not already using a constant
            if line.range(of: #"spacing:\s*Spacing\."#, options: .regularExpression) == nil {
                violations.append("  \(file):\(lineNum): hardcoded spacing value -> use Spacing.* constant")
            }
        }

        // Pattern: .cornerRadius(<number>)
        if let range = line.range(of: #"\.cornerRadius\([^)]*\)"#, options: .regularExpression) {
            let match = String(line[range])
            if match.range(of: #"\(\s*\d+\.?\d*\s*\)"#, options: .regularExpression) != nil {
                violations.append("  \(file):\(lineNum): \(match) -> use CornerRadius.* constant")
            }
        }

        return violations
    }
}
