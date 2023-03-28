package id.walt.auditor

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.HttpCode

object ACLTestApi {
    val ACCEPTED_USER_ID = "0904008084H"

    fun user_in_acl(ctx: Context) {
        val userId = ctx.queryParam("userId") ?: throw BadRequestResponse("No parameter userId specified")
        if(userId == ACCEPTED_USER_ID) {
            ctx.status(HttpCode.OK)
        } else {
            ctx.status(HttpCode.NOT_FOUND)
        }
    }
    private var _javalin: Javalin? = null;
    fun start(port: Int = 8000) {
        _javalin = Javalin.create().routes {
            ApiBuilder.get("user_in_acl", ACLTestApi::user_in_acl)
        }.start(port)
    }

    fun stop() {
        _javalin?.stop()
    }
}
