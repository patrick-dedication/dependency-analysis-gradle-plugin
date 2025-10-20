// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.tasks

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class InternalAccessAnalysisTaskTest {

  @TempDir
  lateinit var testProjectDir: File

  @Test
  fun `internal access analysis task should execute successfully`() {
    // Given a simple multi-module project
    val settingsFile = File(testProjectDir, "settings.gradle.kts")
    settingsFile.writeText("""
      rootProject.name = "test-project"
      include(":lib1", ":lib2")
    """.trimIndent())

    val buildFile = File(testProjectDir, "build.gradle.kts")
    buildFile.writeText("""
      plugins {
        id("com.autonomousapps.dependency-analysis") version "3.1.1-SNAPSHOT"
        `java-library`
      }
    """.trimIndent())

    // Create lib1
    val lib1Dir = File(testProjectDir, "lib1")
    lib1Dir.mkdirs()
    val lib1BuildFile = File(lib1Dir, "build.gradle.kts")
    lib1BuildFile.writeText("""
      plugins {
        `java-library`
      }
    """.trimIndent())

    // Create lib2 that depends on lib1
    val lib2Dir = File(testProjectDir, "lib2")
    lib2Dir.mkdirs()
    val lib2BuildFile = File(lib2Dir, "build.gradle.kts")
    lib2BuildFile.writeText("""
      plugins {
        `java-library`
      }

      dependencies {
        implementation(project(":lib1"))
      }
    """.trimIndent())

    // Create source files
    val lib1MainDir = File(lib1Dir, "src/main/java/com/example/internal")
    lib1MainDir.mkdirs()

    val internalClass = File(lib1MainDir, "InternalClass.java")
    internalClass.writeText("""
      package com.example.internal;

      public class InternalClass {
        public void doSomething() {
          System.out.println("Internal work");
        }
      }
    """.trimIndent())

    val lib2MainDir = File(lib2Dir, "src/main/java/com/example/public")
    lib2MainDir.mkdirs()

    val publicClass = File(lib2MainDir, "PublicClass.java")
    publicClass.writeText("""
      package com.example.public;

      import com.example.internal.InternalClass;

      public class PublicClass {
        public void useInternal() {
          InternalClass internal = new InternalClass();
          internal.doSomething();
        }
      }
    """.trimIndent())

    // When running the internal access analysis task
    val result = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withPluginClasspath()
      .withArguments("lib2:internalAccessAnalysisMain", "--stacktrace")
      .build()

    // Then the task should succeed
    assertEquals(TaskOutcome.SUCCESS, result.task(":lib2:internalAccessAnalysisMain")?.outcome)

    // And the task should have generated an output file
    val outputFile = File(testProjectDir, "lib2/build/reports/dependency-analysis/main/intermediates/internal-access-analysis.txt")
    assertTrue(outputFile.exists())

    val outputContent = outputFile.readText()
    assertTrue(outputContent.contains("Internal access analysis for :lib2"))
  }

  @Test
  fun `internal access analysis task should handle empty classpath`() {
    // Given a project with no dependencies
    val settingsFile = File(testProjectDir, "settings.gradle.kts")
    settingsFile.writeText("""
      rootProject.name = "test-project"
    """.trimIndent())

    val buildFile = File(testProjectDir, "build.gradle.kts")
    buildFile.writeText("""
      plugins {
        id("com.autonomousapps.dependency-analysis") version "3.1.1-SNAPSHOT"
        `java-library`
      }
    """.trimIndent())

    // When running the internal access analysis task
    val result = GradleRunner.create()
      .withProjectDir(testProjectDir)
      .withPluginClasspath()
      .withArguments("internalAccessAnalysisMain", "--stacktrace")
      .build()

    // Then the task should succeed
    assertEquals(TaskOutcome.SUCCESS, result.task(":internalAccessAnalysisMain")?.outcome)

    // And the task should have generated an output file
    val outputFile = File(testProjectDir, "build/reports/dependency-analysis/main/intermediates/internal-access-analysis.txt")
    assertTrue(outputFile.exists())

    val outputContent = outputFile.readText()
    assertTrue(outputContent.contains("Internal access analysis for"))
  }
}