package id.walt.services.velocitynetwork.did

import com.beust.klaxon.Klaxon
import id.walt.services.WaltIdServices
import id.walt.services.velocitynetwork.models.responses.CreateOrganizationResponse
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.pwall.json.schema.JSONSchema

class WaltIdDidVelocityService : DidVelocityService() {
    companion object {
        const val VELOCITY_NETWORK_REGISTRAR_ENDPOINT = "https://%sregistrar.velocitynetwork.foundation/"
    }
    private val VELOCITY_NETWORK_ENV = System.getenv().get("VN_ENV") ?: "staging"
    private val VELOCITY_NETWORK_REGISTRAR_API = when (VELOCITY_NETWORK_ENV) {
        "prod" -> ""
        else -> VELOCITY_NETWORK_ENV
    }.let { String.format(VELOCITY_NETWORK_REGISTRAR_ENDPOINT, it) }

    private val log = KotlinLogging.logger {}

    override fun registerOrganization(
        data: String,
        token: String,
        onResult: (
            did: String,
            didDoc: String,
            keys: List<CreateOrganizationResponse.Key>,
            authClients: List<CreateOrganizationResponse.AuthClient>,
        ) -> Unit
    ) = runBlocking {
        log.debug { "Registering organization on Velocity Network... " }
        if (!validate(data)) throw Exception("Schema validation failed.")
        Klaxon().parse<CreateOrganizationResponse>(
            WaltIdServices.http.post(VELOCITY_NETWORK_REGISTRAR_API) {
                setBody(data)
                header("Bearer", token)
            }.bodyAsText()
        )?.let {
            log.debug { "Registration completed successfully" }
//            onResult(it.id, it.didDoc, it.keys, it.authClients)
        } ?: throw Exception("Empty result")
    }

    override fun validate(data: String) =
        super.validate(data) && JSONSchema.parse("/velocitynetwork/schemas/organization-registration-reqSchema.json")
            .validateBasic(data).valid
}