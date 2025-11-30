// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.internal.utils

import com.autonomousapps.internal.utils.Files.asSequenceOfClassFiles
import com.google.common.truth.Truth
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class FilesTest {

  @TempDir
  lateinit var tempDir: File

  @Test fun `can get relative path`() {
    // Given an absolute path
    val original = File("/Users/me/my-project/foo/bar/baz/build/classes/kotlin/main/com/example/MyClass.class")

    // When we relativize it
    val actual = Files.relativize(original, "build/classes/kotlin/main/")

    // Then we get
    Truth.assertThat(actual).isEqualTo("com/example/MyClass.class")
  }

  @Test fun `can get package path`() {
    // Given an absolute path
    val original = File("/Users/me/my-project/foo/bar/baz/build/classes/kotlin/main/com/example/MyClass.class")

    // When we relativize it
    val actual = Files.asPackagePath(original)

    // Then we get
    Truth.assertThat(actual).isEqualTo("com/example/MyClass.class")
  }

  @Test fun `asSequenceOfClassFiles throws for non-directory`() {
    // Given a file (not directory)
    val file = File("some-file.txt")

    // When/Then it throws
    assertThrows<IllegalStateException> {
      file.asSequenceOfClassFiles()
    }
  }

  @Test fun `asSequenceOfClassFiles returns class files excluding module-info`() {
    // Given a directory with various files
    val classFile = dummyFile("Bar.class")
    val nestedClassFile = dummyFile("nested/Baz.class")
    dummyFile("module-info.class")
    dummyFile("META-INF/services/com.example.Service")

    // When we get class files
    val actual = tempDir.asSequenceOfClassFiles().toList()

    // Then we get
    Truth.assertThat(actual).containsExactly(classFile, nestedClassFile)
  }

  private fun dummyFile(path: String): File = File(tempDir, path).also {
    it.parentFile.mkdirs()
    it.writeText("dummy")
  }
}
