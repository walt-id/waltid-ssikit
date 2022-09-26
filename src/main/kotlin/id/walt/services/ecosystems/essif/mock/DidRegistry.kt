package id.walt.services.ecosystems.essif.mock

object DidRegistry {
    fun get(did: String): String {
        return "did"
    }

    fun insertDidDocument(): String {
        println("13 [POST] /insertDidDocument")
        println("14 Validate request")
        println("15 Generate <unsigned transaction>")
        return "<unsigned transaction>"//readEssif("")
    }

    fun signedTransaction(signedTransaction: String) {
        // write DID to the ledger
    }
}
