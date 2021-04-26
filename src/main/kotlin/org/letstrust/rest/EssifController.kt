package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi
import io.javalin.plugin.openapi.annotations.OpenApiContent
import io.javalin.plugin.openapi.annotations.OpenApiRequestBody
import io.javalin.plugin.openapi.annotations.OpenApiResponse
import org.letstrust.services.essif.EnterpriseWalletService
import org.letstrust.services.essif.EosService
import org.letstrust.services.essif.UserWalletService


data class TodoRequest(
    val todo: String
)

object EssifController {

    @OpenApi(
        summary = "TODO",
        operationId = "todo",
        tags = ["essif"],
        requestBody = OpenApiRequestBody(
            [OpenApiContent(TodoRequest::class)],
            true,
            "todo"
        ),
        responses = [
            OpenApiResponse("200", [OpenApiContent(SuccessResponse::class)], "successful"),
            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
        ]
    )
    fun todo(ctx: Context) {
        ctx.json("todo")
        /**
        // EnterpriseWalletService
        EnterpriseWalletService.onboardTrustedIssuer("")
        EnterpriseWalletService.token("")
        EnterpriseWalletService.createDid()
        EnterpriseWalletService.generateDidAuthRequest()
        EnterpriseWalletService.validateDidAuthResponse("")
        EnterpriseWalletService.requestVerifiableAuthorization("")
        EnterpriseWalletService.requestVerifiableCredential("")
        EnterpriseWalletService.getVerifiableCredential("", "")

        // UserWalletService
        UserWalletService.validateDidAuthRequest("")
        UserWalletService.didAuthResponse("")
        UserWalletService.oidcAuthResponse("")
        UserWalletService.requestAccessToken("")
        UserWalletService.vcAuthResponse("")

        // EOS
        EosService.onboards()
        EosService.didOwnershipResponse("")
        EosService.getCredential("")
        EosService.getCredentials()
        EosService.requestCredentialUri()
        EosService.requestVerifiableCredential("")
        EosService.signedChallenge("")
        **/
    }
}
