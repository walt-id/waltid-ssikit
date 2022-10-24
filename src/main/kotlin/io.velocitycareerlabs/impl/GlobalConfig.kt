/**
 * Created by Michael Avoyan on 3/12/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl

import io.velocitycareerlabs.api.VCLEnvironment

internal object GlobalConfig {
    const val VclPackage = BuildConfig.LIBRARY_PACKAGE_NAME

    var CurrentEnvironment = VCLEnvironment.PROD

    val IsDebug = BuildConfig.DEBUG

    val VersionName = BuildConfig.VERSION_NAME
    val VersionCode = BuildConfig.VERSION_CODE

    const val LogTagPrefix = "VCL "
    // TODO: Will be remotely configurable
    val IsLoggerOn get() = CurrentEnvironment != VCLEnvironment.PROD

    // TODO: Will be remotely configurable
    var IsToLoadFromCacheInitialization = false
}
