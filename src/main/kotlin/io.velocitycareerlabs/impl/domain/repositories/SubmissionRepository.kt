/**
 * Created by Michael Avoyan on 4/11/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.repositories

import io.velocitycareerlabs.api.entities.*

internal interface SubmissionRepository {
    fun submit(submission: VCLSubmission,
               jwt: VCLJWT,
               completionBlock: (VCLResult<VCLSubmissionResult>) -> Unit)
}