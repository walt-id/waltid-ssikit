package org.letstrust.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import org.letstrust.CliConfig

class auth : CliktCommand(

    help = """Authentication.

        Opens and closes an authenticated session with the LetsTrust backend."""
) {

    val config: CliConfig by requireObject()
    val username: String by option(help = "The developer's shown username.")
        .prompt()
    val email: String by option(help = "The developer's email address.")
        .prompt(text = "E-Mail")
    val password: String by option(help = "The login password.")
        .prompt(hideInput = true, requireConfirmation = true)

    override fun run() {
        config.properties["username"] = username
        config.properties["email"] = email
        config.properties["password"] = "*".repeat(password.length)
        echo("Changed credentials.")
        println(config)
    }
}
