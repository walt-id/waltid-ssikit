package id.walt.credentials.w3c.builder

import id.walt.common.createBaseToken
import id.walt.credentials.w3c.*
import id.walt.model.credential.status.CredentialStatus
import id.walt.signatory.ProofConfig
import id.walt.signatory.Signatory
import id.walt.signatory.SignatoryConfig
import id.walt.signatory.revocation.CredentialStatusFactory
import id.walt.signatory.revocation.SimpleStatusFactoryParameter
import id.walt.signatory.revocation.StatusListEntryFactoryParameter
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.format.DateTimeFormatterBuilder

class W3CCredentialBuilderWithCredentialStatus<C : VerifiableCredential, B : AbstractW3CCredentialBuilder<C, B>>(
    private val builder: AbstractW3CCredentialBuilder<C, B>,
    private val proofConfig: ProofConfig,
) : AbstractW3CCredentialBuilder<VerifiableCredential, W3CCredentialBuilder>(builder.type, VerifiableCredential) {

    private val signatoryConfig = Signatory.getService().configuration as? SignatoryConfig

    override fun build(): C = builder.apply {
        getStatusProperty(
            issuer = proofConfig.issuerDid,
            type = proofConfig.statusType!!,
            purpose = proofConfig.statusPurpose,
            credentialUrl = proofConfig.credentialsEndpoint ?: signatoryConfig?.proofConfig?.credentialsEndpoint
            ?: proofConfig.statusPurpose
        )?.let { this.setProperty("credentialStatus", it) }
    }.build()

    private fun getStatusProperty(issuer: String, type: CredentialStatus.Types, purpose: String, credentialUrl: String) =
        when (type) {
            CredentialStatus.Types.SimpleCredentialStatus2022 -> SimpleStatusFactoryParameter(
                id = URLBuilder().takeFrom(credentialUrl).appendPathSegments("token", createBaseToken()).buildString(),
            )
            CredentialStatus.Types.StatusList2021Entry -> StatusListEntryFactoryParameter(
                purpose = purpose,
                credentialUrl = credentialUrl,
                issuer = issuer,
            )
        }.let {
            CredentialStatusFactory.create(it)
        }.takeIf {
            it.isNotEmpty()
        }
}

class W3CCredentialBuilder(type: List<String> = listOf("VerifiableCredential")) :
    AbstractW3CCredentialBuilder<VerifiableCredential, W3CCredentialBuilder>(type, VerifiableCredential) {
    companion object {
        fun fromPartial(partialCredential: VerifiableCredential) =
            W3CCredentialBuilder(listOf()).setFromJsonObject(partialCredential.toJsonObject())

        fun fromPartial(partialJson: String) = fromPartial(VerifiableCredential.fromJson(partialJson))
    }
}

abstract class AbstractW3CCredentialBuilder<C : VerifiableCredential, B : AbstractW3CCredentialBuilder<C, B>>(
    initialType: List<String>,
    val credentialFactory: CredentialFactory<C>
) : BasicBuilder<C, B>() {

    init {
        setProperty("type", initialType)
        setProperty("@context", listOf("https://www.w3.org/2018/credentials/v1"))
    }

    protected val dateFormat = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendInstant(0)
        .toFormatter()

    protected val subjectBuilder = SubjectBuilder()

    val type: List<String>
        get() = properties["type"]?.let { (JsonConverter.fromJsonElement(it) as List<*>).map { it.toString() } } ?: listOf()

    fun addContext(contextItem: W3CContext): B {
        setProperty("@context", properties["@context"]!!.jsonArray.plus(contextItem.toJsonElement()))
        return this as B
    }

    fun setId(id: String) = setProperty("id", id)
    fun setIssuer(issuer: String) = setProperty("issuer", issuer)
    fun setIssuer(issuer: W3CIssuer) = setProperty("issuer", issuer.toJsonElement())
    fun setIssuerId(issuerId: String) = setIssuer(properties["issuer"]?.let {
        W3CIssuer.fromJsonElement(it).apply {
            id = issuerId
        }
    } ?: W3CIssuer(issuerId))

    fun setIssuanceDate(date: Instant) = setProperty("issuanceDate", dateFormat.format(date))
    fun setIssued(date: Instant) = setProperty("issued", dateFormat.format(date))
    fun setValidFrom(date: Instant) = setProperty("validFrom", dateFormat.format(date))
    fun setExpirationDate(date: Instant) = setProperty("expirationDate", dateFormat.format(date))
    fun setCredentialSchema(schema: W3CCredentialSchema) = setProperty("credentialSchema", schema.toJsonObject() as JsonElement)
    fun setSubjectId(id: String) = buildSubject { setId(id) }
    fun buildSubject(builderAction: SubjectBuilder.() -> Unit): B {
        subjectBuilder.builderAction()
        return this as B
    }

    fun setProof(proof: W3CProof) = setProperty("proof", proof.toJsonObject() as JsonElement)

    override fun setFromJsonObject(jsonObject: JsonObject): B {
        super.setFromJsonObject(jsonObject)
        properties["credentialSubject"]?.let { subjectBuilder.setFromJsonObject(it.jsonObject) }
        return this as B
    }

    override fun build(): C {
        val subject = subjectBuilder.build().toJsonObject()
        if (subject.isNotEmpty()) {
            setProperty("credentialSubject", subject as JsonElement)
        }
        return credentialFactory.fromJsonObject(JsonObject(properties))
    }
}
