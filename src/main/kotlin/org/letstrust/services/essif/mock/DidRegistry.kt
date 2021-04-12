package org.letstrust.services.essif.mock

import org.letstrust.common.readEssif

object DidRegistry {
    fun get(did: String): String {
        return "did"
    }

    fun insertDidDocument(): String {
        println("13 [POST] /insertDidDocument")
        println("14 Validate request")
        println("15 Generate <unsigned transaction>")
        return readEssif("")
    }

    fun signedTransaction() {
        TODO("Not yet implemented")
    }
}
