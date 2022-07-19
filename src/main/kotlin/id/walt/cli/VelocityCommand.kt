package id.walt.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import id.walt.services.velocitynetwork.VelocityClient
import java.io.File

class VelocityCommand : CliktCommand(
    name = "vnf",
    help = """Velocity Network specific operations.

        Velocity Network functions & flows."""
) {
    override fun run() {}
}

class VelocityRegistrationCommand : CliktCommand(
    name = "register",
    help = """Velocity Network DID acquisition flow

        Registers a new organization to the Velocity Network ecosystem. 
        
        For gaining access to the Velocity Network registrar service,
        https://docs.velocitynetwork.foundation/docs/developers/developers-guide-getting-started#register-an-organization"""
) {
    val data: File by argument("ORGANIZATION-DATA-JSON", help = "File containing the organization data").file()
    val registrarBearerTokenFile: File by argument("BEARER-TOKEN-FILE", help = "File containing the bearer token for VN registrar").file()

    override fun run() {

        echo("Registering organization on Velocity Network...\n")
        val did = VelocityClient.registerOrganization(data.readText(), registrarBearerTokenFile.readText().replace("\n", ""))
        echo("Velocity Network DID acquired successfully: $did")
    }
}

class VelocityIssueCommand: CliktCommand(
    name = "issue",
    help = "Issue credential on Velocity Network"
){
    val subject: File by argument("SUBJECT-IDENTIFICATION-CREDENTIAL", help = "File containing subject identification credential").file()
    val issuer: String by option("-i", "--issuer", help = "DID of the issuer.").required()
    val types: List<String> by option("-c", "--credentials", help = "Credential types list").split(" ").required()

    override fun run() {
        echo("Issuing by $issuer on Velocity Network the credentials:\n$types")
        val credentials = VelocityClient.issue(subject.readText(), issuer, *types.toTypedArray())
        echo("The issued credentials:\n$credentials")
    }
}