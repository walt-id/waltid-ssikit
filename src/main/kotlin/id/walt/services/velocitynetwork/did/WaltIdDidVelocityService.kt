package id.walt.services.velocitynetwork.did

import id.walt.common.readWhenContent
import id.walt.services.WaltIdServices
import id.walt.services.did.DidService
import io.ktor.client.call.*
import io.ktor.client.request.*
import jakarta.json.JsonObject
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.pwall.json.schema.JSONSchema
import java.io.File

class WaltIdDidVelocityService : DidVelocityService() {
    companion object {
        const val VN_REGISTRAR_ENDPOINT = "https://%sregistrar.velocitynetwork.foundation/"
    }
    private val VN_ENV = System.getenv().get("VELOCITYNETWORK_ENV") ?: "staging"
    private val VN_REGISTRAR_API = when (VN_ENV) {
        "prod" -> ""
        else -> VN_ENV
    }.let { String.format(VN_REGISTRAR_ENDPOINT, it) }

    private val log = KotlinLogging.logger {}

    override fun createOrganization(data: String?) = runBlocking {
        val json = data ?: readWhenContent(File("/velocitynetwork/samples/organization-registration-req.json"))
        if(!validate(json)) throw Exception("Schema validation failed.")
        val response = WaltIdServices.http.post(VN_REGISTRAR_API){
            setBody(json)
        }
        val ids = response.body<JsonObject>()["ids"]
        val keys = response.body<JsonObject>()["keys"]
        val clients = response.body<JsonObject>()["authClients"]

        DidService.importDid("ids.did")
    }

    override fun validate(data: String) =
        super.validate(data) && JSONSchema.parse("/velocitynetwork/schemas/organization-registration-reqSchema.json")
            .validateBasic(data).valid
}