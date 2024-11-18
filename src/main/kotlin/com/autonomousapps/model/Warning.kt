package com.autonomousapps.model

import com.autonomousapps.internal.utils.LexicographicIterableComparator
import com.squareup.moshi.JsonClass

/** Warnings about the project for which the plugin cannot yet make universally actionable advice. */
@JsonClass(generateAdapter = false)
data class Warning(
  val duplicateClasses: Set<DuplicateClass>,
) : Comparable<Warning> {

  internal companion object {
    @JvmStatic
    fun empty() = Warning(emptySet())
  }

  override fun compareTo(other: Warning): Int {
    return LexicographicIterableComparator<DuplicateClass>()
      .compare(duplicateClasses, other.duplicateClasses)
  }

  fun isEmpty(): Boolean = duplicateClasses.isEmpty()
  fun isNotEmpty(): Boolean = !isEmpty()
}