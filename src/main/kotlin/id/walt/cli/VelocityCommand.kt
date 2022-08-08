package id.walt.cli

import com.beust.klaxon.Klaxon
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import id.walt.services.velocitynetwork.VelocityClient
import id.walt.services.velocitynetwork.models.CredentialCheckType
import id.walt.services.velocitynetwork.models.CredentialCheckValue
import java.io.File

class VelocityCommand : CliktCommand(
    name = "velocity",
    help = """Velocity Network specific operations.

        Velocity Network functions & flows."""
) {
    override fun run() {}
}

class VelocityOnboardingCommand: CliktCommand(
    name = "onboarding",
    help = "Velocity Network onboarding functions"
){
    override fun run() {
        echo()
    }
}

class VelocityRegistrationCommand : CliktCommand(
    name = "register",
    help = """Velocity Network DID acquisition flow

        Registers a new organization to the Velocity Network ecosystem. 
        
        For gaining access to the Velocity Network registrar service,
        https://docs.velocitynetwork.foundation/docs/developers/developers-guide-getting-started#register-an-organization"""
) {
    val data: File by argument("ORGANIZATION-DATA-JSON", help = "File containing the organization data").file()

    override fun run() {

        echo("Registering organization on Velocity Network...\n")
        val did = VelocityClient.register(data.readText())
        echo("Velocity Network DID acquired successfully: $did")
    }
}

class VelocityTenantCommand: CliktCommand(
    name = "tenant",
    help = """Velocity Network tenant functions
        
        Set up tenants with the credential agent
            """
){
    val data: File by argument("TENANT-DATA-JSON", help = "File containing the tenant request data").file()
    override fun run() {
        val tenant = data.readText()
        echo("Setting up new tenant on credential agent")
        echo(tenant)
        val result = VelocityClient.addTenant(tenant)
        echo("Result:\n$result")
    }
}

class VelocityDisclosureCommand: CliktCommand(
    name = "disclosure",
    help = """Velocity Network disclosure functions
        
        Add disclosure
    """
){
    val issuer: String by option("-i", "--issuer", help = "DID of the issuer.").required()
    val data: File by argument("DISCLOSURE-DATA-JSON", help = "File containing the disclosure request data").file()
    override fun run() {
        val disclosure = data.readText()
        echo("Adding disclosure for $issuer")
        echo(disclosure)
        val result = VelocityClient.addDisclosure(issuer, disclosure)
        echo("Result:\n$result")
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
    val credential: File by argument("CREDENTIAL-FILE", help = "File containing credential").file()
    val checkList: File by argument("CHECK-LIST-FILE", help = "File containing the checks to verify against").file()

    override fun run() {
        val credentialContent = credential.readText()
        val checks = Klaxon().parse<Map<CredentialCheckType, CredentialCheckValue>>(checkList.readText())
        echo("Verifying with $issuer on Velocity Network the credentials:\n$credentialContent")
        val result = VelocityClient.verify(issuer, credentialContent, checks!!)
        echo("Verification result:\n$result")
    }
}