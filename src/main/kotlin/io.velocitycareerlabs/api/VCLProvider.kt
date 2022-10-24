/**
 * Created by Michael Avoyan on 3/20/21.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api

import io.velocitycareerlabs.impl.VCLImpl

class VCLProvider {
    companion object {
        fun vclInstance(): VCL {
            return VCLImpl()
        }
    }
}