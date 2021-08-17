package id.walt.services.vcstore

import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.model.VerifiableCredential
import java.io.File

open class FileSystemVcStoreService : VcStoreService() {

    val store = File("credential-store").apply { mkdir() }

    private fun getFileById(id: String) = File("${store.absolutePath}/$id.cred")
    private fun loadFileString(id: String) = getFileById(id).readText()

    override fun getCredential(id: String): VerifiableCredential = loadFileString(id).toCredential()

    override fun listCredentials(): List<VerifiableCredential> =
        listCredentialIds().map { loadFileString(it).toCredential() }

    override fun listCredentialIds(): List<String> = store.list()!!.asList()

    override fun storeCredential(alias: String, vc: VerifiableCredential) = getFileById(alias).writeText(vc.encode())
}
