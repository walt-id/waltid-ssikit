package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject

class AuthCommand : CliktCommand(
    help = """Authentication.

        Opens and closes an authenticated session with the Walt backend."""
) {

    val config: CliConfig by requireObject()

    /*
    val username: String by option(help = "The developer's shown username.")
    .prompt()
    */

    private val email: String by option(help = "Your email address.")
        .prompt(text = "E-Mail")
    private val password: String by option(help = "Your password.")
        .prompt(hideInput = true)//, requireConfirmation = true)

    override fun run() {
        /*
        config.properties["username"] = username
        config.properties["email"] = email
        config.properties["password"] = "*".repeat(password.length)
        echo("Changed credentials.")
        println(config)
        */

        runBlocking {
            val client = HttpClient(CIO)
            val token = client.post("https://api.walt.id/users/auth/login") {
                setBody(mapOf("email" to email, "password" to password))
            }.body<JsonObject>()["token"].toString()

            println(token)

            val jwt = SignedJWT.parse(token)

            val claimsMap = jwt.jwtClaimsSet.claims
            claimsMap.iterator().forEach { println(it) }

        }
    }
}
