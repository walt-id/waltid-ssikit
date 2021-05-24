package org.letstrust.services.did

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.letstrust.crypto.KeyAlgorithm
import org.letstrust.model.DidMethod
import org.letstrust.model.DidUrl
import org.letstrust.services.key.KeyManagementService
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DidServiceTest {

    private val RESOURCES_PATH: String = "src/test/resources"

    fun readExampleDid(fileName: String) =
        File("$RESOURCES_PATH/dids/${fileName}.json").readText(Charsets.UTF_8)

    val ds = DidService

    @Test
    fun parseDidUrlTest() {

        val didUrl = DidUrl("method", "identifier", "key1")
        assertEquals("did:method:identifier#key1", didUrl.url)

        val obj: DidUrl = DidUrl.from(didUrl.url)
        assertEquals(didUrl, obj)
    }

    @Test
    fun createResolveDidKeyTest() {

        // Create
        val did = ds.create(DidMethod.key)
        val didUrl = DidUrl.from(did)
        assertEquals(did, didUrl.did)
        assertEquals("key", didUrl.method)
        print(did)

        // Resolve
        val resolvedDid = ds.resolve(did)
        val encoded = Json { prettyPrint = true }.encodeToString(resolvedDid)
        println(encoded)
    }

    @Test
    fun createResolveDidWebTest() {

        // Create
        val did = ds.create(DidMethod.web)
        val didUrl = DidUrl.from(did)
        assertEquals(did, didUrl.did)
        assertEquals("web", didUrl.method)
        print(did)

        // Resolve
        val resolvedDid = ds.resolve(did)
        val encoded = Json { prettyPrint = true }.encodeToString(resolvedDid)
        println(encoded)
    }

    @Test
    fun createDidEbsiV2Identifier() {
        val didUrl = DidUrl.generateDidEbsiV2DidUrl()
        val did = didUrl.did
        assertEquals("did:ebsi:", did.substring(0, 9))
        assertEquals(48, didUrl.identifier.length)
    }

    @Test
    fun createResolveDidEbsiTest() {

        // Create
        val keyId = KeyManagementService.generate(KeyAlgorithm.ECDSA_Secp256k1)
        val did = ds.create(DidMethod.ebsi, keyId.id)
        println(did)
        val didUrl = DidUrl.from(did)
        assertEquals(did, didUrl.did)
        assertEquals("ebsi", didUrl.method)
        print(did)

        // Resolve
        val resolvedDid = ds.resolveDidEbsi(did)
        val encoded = Json { prettyPrint = true }.encodeToString(resolvedDid)
        println(encoded)

        // Update
        resolvedDid.assertionMethod = listOf(resolvedDid.verificationMethod!!.get(0).id)
        ds.updateDidEbsi(resolvedDid)
        val encodedUpd = Json { prettyPrint = true }.encodeToString(resolvedDid)
        println(encodedUpd)
    }

    @Test
    fun listDidsTest() {

        ds.create(DidMethod.key)

        val dids = ds.listDids()

        assertTrue(dids.size > 0)

        dids.forEach { s -> assertEquals(s, DidUrl.from(s).did) }
    }

}
