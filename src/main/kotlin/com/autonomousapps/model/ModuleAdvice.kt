// Copyright (c) 2025. Tony Robalik.
// SPDX-License-Identifier: Apache-2.0
package com.autonomousapps.model

import com.autonomousapps.extension.Behavior
import com.autonomousapps.extension.anyMatches
import com.autonomousapps.internal.unsafeLazy
import com.autonomousapps.model.internal.intermediates.AndroidScoreVariant
import com.squareup.moshi.JsonClass
import dev.zacsweers.moshix.sealed.annotations.TypeLabel

@JsonClass(generateAdapter = false, generator = "sealed:type")
public sealed class ModuleAdvice : Comparable<ModuleAdvice> {

  public abstract val name: String

  internal fun shouldIgnore(behavior: Behavior): Boolean {
    return behavior.filter.anyMatches(name)
  }

  internal abstract fun isActionable(): Boolean

  override fun compareTo(other: ModuleAdvice): Int {
    if (this === other) return 0

    if (this is AndroidScore && other is AndroidScore) {
      return compareBy(AndroidScore::hasAndroidAssets)
        .thenBy(AndroidScore::hasAndroidRes)
        .thenBy(AndroidScore::usesAndroidClasses)
        .thenBy(AndroidScore::hasBuildConfig)
        .thenBy(AndroidScore::hasAndroidDependencies)
        .compare(this, other)
    }

    if (this is InternalAccessAdvice && other is InternalAccessAdvice) {
      return compareBy(InternalAccessAdvice::sourceProject)
        .thenBy(InternalAccessAdvice::targetProject)
        .compare(this, other)
    }

    // Different types - compare by name
    return name.compareTo(other.name)
  }

  internal companion object {
    /** Returns `true` if [moduleAdvice] is effectively empty or unactionable. */
    fun isEmpty(moduleAdvice: Set<ModuleAdvice>) = moduleAdvice.none { it.isActionable() }

    /** Returns `true` if [moduleAdvice] is in any way actionable. */
    fun isNotEmpty(moduleAdvice: Set<ModuleAdvice>) = !isEmpty(moduleAdvice)
  }
}

@TypeLabel("internal_access")
@JsonClass(generateAdapter = false)
public data class InternalAccessAdvice(
  val sourceProject: String,
  val targetProject: String,
  val internalAccesses: Set<InternalAccess>,
) : ModuleAdvice() {

  override val name: String = "internal-access"

  override fun isActionable(): Boolean = internalAccesses.isNotEmpty()

  @JsonClass(generateAdapter = false)
  public data class InternalAccess(
    val accessedClass: String,
    val accessedMember: String?,
    val accessType: AccessType,
    val isInternalApi: Boolean = false,
    val recommendation: Recommendation,
  ) {

    public enum class AccessType {
      METHOD_CALL,
      FIELD_ACCESS,
      CLASS_REFERENCE,
      CONSTRUCTOR_CALL,
      INTERFACE_IMPLEMENTATION,
      CLASS_EXTENSION
    }

    public enum class Recommendation {
      /** The accessed class should be moved to a shared module or made public API */
      EXPOSE_AS_API,
      /** The accessing code should be refactored to avoid internal access */
      REFACTOR_ACCESS,
      /** This internal access is acceptable (whitelisted) */
      ACCEPTABLE,
      /** Requires manual review to determine appropriate action */
      MANUAL_REVIEW
    }

    public fun getDisplayDescription(): String = buildString {
      append("Accesses internal ${accessType.name.lowercase().replace('_', ' ')}")
      append(" of $accessedClass")
      accessedMember?.let { append(".$it") }
      if (isInternalApi) {
        append(" (marked as internal API)")
      }
      append(" - Recommendation: ${recommendation.name.lowercase().replace('_', ' ')}")
    }
  }

  internal companion object {
    fun of(
      sourceProject: String,
      targetProject: String,
      internalAccesses: Set<InternalAccess>
    ): InternalAccessAdvice {
      return InternalAccessAdvice(
        sourceProject = sourceProject,
        targetProject = targetProject,
        internalAccesses = internalAccesses
      )
    }
  }
}

@TypeLabel("android_score")
@JsonClass(generateAdapter = false)
public data class AndroidScore(
  val hasAndroidAssets: Boolean,
  val hasAndroidRes: Boolean,
  val usesAndroidClasses: Boolean,
  val hasBuildConfig: Boolean,
  val hasAndroidDependencies: Boolean,
  val hasBuildTypeSourceSplits: Boolean,
) : ModuleAdvice() {

  override val name: String = "android"

  @delegate:Transient
  private val score: Float by unsafeLazy {
    var count = 0f
    if (hasAndroidAssets) count += 2
    if (hasAndroidRes) count += 2
    if (usesAndroidClasses) count += 2
    if (hasBuildConfig) count += 0.5f
    if (hasAndroidDependencies) count += 100f
    if (hasBuildTypeSourceSplits) count +=  0.25f
    count
  }

  /** True if this project uses no Android facilities at all. */
  public fun shouldBeJvm(): Boolean = score == 0f

  /** True if this project only uses some limited number of Android facilities. */
  public fun couldBeJvm(): Boolean = score < THRESHOLD

  override fun isActionable(): Boolean = couldBeJvm()

  internal companion object {
    private const val THRESHOLD = 2f

    fun ofVariants(scores: Collection<AndroidScoreVariant>): AndroidScore? {
      // JVM projects don't have an AndroidScore
      if (scores.isEmpty()) return null

      var hasAndroidAssets = false
      var hasAndroidRes = false
      var hasBuildConfig = false
      var usesAndroidClasses = false
      var hasAndroidDependencies = false
      var hasBuildTypeSourceSplits = false

      scores.forEach {
        hasAndroidAssets = hasAndroidAssets || it.hasAndroidAssets
        hasAndroidRes = hasAndroidRes || it.hasAndroidRes
        hasBuildConfig = hasBuildConfig || it.hasBuildConfig
        usesAndroidClasses = usesAndroidClasses || it.usesAndroidClasses
        hasAndroidDependencies = hasAndroidDependencies || it.hasAndroidDependencies
        hasBuildTypeSourceSplits = hasBuildTypeSourceSplits || it.hasBuildTypeSourceSplits
      }

      return AndroidScore(
        hasAndroidAssets = hasAndroidAssets,
        hasAndroidRes = hasAndroidRes,
        hasBuildConfig = hasBuildConfig,
        usesAndroidClasses = usesAndroidClasses,
        hasAndroidDependencies = hasAndroidDependencies,
        hasBuildTypeSourceSplits = hasBuildTypeSourceSplits,
      )
    }
  }
}
