package id.walt.services.vcstore


import id.walt.credentials.w3c.VerifiableCredential
import id.walt.credentials.w3c.toVerifiableCredential
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

open class FileSystemVcStoreService : VcStoreService() {

    val store = File("data/credential-store").apply { mkdir() }

    private fun getGroupDir(group: String) = File(store.absolutePath, group).apply { mkdirs() }
    private fun getFileById(id: String, group: String) =
        File(getGroupDir(group), "${URLEncoder.encode(id, StandardCharsets.UTF_8)}.cred")

    private fun loadFileString(id: String, group: String) = getFileById(id, group).let {
        when (it.exists()) {
            true -> it.readText()
            false -> null
        }
    }

    override fun getCredential(id: String, group: String): VerifiableCredential? =
        loadFileString(id, group)?.toVerifiableCredential()

    override fun listCredentials(group: String): List<VerifiableCredential> =
        listCredentialIds(group).map { getCredential(it, group)!! }

    override fun listCredentialIds(group: String): List<String> = getGroupDir(group).listFiles()!!
        .map { URLDecoder.decode(it.nameWithoutExtension, StandardCharsets.UTF_8) }

    override fun storeCredential(alias: String, vc: VerifiableCredential, group: String) =
        getFileById(
            alias, group
        ).writeText(vc.encode())

    override fun deleteCredential(alias: String, group: String) = getFileById(alias, group).delete()
}
