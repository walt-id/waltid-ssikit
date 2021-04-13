package org.letstrust.examples


fun main() {

    // https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=2.+DID-registry+interaction
    println("ESSIF DID-registry interaction")

    ///////////////////////////////////////////////////////////////////////////
    // Generate ETH address + DID
    ///////////////////////////////////////////////////////////////////////////

    // val did = EssifService.generateDid("ebsi")

    ///////////////////////////////////////////////////////////////////////////
    // Obtain Verifiable Authorization by following steps
    // - Precondition: Access Token needs to be obtained via EU login
    // - Proof control over DID
    // - Receive/store Verifiable Authorisation
    ///////////////////////////////////////////////////////////////////////////

    // val verifiableAuthorization = EssifService.onboard(accessToken, did)

    ///////////////////////////////////////////////////////////////////////////
    // Register EBSI DID
    // - present Verifiable Authorisation to API
    // - Insert DID to the EBSI ledger
    // - Sign transaction
    ///////////////////////////////////////////////////////////////////////////

    // EssifService.registerDid(verifiableAuthorization, did)

}
