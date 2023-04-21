package id.walt.signatory.revocation.repository

import id.walt.common.KlaxonWithConverters
import id.walt.common.resolveContent
import id.walt.signatory.revocation.RevocationItem
import id.walt.signatory.revocation.RevocationLookupParameter
import id.walt.signatory.revocation.StatusListRevocationItem
import id.walt.signatory.revocation.StatusListRevocationLookupParameter
import java.io.File

interface RevocationRepository {
    fun all(): List<RevocationItem>
    fun clear()
    fun get(revocation: RevocationLookupParameter): RevocationItem?
    fun set(revocation: RevocationItem)
}

class StatusListRevocationRepository() : RevocationRepository {
    private val indexPath = ""

    override fun all(): List<RevocationItem> = resolveContent(indexPath).takeIf { !it.equals(indexPath, true) }?.let {
        KlaxonWithConverters().parseArray(it)!!
    } ?: emptyList()

    override fun clear() = updateIndex(emptyList())

    override fun set(revocation: RevocationItem) = remove(revocation).toMutableList().run {
        this.add(revocation)
        updateIndex(this)
    }

    override fun get(revocation: RevocationLookupParameter): RevocationItem? = all().find {
        (it as StatusListRevocationItem).credentialId == (revocation as StatusListRevocationLookupParameter).id
    }

    private fun remove(revocation: RevocationItem): List<RevocationItem> = all().toMutableList().let {
        it.removeIf {
            (it as StatusListRevocationItem).credentialId == (revocation as StatusListRevocationItem).credentialId
        }
        updateIndex(it)
        it
    }

    private fun updateIndex(list: List<RevocationItem>): Unit = let {
        File(indexPath).takeIf { it.exists() } ?: let {
            File(indexPath).createNewFile()
            File(indexPath)
        }.run {
            this.writeText(KlaxonWithConverters().toJsonString(list))
        }
    }
}
