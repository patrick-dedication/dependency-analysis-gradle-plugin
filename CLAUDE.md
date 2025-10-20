# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **Dependency Analysis Gradle Plugin** - a Gradle plugin that analyzes dependency usage in Android and JVM projects. It identifies unused dependencies, used transitive dependencies that should be declared directly, incorrect dependency configurations, and unused annotation processors. The plugin provides auto-remediation capabilities and works with multi-project builds.

## Build Commands

### Basic Development Commands
- `./gradlew build` - Build the entire project
- `./gradlew test` - Run unit tests
- `./gradlew check` - Run all checks (including tests, linting, etc.)
- `./gradlew pubLocal` - Publish all local artifacts to Maven local (includes subprojects)
- `./gradlew installForFuncTest` - Install to Maven local for functional testing

### Functional Testing
- `./gradlew functionalTest` - Run full functional test suite against all supported AGP/Gradle versions (slow)
- `./gradlew functionalTest -DfuncTest.quick` - Run functional tests only against latest versions (fast)
- `./gradlew quickFunctionalTest` - Alias for quick functional tests
- `./gradlew qFT` - Short alias for quick functional tests
- `./gradlew functionalTest --tests TestName` - Run specific functional test

### Smoke Testing
- `./gradlew smokeTest` - Run smoke tests against latest release version

### Publishing
- `./gradlew publishToMavenCentral` - Publish to Maven Central
- `./gradlew publishPlugins` - Publish to Gradle Plugin Portal (releases only)
- `./gradlew publishEverywhere` - Publish to both Maven Central and Plugin Portal

## Architecture Overview

### Plugin Structure
The project consists of two main plugins:
1. **DependencyAnalysisPlugin** (`com.autonomousapps.dependency-analysis`) - Applied to individual projects
2. **BuildHealthPlugin** (`com.autonomousapps.build-health`) - Applied in settings script for whole builds

### Core Packages
- `com.autonomousapps.subplugin` - Plugin implementation (`ProjectPlugin`, `RootPlugin`, `RedundantJvmPlugin`)
- `com.autonomousapps.tasks` - Gradle tasks for dependency analysis
- `com.autonomousapps.internal` - Internal analysis logic and utilities
- `com.autonomousapps.model` - Data models for dependencies and analysis results
- `com.autonomousapps.services` - Gradle services for caching and configuration
- `com.autonomousapps.extension` - DSL extensions and configuration
- `com.autonomousapps.visitor` - Code visitors for bytecode analysis

### Key Tasks
- `ComputeAdviceTask` - Main analysis task that generates dependency advice
- `ReasonTask` - Explains why specific dependencies are recommended
- `RewriteTask` - Auto-fixes build files based on analysis
- `projectHealth`/`buildHealth` - Main entry points for running analysis

### Subprojects
- `graph-support` - Graph analysis utilities
- `variant-artifacts` - Android variant artifact handling
- `testkit` - Gradle TestKit support and testing utilities
- `build-logic` - Shared build logic and conventions

## Development Notes

### Test Structure
The project has multiple test source sets:
- `test` - Unit tests
- `functionalTest` - Integration tests with real Gradle projects
- `smokeTest` - End-to-end tests against plugin releases
- `commonTest` - Shared test utilities

### Functional Test Filtering
Functional tests can be filtered by package:
- `-DfuncTest.package=android` - Run only Android tests
- `-DfuncTest.package=jvm` - Run only JVM tests
- `-DfuncTest.package=all` - Run all tests (default)

### Version Compatibility
The plugin supports:
- Gradle: Minimum version defined in `gradle.properties` via `libs.versions.gradle.version.min`
- AGP: Versions between `AgpVersion.AGP_MIN` and `AgpVersion.AGP_MAX`
- Kotlin: Multiple versions for compatibility testing

### Shadowed Dependencies
The project includes relocated versions of some dependencies in `shadowed/` to avoid conflicts:
- `antlr` - ANTLR parser
- `asm-relocated` - ASM bytecode analysis library
- `kotlin-editor-relocated` - Kotlin editor utilities

## Testing Environment Setup

### Prerequisites
- JDK 8, 11, and 17
- Android SDK (set `ANDROID_HOME` environment variable)

### Android SDK Setup (macOS)
```bash
brew install android-commandlinetools
export ANDROID_HOME="/path/to/android-commandlinetools"
```

## Configuration

### Plugin Application
Apply the plugin in settings script for whole builds:
```kotlin
// settings.gradle.kts
plugins {
  id("com.autonomousapps.build-health") version "VERSION"
}
```

Or apply to specific projects:
```kotlin
// build.gradle.kts
plugins {
  id("com.autonomousapps.dependency-analysis")
}
```

### Key DSL Options
```kotlin
dependencyAnalysis {
  issues {
    all {
      onAny {
        severity("fail") // or "warn", "ignore"
      }
    }
  }
  reporting {
    printBuildHealth(true)
  }
}
```