package id.walt.cli

import com.beust.klaxon.Klaxon
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import id.walt.common.prettyPrint
import id.walt.common.saveToFile
import id.walt.services.WaltIdServices
import id.walt.services.velocitynetwork.VelocityClient
import id.walt.services.velocitynetwork.models.CredentialCheckType
import id.walt.services.velocitynetwork.models.CredentialCheckValue
import id.walt.services.velocitynetwork.models.responses.CreateOrganizationResponse
import java.io.File
import java.sql.Timestamp
import java.time.LocalDateTime

class VelocityCommand : CliktCommand(
    name = "velocity",
    help = """Velocity Network specific operations.

        Velocity Network functions & flows."""
) {
    override fun run() {}
}

class VelocityOnboardingCommand : CliktCommand(
    name = "onboarding",
    help = "Velocity Network onboarding functions"
) {
    override fun run() {
        echo()
    }
}

class VelocityOrganizationCommand : CliktCommand(
    name = "organization",
    help = """Velocity Network DID acquisition flow

        Registers a new organization to the Velocity Network ecosystem. 
        
        For gaining access to the Velocity Network registrar service,
        https://docs.velocitynetwork.foundation/docs/developers/developers-guide-getting-started#register-an-organization"""
) {
    val data: File by argument("ORGANIZATION-DATA-JSON", help = "File containing the organization data").file()

    override fun run() {
        echo("Registering organization on Velocity Network...\n")
        VelocityClient.register(data.readText())
            .onSuccess { response ->
                runCatching {
                    Klaxon().parse<CreateOrganizationResponse>(response)!!
                }.onSuccess {
                    echo("Velocity Network DID acquired successfully:\n${it.id}")
                    val filepath = "${WaltIdServices.organizationDir}/${it.profile.name}.json"
                    saveToFile(filepath, response)
                    echo("Organization data saved at: $filepath")
                }
            }.onFailure {
                echo("Registration failed:\n${it.message}")
            }
    }
}

class VelocityTenantCommand : CliktCommand(
    name = "tenant",
    help = """Velocity Network tenant functions
        
        Set up tenants with the credential agent:
        https://docs.velocitynetwork.foundation/docs/developers/developers-guide-credential-agent-configuration#tenants
            """
) {
    val data: File by argument("TENANT-DATA-JSON", help = "File containing the tenant request data").file()
    override fun run() {
        val tenant = data.readText()
        echo("Setting up new tenant on credential agent")
        echo(tenant)
        VelocityClient.addTenant(tenant).onSuccess {
            echo("Tenant created successfully!\n${it.prettyPrint()}")
            val filepath = "${WaltIdServices.tenantDir}/disclosure-${Timestamp.valueOf(LocalDateTime.now()).time}.json"
            saveToFile(filepath, it)
            echo("Tenant data saved at: $filepath")
        }.onFailure {
            echo("Could not create tenant:\n${it.message}")
        }
    }
}

class VelocityDisclosureCommand : CliktCommand(
    name = "disclosure",
    help = """Velocity Network disclosure functions
        
        Add disclosure type for issuer:
        https://docs.velocitynetwork.foundation/docs/developers/developers-guide-credential-agent-configuration#disclosure-requests
    """
) {
    val issuer: String by option("-i", "--issuer", help = "DID of the issuer.").required()
    val data: File by argument("DISCLOSURE-DATA-JSON", help = "File containing the disclosure type request data").file()
    override fun run() {
        val disclosure = data.readText()
        echo("Adding disclosure for $issuer")
        echo(disclosure)
        VelocityClient.addDisclosure(issuer, disclosure).onSuccess {
            echo("Disclosure added successfuly!\n${it.prettyPrint()}")
            val filepath = "${WaltIdServices.disclosureDir}/disclosure-${Timestamp.valueOf(LocalDateTime.now()).time}.json"
            saveToFile(filepath, it)
            echo("Organization data saved at: $filepath")
        }.onFailure {
            echo("Could not add disclosure:\n${it.message}")
        }
    }
}

class VelocityOfferCommand : CliktCommand(
    name = "offer",
    help = "Create offer on Velocity Network"
) {
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

class VelocityIssueCommand : CliktCommand(
    name = "issue",
    help = "Issue credential on Velocity Network"
) {
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

class VelocityVerifyCommand : CliktCommand(
    name = "verify",
    help = """Verify credential on Velocity Network
        
        Verify credential issued on Velocity Network:
        https://docs.velocitynetwork.foundation/docs/developers/developers-guide-disclosure-exchange#the-verification-process
        """
) {
    val issuer: String by option("-i", "--issuer", help = "DID of the issuer.").required()
    val credential: File by argument("CREDENTIAL-FILE", help = "File containing the jwt-formatted credential to be verified").file()
    val checkList: File by argument(
        "CHECK-LIST-FILE",
        """help = "File containing the check-list to verify against
                e.g. 
                {
                  "TRUSTED_ISSUER": "PASS",
                  "UNREVOKED": "PASS",
                  "UNEXPIRED": "NOT_APPLICABLE",
                  "UNTAMPERED": "PASS"
                }
            """).file()

    override fun run() {
        val credentialContent = credential.readText()
        val checks = Klaxon().parse<Map<CredentialCheckType, CredentialCheckValue>>(checkList.readText())
        echo("Verifying with $issuer on Velocity Network..")
        VelocityClient.verify(issuer, credentialContent, checks!!).onSuccess {
            echo(
                "Verification result:" +
                        "${it.map { it.result }.reduce { acc, b -> acc && b }}" +
                        "\n${it.map{ it.checks.prettyPrint() }}"
            )
        }.onFailure {
            echo("Verification failed:\n${it.message}")
        }

    }
}