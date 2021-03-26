package org.letstrust.services.vc

import org.junit.Before
import org.junit.Test
import org.letstrust.model.DidMethod
import org.letstrust.services.did.DidService
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CredentialServiceTest {

    private val RESOURCES_PATH: String = "src/test/resources"

    fun readCredOffer(fileName: String) =
        File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

    fun readVerifiableCredential(fileName: String) =
        File("$RESOURCES_PATH/verifiable-credentials/${fileName}.json").readText(Charsets.UTF_8)

    @Before
    fun setup() {

    }


    @Test
    fun issueWorkHistoryCredential() {

        val credOffer = readCredOffer("WorkHistory")

        val issuerDid = DidService.create(DidMethod.key)
        val domain = "example.com"
        val nonce: String? = null

        val vc = CredentialService.sign(issuerDid, credOffer, CredentialService.SignatureType.Ed25519Signature2018, domain, nonce)
        assertNotNull(vc)
        println("Credential generated: $vc")

        val vcVerified = CredentialService.verify(issuerDid, vc, CredentialService.SignatureType.Ed25519Signature2018)
        assertTrue(vcVerified)
    }

}
