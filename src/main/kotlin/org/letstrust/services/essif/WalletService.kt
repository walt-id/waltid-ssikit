package org.letstrust.services.essif

import org.letstrust.s.essif.EosService

object WalletService {

    fun didGeneration() {
        println("1. [Wallet] Generate ETH address (keys)")
        println("2. [Wallet] Generate DID Controlling Keys)")
        println("3. [Wallet] Store DID Controlling Private Key")
        println("4. [Wallet] Generate DID Document")
    }

    fun authorizationRequest() {
        println("5. [Wallet] POST /onboards")
        val didOwnershipReq = EosService.onboards()
        println("7. [Wallet] Signed Challenge")
        val verifiableAuthorization = EosService.signedChallenge("signedChallenge")
        println("12. [Wallet] 201 V. Authorization")
    }

    fun authorizationIssuance() {

    }

    fun requestVerifiableId() {
        val didOwnershipReq = EosService.requestVerifiableId()
        println("5. [Wallet] Request DID prove")
    }

    fun getVerifiableId(): String {
        val vIdRequest = EosService.didOwnershipResponse()
        EosService.getCredentials()
        println("13 [Wallet] 200 <V.ID>")
        return vIdRequest
    }
}
