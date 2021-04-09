package org.letstrust.services.essif.mock

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

object AuthorizationApi {
    fun getAuthorizationRequest(authenticationRequest: String): String {
        println("2. [AuthApi] Create <AuthReq>")
        val authenticationRequestResponse = Json.encodeToString(
            mapOf(
                "uri" to "openid://?response_type=id_token&client_id=https%3A%2F%2Fapi.ebsi.zyz%2Faccess-tokens&scope=openid%20did_authn&request=eyJraWQiOiJMZXRzVHJ1c3QtS2V5LTBhNzBjZmZlMmQxMDQyY2Q4NDkwYzIxYjcxYjkzZTM3IiwiYWxnIjoiRVMyNTZLIn0.eyJzY29wZSI6Im9wZW5pZCBkaWRfYXV0aG4iLCJpc3MiOiJkaWQ6ZWJzaToweDQxNmU2ZTYxNjI2NTZjMmU0YzY1NjUyZTQ1MmQ0MTJkNTA2ZjY1MmUiLCJjbGFpbXMiOnsiaWRfdG9rZW4iOnsidmVyaWZpZWRfY2xhaW1zIjp7InZlcmlmaWNhdGlvbiI6eyJldmlkZW5jZSI6eyJkb2N1bWVudCI6eyJjcmVkZW50aWFsU2NoZW1hIjp7ImlkIjp7InZhbHVlIjoiaHR0cHM6XC9cL2Vic2kueHl6XC90cnVzdGVkLXNjaGVtYXMtcmVnaXN0cnlcL3ZlcmlmaWFibGUtYXV0aG9yaXNhdGlvbiIsImVzc2VudGlhbCI6dHJ1ZX19LCJ0eXBlIjp7InZhbHVlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiVmVyaWZpYWJsZUF1dGhvcmlzYXRpb24iXSwiZXNzZW50aWFsIjp0cnVlfX0sInR5cGUiOnsidmFsdWUiOiJ2ZXJpZmlhYmxlX2NyZWRlbnRpYWwifX0sInRydXN0X2ZyYW1ld29yayI6IkVCU0kifX19fSwicmVzcG9uc2VfdHlwZSI6ImlkX3Rva2VuIiwicmVnaXN0cmF0aW9uIjoiPHJlZ2lzdHJhdGluIG9iamVjdD4iLCJub25jZSI6IjxyYW5kb20tbm9uY2U-IiwiY2xpZW50X2lkIjoiPHJlZGlyZWN0LXVyaT4ifQ.4SM7quGYTHq8b8jXcx1tQHUay9MZwM4obVN459HMXX3V6lfhGjBeqVQOd3TyE18ORVn8SAviTBLSnkWdZN14zg"
            )
        )
        return authenticationRequestResponse
    }
}
