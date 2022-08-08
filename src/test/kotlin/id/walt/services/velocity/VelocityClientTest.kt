package id.walt.services.velocity

import com.beust.klaxon.Klaxon
import id.walt.common.readWhenContent
import id.walt.services.velocitynetwork.VelocityClient
import id.walt.services.velocitynetwork.models.CredentialCheckType
import id.walt.services.velocitynetwork.models.CredentialCheckValue
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.blocking.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import java.io.File

class VelocityClientTest : StringSpec({

    "verify credential returns expected"{
        forAll(
            row("src/test/resources/velocity/id-credential-check-true.json", true),
            row("src/test/resources/velocity/id-credential-check-partial-true.json", true),
            row("src/test/resources/velocity/id-credential-check-false.json", false),
        ) { path, expected ->
            // given
//        val result = readWhenContent(File("src/test/resources/velocity/id-document-check-response.json"))
//        val verifierService = mockk<VerifierVelocityService>()
//        coEvery { verifierService.check(any(), credential) } returns result
            val credential = readWhenContent(File("src/test/resources/velocity/id-raw-credential.txt"))
            //TODO: deployment of a fully set up credential-agent is required for the tests to run
            val did = "did:ion:EiAD16U5eAJMUulSFMmV3ekV_mQ2rresO9d6mY6Vohg6Hg"
            val checks = Klaxon().parse<Map<CredentialCheckType, CredentialCheckValue>>(readWhenContent(File(path)))

            // when
            val result = VelocityClient.verify(did, credential, checks!!)

            // then
            result shouldBe expected
        }
    }
})