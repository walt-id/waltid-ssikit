package id.walt.services.vcstore

import id.walt.services.hkvstore.HierarchicalKeyValueStoreService
import id.walt.vclib.Helpers.encode
import id.walt.vclib.Helpers.toCredential
import id.walt.vclib.model.VerifiableCredential
import io.ktor.util.*
import java.nio.file.Path


class HKVVcStoreService : VcStoreService() {

    val hkvStore = HierarchicalKeyValueStoreService.getService()
    val vcRoot = Path.of("vc")

    private fun getGroupRoot(group: String): Path = vcRoot.combineSafe(Path.of(group)).toPath()

    private fun getStoreKeyFor(id: String, group: String): Path =
        getGroupRoot(group).combineSafe(Path.of("${id}.cred")).toPath()

    override fun getCredential(id: String, group: String): VerifiableCredential =
        hkvStore.getAsString(getStoreKeyFor(id, group)).toCredential()

    override fun listCredentials(group: String): List<VerifiableCredential> =
        listCredentialIds(group).map { hkvStore.getAsString(getStoreKeyFor(it, group)).toCredential() }

    override fun listCredentialIds(group: String): List<String> =
        hkvStore.getChildKeys(getGroupRoot(group)).map { it.toFile().nameWithoutExtension }

    override fun storeCredential(alias: String, vc: VerifiableCredential, group: String) =
        hkvStore.put(getStoreKeyFor(alias, group), vc.encode())

    override fun deleteCredential(alias: String, group: String): Boolean = hkvStore.delete(getStoreKeyFor(alias, group))
}
