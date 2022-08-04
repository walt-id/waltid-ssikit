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
    name = "velocity",
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
        val did = VelocityClient.register(data.readText(), registrarBearerTokenFile.readText().replace("\n", ""))
        echo("Velocity Network DID acquired successfully: $did")
    }
}

class VelocityOfferCommand: CliktCommand(
    name = "offer",
    help = "Create offer on Velocity Network"
){
    val issuer: String by option("-i", "--issuer", help = "DID of the issuer.").required()
    val credential: File by argument("CREDENTIAL-FILE", help = "File containing credential").file()
    val token: File by argument("AUTH-TOKEN-FILE", help = "File containing the Auth Bearer Token").file()

    override fun run() {
        val credentialContent = credential.readText()
        val tokenContent = token.readText()
        echo("Issuing with $issuer on Velocity Network the credentials:\n$credentialContent")
        echo("using token:\n$tokenContent")
        val uri = VelocityClient.issue(issuer, credentialContent, tokenContent)
        echo("The issued credential uri:\n$uri")
    }
}

class VelocityIssueCommand: CliktCommand(
    name = "issue",
    help = "Issue credential on Velocity Network"
){
    val issuer: String by option("-i", "--issuer", help = "DID of the issuer.").required()
    val types: List<String> by option("-c", "--credentials", help = "Credential types list").split(" ").required()
    val holder: File by argument("CREDENTIAL-FILE", help = "File containing credential").file()
    val token: File by argument("AUTH-TOKEN-FILE", help = "File containing the Auth Bearer Token").file()

    override fun run() {
        val holderContent = holder.readText()
        val tokenContent = token.readText()
        echo("Issuing types $types with $issuer on Velocity Network for holder:\n$holderContent")
        echo("using token:\n$tokenContent")
        val credentials = VelocityClient.issue(holderContent, issuer, *types.toTypedArray()) {
            VelocityClient.OfferChoice(it.map { it.id }, emptyList())
        }
        echo("The issued credential:\n$credentials")
    }
}

class VelocityVerifyCommand: CliktCommand(
    name = "verify",
    help = "Verify credential on Velocity Network"
){
    val issuer: String by option("-i", "--issuer", help = "DID of the issuer.").required()
    val credentialId: String by option("-c", "--credentialid", help = "Credential id.").required()
    val credential: File by argument("CREDENTIAL-FILE", help = "File containing credential").file()
    val token: File by argument("AUTH-TOKEN-FILE", help = "File containing the Auth Bearer Token").file()

    override fun run() {
        val credentialContent = credential.readText()
        val tokenContent = token.readText()
        echo("Verifying with $issuer on Velocity Network the credentials:\n$credentialContent")
        echo("using token:\n$tokenContent")
        val result = VelocityClient.verify(issuer, credentialId, credentialContent, tokenContent)
        echo("Verification result:\n$result")
    }
}