package id.walt.services.vcstore

import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.model.VerifiableCredential
import java.io.File

open class FileSystemVcStoreService : VcStoreService() {

    val store = File("credential-store").apply { mkdir() }

    private fun getGroupDir(group: String) = File(store.absolutePath, group).apply { mkdirs() }
    private fun getFileById(id: String, group: String) = File(getGroupDir(group),"${id}.cred")
    private fun loadFileString(id: String, group: String) = getFileById(id, group).readText()

    override fun getCredential(id: String, group: String): VerifiableCredential = loadFileString(id, group).toCredential()

    override fun listCredentials(group: String): List<VerifiableCredential> =
        listCredentialIds(group).map { loadFileString(it, group).toCredential() }

    override fun listCredentialIds(group: String): List<String> = getGroupDir(group).listFiles()!!.map { it.nameWithoutExtension }

    override fun storeCredential(alias: String, vc: VerifiableCredential, group: String) = getFileById(alias, group).writeText(vc.encode())

    override fun deleteCredential(alias: String, group: String) = getFileById(alias, group).delete()
}
