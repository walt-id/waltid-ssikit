/**
 * Created by Michael Avoyan on 4/20/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.impl.domain.usecases

import io.velocitycareerlabs.api.entities.VCLOrganizations
import io.velocitycareerlabs.api.entities.VCLOrganizationsSearchDescriptor
import io.velocitycareerlabs.api.entities.VCLResult

internal interface OrganizationsUseCase {
    fun searchForOrganizations(organizationsSearchDescriptor: VCLOrganizationsSearchDescriptor,
                               completionBlock: (VCLResult<VCLOrganizations>) -> Unit)
}