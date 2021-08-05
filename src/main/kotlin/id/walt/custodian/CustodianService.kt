package id.walt.custodian



interface CustodianService {
    fun storeCredential()
    fun loadCredential()
    fun createPresentation()
    fun importKey()
    fun exportKey()
}
