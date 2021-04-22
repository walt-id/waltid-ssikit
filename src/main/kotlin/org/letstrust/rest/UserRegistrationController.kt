package org.letstrust.rest

import io.javalin.http.Context
import io.javalin.plugin.openapi.annotations.OpenApi

object UserRegistrationController {

    @OpenApi(
        summary = "Register user",
        operationId = "registerUser",
        tags = ["User"],
//        requestBody = OpenApiRequestBody(
//            [OpenApiContent(RegisterUserRequest::class)],
//            true,
//            "the desired user information"
//        ),
//        responses = [
//            OpenApiResponse("200", [OpenApiContent(CreateUserResponse::class)], "successful"),
//            OpenApiResponse("400", [OpenApiContent(ErrorResponse::class)], "invalid request")
//        ]
    )
    fun register(ctx: Context) {
        ctx.json("todo")
    }

}
