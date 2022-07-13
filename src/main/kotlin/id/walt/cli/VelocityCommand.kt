package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import id.walt.services.velocitynetwork.VelocityClient
import java.io.File

class VelocityCommand : CliktCommand(
    name = "velocity",
    help = """Velocity Network specific operations.

        Velocity Network functions & flows."""
) {
    override fun run() {}
}

class VelocityRegistrationCommand : CliktCommand(
    name = "register",
    help = """Velocity Network DID acquisition flow

        Registers a new organization to the Velocity Network eco system. 
        
        For gaining access to the Velocity Network registrar service,
        https://docs.velocitynetwork.foundation/docs/developers/developers-guide-getting-started#register-an-organization"""
) {
    val data: File by argument("ORGANIZATION-DATA-JSON", help = "File containing the organization data").file()
    val registrarBearerTokenFile: File by argument("BEARER-TOKEN-FILE", help = "File containing the bearer token for VN registrar").file()

    override fun run() {

        echo("Registering organization on Velocity Network...\n")

        VelocityClient.register(data.readText(), registrarBearerTokenFile.readText().replace("\n", ""))

        echo("Velocity Network DID acquired successfully: $data")
    }
}