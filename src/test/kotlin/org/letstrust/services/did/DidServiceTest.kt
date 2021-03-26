package org.letstrust.services.did

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import org.letstrust.model.DidMethod
import org.letstrust.model.DidUrl
import org.letstrust.model.toDidUrl
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

        val obj: DidUrl = didUrl.url.toDidUrl()
        assertEquals(didUrl, obj)
    }

    @Test
    fun createResolveDidKeyTest() {

        // Create
        val did = ds.create(DidMethod.key)
        assertNotNull(did)
        assertTrue(32 < did.length)
        assertEquals("did:key:", did.substring(0, 8))
        print(did)

        // Resolve
        val resolvedDid = ds.resolve(did)
        val encoded = Json { prettyPrint = true }.encodeToString(resolvedDid)
        println(encoded)
    }

}
