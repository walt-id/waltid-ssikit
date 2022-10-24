/**
 * Created by Michael Avoyan on 24/06/2021.
 *
 * Copyright 2022 Velocity Career Labs inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package io.velocitycareerlabs.api.entities

import io.velocitycareerlabs.impl.extensions.encode

data class VCLOrganizationsSearchDescriptor(
    val filter: VCLFilter? = null,
    val page: VCLPage? = null,
    /**
     * A array of tuples indicating the field to sort by
     */
    val sort: List<List<String>>? = null,
    /**
     * Full Text search for the name property of the organization
     * Matches on partials strings
     * Prefer results that are the first word of the name, or first letter of a word
     */
    val query: String? = null
) {

    val queryParams = generateQueryParams()

    private fun generateQueryParams(): String? {
        val pFilterDid = filter?.did?.let { "$KeyFilterDid=$it" }
        val pFilterServiceTypes = filter?.serviceTypes?.let { serviceTypes ->
            "$KeyFilterServiceTypes=${serviceTypes.joinToString(separator = ",") { it.toString() }}" }
        val pFilterCredentialTypes = filter?.credentialTypes?.let { credentialTypes ->
            "$KeyFilterCredentialTypes=${credentialTypes.map { it.encode() }.joinToString(separator = ",") { it }}"}
        val pSort = sort?.mapIndexed{index, list -> "$KeySort[$index]=${list.joinToString(separator = ",")}" }?.joinToString(separator = "&")
        val pPageSkip = page?.skip?.encode()?.let { "$KeyPageSkip=${it}" }
        val pPageSize = page?.size?.encode()?.let { "$KeyPageSize=${it}" }
        val pQuery = query?.encode()?.let { "$KeyQueryQ=${it}" }
        val qParams = listOfNotNull(
            pFilterDid,
            pFilterServiceTypes,
            pFilterCredentialTypes,
            pSort,
            pPageSkip,
            pPageSize,
            pQuery
        )
        return if(qParams.isNotEmpty()) qParams.joinToString("&") else null
    }

    companion object CodingKeys {
        const val KeyQueryQ = "q"

        const val KeySort = "sort"

        const val KeyFilterDid = "filter.did"
        const val KeyFilterServiceTypes = "filter.serviceTypes"
        const val KeyFilterCredentialTypes = "filter.credentialTypes"

        const val KeyPageSkip = "page.skip"
        const val KeyPageSize = "page.size"
    }
}

data class VCLFilter(
    /**
     * Filters organizations based on DIDs
     */
    val did: String? = null,
    /**
     * Filters organizations based on Service Types e.g. [VCLServiceType]
     */
    val serviceTypes: List<VCLServiceType>? = null,
    /**
     * Filters organizations based on credential types e.g. [EducationDegree]
     */
    val credentialTypes: List<String>? = null
)

data class VCLPage(
    /**
     * The number of records to retrieve
     */
    val size: String? = null,
    /**
     * The objectId to skip
     */
    val skip: String? = null
)
