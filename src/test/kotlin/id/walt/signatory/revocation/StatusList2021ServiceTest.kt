package id.walt.signatory.revocation

import com.beust.klaxon.Klaxon
import id.walt.common.resolveContent
import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.builder.W3CCredentialBuilder
import id.walt.credentials.w3c.templates.VcTemplateService
import id.walt.crypto.KeyAlgorithm
import id.walt.model.DidMethod
import id.walt.model.credential.status.CredentialStatus
import id.walt.model.credential.status.StatusList2021EntryCredentialStatus
import id.walt.servicematrix.ServiceMatrix
import id.walt.services.WaltIdServices
import id.walt.services.did.DidService
import id.walt.services.did.DidWebCreateOptions
import id.walt.services.key.KeyService
import id.walt.signatory.ProofConfig
import id.walt.signatory.ProofType
import id.walt.signatory.Signatory
import id.walt.signatory.revocation.statuslist2021.StatusList2021EntryClientService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.random.Random

internal class StatusList2021ServiceTest : StringSpec({
    val sut = StatusList2021EntryClientService()
    val rootPath = "src/test/resources/credential-status/"
    val statusLisCredential = resolveContent(rootPath + "status-list-credential.json")
    lateinit var keyId: String

    beforeSpec {
        ServiceMatrix("service-matrix.properties")
        keyId = KeyService.getService().generate(KeyAlgorithm.EdDSA_Ed25519).id
    }

    afterSpec {
        KeyService.getService().delete(keyId)
    }

    "test result" {
        forAll(
            row("vc-status-revoked.json", true),
            row("vc-status-unrevoked.json", false),
        ) { vcPath, isRevoked ->
            val vcStatus = Klaxon().parse<StatusList2021EntryCredentialStatus>(resolveContent(rootPath + vcPath))!!
            mockkStatic(::resolveContent)
            every { resolveContent(any()) } returns statusLisCredential

            val result = sut.checkRevocation(StatusListRevocationCheckParameter(vcStatus))

            result.isRevoked shouldBe isRevoked

            unmockkStatic(::resolveContent)
        }
    }

    "test throwing" {
        forAll(
            row("vc-status-wrong-purpose.json", IllegalArgumentException::class, "Status purposes don't match"),
            row("vc-status-missing-index.json", IllegalArgumentException::class, "Couldn't parse status list index"),
        ) { vcPath, throwing, message ->
            val vcStatus = Klaxon().parse<StatusList2021EntryCredentialStatus>(resolveContent(rootPath + vcPath))!!
            mockkStatic(::resolveContent)
            every { resolveContent(any()) } returns statusLisCredential

            val exception = shouldThrow<Exception> {
                sut.checkRevocation(StatusListRevocationCheckParameter(vcStatus))
            }
            exception.message shouldBe message
            exception::class shouldBe throwing

            unmockkStatic(::resolveContent)
        }
    }

    "given issuer, when issuing a credential with status then the status-list credential has the issuer's did".config(
        blockingTest = true
    ) {
        forAll(
            row(DidService.create(DidMethod.web, keyId, DidWebCreateOptions("example.com"))),
            row(DidService.create(DidMethod.key, keyId)),
            row(DidService.create(DidMethod.ebsi, keyId)),
            row(DidService.create(DidMethod.jwk, keyId)),
            row(DidService.create(DidMethod.cheqd, keyId)),
        ) { issuer ->
            // given
            val credentialUrl = "http://localhost:7001/credentials/status/#${Random.nextInt()}"
            val path = Path(WaltIdServices.revocationDir, "${URLEncoder.encode(credentialUrl, StandardCharsets.UTF_8)}.cred").pathString
            val template = VcTemplateService.getService().getTemplate("VerifiableId").template!!
            // when
            Signatory.getService().issue(
                W3CCredentialBuilder.fromPartial(template), ProofConfig(
                    subjectDid = issuer,
                    issuerDid = issuer,
                    proofType = ProofType.LD_PROOF,
                    statusPurpose = "revocation",
                    statusType = CredentialStatus.Types.StatusList2021Entry,
                    credentialsEndpoint = credentialUrl
                )
            )
            // then
            val statusListVcStr = resolveContent(path)
            val statusListVc = VerifiableCredential.fromString(statusListVcStr)
            statusListVc.shouldNotBeNull()
            statusListVc.issuerId.shouldNotBeNull()
            statusListVc.issuerId shouldBe issuer

            //cleanup TODO: dids
            File(path).takeIf { it.exists() }?.run {
                this.delete()
            }
        }
    }
})
