# Testing Strategy & Plan

## Overview
This document outlines the testing strategy for the Battery Butler project, ensuring high quality across all layers of the Kotlin Multiplatform architecture.

## Test Layers

### 1. Unit Tests
- **Modules**: `domain`, `data`, `data-local`, `data-network`, `viewmodel`, `usecase`, `presentation-model`.
- **Purpose**: Verify isolated logic, business rules, and data transformations.
- **Location**: `commonTest`, `androidUnitTest`, `iosTest`.
- **Tools**: JUnit 4/5, Kotlin Test, MockK/Mockative.

### 2. Integration Tests
- **Modules**: `e2e-tests`.
- **Purpose**: Verify interactions between modules and deeper flows.
- **Scope**: Covers critical user journeys and API integrations (where applicable/mocked).

### 3. Screenshot Tests
- **Module**: `android-screenshot-tests`.
- **Purpose**: Detect visual regressions in UI components across different states and configurations.
- **Tools**: Android Screenshot Testing library (or similar).
- **Process**:
  - Run `./metrics/update_screenshots.sh` to baseline.
  - Run `./metrics/verify_screenshots.sh` to validate.

### 4. Test Infrastructure
- **`fixtures`**: Provides shared test data and object mothers to keep tests DRY and consistent.
- **`test-common`**: Contains shared test utilities, rules, and extensions used across multiple test modules.

## Continuous Integration
Tests are run automatically on CI for every Pull Request.
- `./gradlew check`: Runs all unit tests and lint checks.
- `./scripts/validate.sh`: Comprehensive local validation script.

## Future Improvements
- [ ] Add UI tests for iOS (XCTest).
- [ ] Expand coverage for `server` module.
- [ ] Automate performance testing.
