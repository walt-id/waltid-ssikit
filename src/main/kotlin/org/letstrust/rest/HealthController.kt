package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.letstrust.LetsTrustServices
import org.letstrust.crypto.Key
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.services.key.KeyManagementService


object HealthController {

    @OpenApi(
        summary = "Returns HTTP 200 in case all services are up and running",
        operationId = "health",
        responses = [
            OpenApiResponse("200", [OpenApiContent(String::class)], "successful request"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun health(ctx: Context) {
        // TODO: implement: LetsTrustServices.checkHealth()
        ctx.json("ok")
    }
}
