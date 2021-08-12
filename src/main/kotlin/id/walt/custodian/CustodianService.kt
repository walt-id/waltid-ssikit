package id.walt.custodian

import id.walt.crypto.Key
import id.walt.crypto.KeyId
import id.walt.vclib.model.VerifiableCredential

interface CustodianService {
    fun getKey(keyId: KeyId): Key
    fun listKeys(): List<Key>
    fun storeKey(key: Key)

    fun getCredential(id: String): VerifiableCredential
    fun listCredentials(): List<VerifiableCredential>
    fun storeCredential(vc: VerifiableCredential)

    // fun createPresentation()
}
