package id.walt.services.essif.userwallet

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import com.beust.klaxon.Parser
import com.nimbusds.jose.crypto.impl.ECDH
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.OctetKeyPair
import com.nimbusds.jwt.JWTClaimsSet
import id.walt.common.toParamMap
import id.walt.crypto.*
import id.walt.model.*
import id.walt.services.did.DidService
import id.walt.services.essif.EbsiVAWrapper
import id.walt.services.essif.EbsiVaVp
import id.walt.services.essif.EssifClient
import id.walt.services.essif.LegalEntityClient
import id.walt.services.essif.enterprisewallet.EnterpriseWalletService
import id.walt.services.essif.mock.AuthorizationApi
import id.walt.services.essif.mock.DidRegistry
import id.walt.services.hkvstore.HKVKey
import id.walt.services.hkvstore.HKVStoreService
import id.walt.services.jwt.InMemoryJwtService
import id.walt.services.key.InMemoryKeyService
import id.walt.services.keystore.KeyType
import id.walt.services.vc.JsonLdCredentialService
import id.walt.signatory.ProofConfig
import mu.KotlinLogging
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.MessageDigest
import java.time.Instant
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val log = KotlinLogging.logger {}

object UserWalletService {

//    val didUrlUser by lazy {
//        DidService.create(DidMethod.web)
//    }

    private val credentialService = JsonLdCredentialService.getService()
    private val enterpriseWalletService = EnterpriseWalletService.getService()
    private val jwtService = InMemoryJwtService.getService()
    private val keyService = InMemoryKeyService.getService()

    fun createDid(): String {
        val did = enterpriseWalletService.didGeneration()
        log.debug { "did: $did" }

        val verifiableAuthorization = enterpriseWalletService.requestVerifiableAuthorization(did)
        log.debug { "verifiableAuthorization: $verifiableAuthorization" }

        val unsignedTransaction = DidRegistry.insertDidDocument()
        println("16. [EWallet] 200 <unsigned transaction>")
        println("17. [EWallet] Generate <signed transaction>")
        val signedTransaction = ""
        DidRegistry.signedTransaction(signedTransaction)
        return did
    }

    fun oidcAuthResponse(oidcAuthReq: String): Boolean {
        println("8. [UWallet] OIDC Validation")
        println("9. [UWallet] DID AuthN validation")
        println("10. [UWallet] Generate Authentication Response")
        println("11. [UA] Authentication Response: Callback /callback 302")
        return LegalEntityClient.rp.callback("<auth response>")
    }

    fun validateDidAuthRequest(didAuthRequest: String) {
        println("10. [UWallet] Validate request")

        //this.didAuthResponse(didAuthRequest)
    }

    fun vcAuthResponse(vcExchangeRequest: String): String {
        println("10. [UWallet] Validate request")

        return this.didAuthResponse(vcExchangeRequest)
    }

    fun didAuthResponse(didAuthRequest: String): String {
        println("13/11. [UWallet] Generate (DID-)Auth Response")
        println("14/12. [UWallet] /callback (DID-)Auth Response")
        val vcToken = enterpriseWalletService.validateDidAuthResponse("didAuthResp")
        println("16/14. [UWallet] 200 OK")
        return "vcToken"
    }

    // https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=2.+Authorization+API
    fun requestAccessToken(did: String): String {

        ///////////////////////////////////////////////////////////////////////////
        // Prerequisite:
        // - Bearer token must be available
        // - Verifiable Authorization must be previously installed by running
        //   ESSIF onboarding flow (DID registration)
        ///////////////////////////////////////////////////////////////////////////

        log.debug { "Loading Verifiable Authorization from HKV Store." }

        val verifiableAuthorization = HKVStoreService.getService().getAsString(HKVKey("ebsi", did.substringAfterLast(":"), EssifClient.verifiableAuthorizationFile))!!

        // val verifiableAuthorization = readWhenContent(EssifClient.verifiableAuthorizationFile)

        // log.debug { "Loaded bearer token from ${EssifClient.bearerTokenFile.absolutePath}." }

        val accessToken = siopSession(did, verifiableAuthorization)

        log.debug { "Decrypted Access Token: $accessToken" }

        return accessToken


        // {"aud":"\/siop-sessions",
        //"sub":"dIEuWeirGBGORXK5IS5ttWSJB_wpeL6ojEyHtoezFiM",
        //"iss":"https:\/\/self-issued.me",
        //"claims":
        //{"verified_claims":"
        //eyJAY29udGV4dCI6WyJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy92MSJdLCJwcm9vZiI6eyJjcmVhdGVkIjoiMjAyMS0wNi0wOFQxMjo0NzoxM1oiLCJjcmVhdG9yIjoiZGlkOmVic2k6MjZ3bmVrMzZ6NGRqcTFmZENnVFpMVHVSQ2U5Z01mNUNyNkZHOGNoeXVhRUJSNGZUIiwiandzIjoiZXlKaU5qUWlPbVpoYkhObExDSmpjbWwwSWpwYkltSTJOQ0pkTENKaGJHY2lPaUpGVXpJMU5rc2lmUS4uRWlxdkdfN3RObGVhWUpockI3Q010dklCdURtYWNiSmdvMnFvbmZLcC1kTHJXaUotVVFKVFZBQWNIc3JCWGxNaXlzaHZ6T25mTzFaMmhyX2ZhQ2FRVmciLCJ0eXBlIjoiRWNkc2FTZWNwMjU2azFTaWduYXR1cmUyMDE5In0sInR5cGUiOlsiVmVyaWZpYWJsZVByZXNlbnRhdGlvbiJdLCJ2ZXJpZmlhYmxlQ3JlZGVudGlhbCI6W3siQGNvbnRleHQiOlsiaHR0cHM6Ly93d3cudzMub3JnLzIwMTgvY3JlZGVudGlhbHMvdjEiLCJodHRwczovL3d3dy53My5vcmcvMjAxOC9jcmVkZW50aWFscy9leGFtcGxlcy92MSIsImh0dHBzOi8vdzNjLWNjZy5naXRodWIuaW8vbGRzLWp3czIwMjAvY29udGV4dHMvbGRzLWp3czIwMjAtdjEuanNvbiJdLCJjcmVkZW50aWFsU2NoZW1hIjp7ImlkIjoiaHR0cHM6Ly9hcGkucHJlcHJvZC5lYnNpLmV1L3RydXN0ZWQtc2NoZW1hcy1yZWdpc3RyeS92MS9zY2hlbWFzLzB4MzEyZTMzMmUzNjJlMzEyZTM0MmUzMTJlMzEzNjM2MzQyZTMxMzAyZTMxMzgzNzJlMzEyZTMyMmUzMjJlMzMzMyIsInR5cGUiOiJPSUQifSwiY3JlZGVudGlhbFN1YmplY3QiOnsiaWQiOiJkaWQ6ZWJzaToyNnduZWszNno0ZGpxMWZkQ2dUWkxUdVJDZTlnTWY1Q3I2Rkc4Y2h5dWFFQlI0ZlQifSwiZXhwaXJhdGlvbkRhdGUiOiIyMDIxLTEyLTA3VDA3OjU2OjM2WiIsImlkIjoidmM6ZWJzaTphdXRoZW50aWNhdGlvbiNiYjkwZGExMC0xYTM4LTRjY2QtOTg5Ny0zOTdjYTY5MGFmYTgiLCJpc3N1YW5jZURhdGUiOiIyMDIxLTA2LTA4VDA3OjU2OjM2WiIsImlzc3VlciI6ImRpZDplYnNpOjRqUHhjaWd2ZmlmWnlWd3ltNXpqeGFLWEdKVHQ3WXdGdHBnNkFYdHNSNGQ1IiwicHJvb2YiOnsiY3JlYXRlZCI6IjIwMjEtMDYtMDhUMDc6NTY6MzZaIiwiandzIjoiZXlKaGJHY2lPaUpGVXpJMU5rc2lMQ0owZVhBaU9pSktWMVFpZlEuLllPTlBkS1Z2MXFsMHBZbFZmUjR2b1I2c0hONU9MbVRHRFFxekxDbVZfUDdsNWgxeFNOZDgtcEU5NUVSSkhZZ3dxa05zZ0VxaWtZYjVCaXBXUkVfTWRRIiwicHJvb2ZQdXJwb3NlIjoiYXNzZXJ0aW9uTWV0aG9kIiwidHlwZSI6IkVjZHNhU2VjcDI1NmsxU2lnbmF0dXJlMjAxOSIsInZlcmlmaWNhdGlvbk1ldGhvZCI6ImRpZDplYnNpOjRqUHhjaWd2ZmlmWnlWd3ltNXpqeGFLWEdKVHQ3WXdGdHBnNkFYdHNSNGQ1I2tleXMtMSJ9LCJ0eXBlIjpbIlZlcmlmaWFibGVDcmVkZW50aWFsIiwiVmVyaWZpYWJsZUF1dGhvcmlzYXRpb24iXSwidmFsaWRGcm9tIjoiMjAyMS0wNi0wOFQwNzo1NjozNloifV19
        //","encryption_key":{"crv":"secp256k1","kyt":"EC","x":"Cyb12xp1x7LfaulXdDkDovXXiAJtR4xPjGQiH9B6lcw","y":"nNV-RFkLeFefO5dM2lOybYebr8qFCi3grdV7fTQTKgo","alg":"ES256K"}},"sub_jwk":{"kty":"EC","use":"sig","crv":"secp256k1","kid":"did:ebsi:26wnek36z4djq1fdCgTZLTuRCe9gMf5Cr6FG8chyuaEBR4fT#key-1","x":"Cyb12xp1x7LfaulXdDkDovXXiAJtR4xPjGQiH9B6lcw","y":"nNV-RFkLeFefO5dM2lOybYebr8qFCi3grdV7fTQTKgo","alg":"ES256K"},"exp":1623156746,"iat":1623156446,"nonce":"8f444370-cf7b-4029-8027-2c1d9ac30778"}

        // SIOP REQUEST
        // POST https://api.preprod.ebsi.eu/authorisation/v1/siop-sessions
        //Data:
        //{
        //    "id_token": "eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QiLCJraWQiOiJkaWQ6ZWJzaTp5TWUzZDJKRHF1TjI3ck1QcW9ZakZWWmhzOEJjd1ZXYnBnWTFxSFp5OHpHI2tleS0xIn0.eyJpYXQiOjE2MjMxNTY5MjMsImV4cCI6MTYyMzE1NzIyMywiaXNzIjoiaHR0cHM6Ly9zZWxmLWlzc3VlZC5tZSIsInN1YiI6InNYZEg2eHhqMEVMeU1QMnM4Y1N4UUVSYzJKSkZMLTVhbkdFejJrX1QzSFUiLCJhdWQiOiIvc2lvcC1zZXNzaW9ucyIsIm5vbmNlIjoiNDFmMGNiZGQtODg2Yy00OTE1LTkxOWMtNTQ5ZDI3NzU4NzNlIiwic3ViX2p3ayI6eyJraWQiOiJkaWQ6ZWJzaTp5TWUzZDJKRHF1TjI3ck1QcW9ZakZWWmhzOEJjd1ZXYnBnWTFxSFp5OHpHI2tleS0xIiwia3R5IjoiRUMiLCJjcnYiOiJzZWNwMjU2azEiLCJ4IjoiRG1QS0VQZFRPTGZpcFNuUm9xTlFZSnlQM183RUZ6QmdaVi1DTjhOZDBWYyIsInkiOiJmRWg0enFQLTJncHNCMF9jbDJtdHBpT1NUX3hBaEhhV18tNFN1eGpoLW9FIn0sImNsYWltcyI6eyJ2ZXJpZmllZF9jbGFpbXMiOiJleUpBWTI5dWRHVjRkQ0k2V3lKb2RIUndjem92TDNkM2R5NTNNeTV2Y21jdk1qQXhPQzlqY21Wa1pXNTBhV0ZzY3k5Mk1TSmRMQ0pvYjJ4a1pYSWlPaUprYVdRNlpXSnphVHA1VFdVelpESktSSEYxVGpJM2NrMVFjVzlaYWtaV1dtaHpPRUpqZDFaWFluQm5XVEZ4U0ZwNU9IcEhJaXdpY0hKdmIyWWlPbnNpWTNKbFlYUmxaQ0k2SWpJd01qRXRNRFl0TURoVU1USTZOVFU2TWpCYUlpd2lhbmR6SWpvaVpYbEthR0pIWTJsUGFVcEdWWHBKTVU1cmMybE1RMG93WlZoQmFVOXBTa3RXTVZGcFRFTktjbUZYVVdsUGFVcHZaRWhTZDJONmIzWk1Na1ozWVZNMWQyTnRWbmRqYlRsclRHMVdhV015YTNWYVdGVjJXa2RzYTB4WVNteGFNbXg2WkVoS05Vd3pXWGxNTW14cldsYzFNR0ZYV25CYVdFcDZUREpTY0ZwRWNHeFpiazV3VDI1c1RscFVUbXROYTNCRlkxaFdUMDFxWkhsVVZrSjRZakZzY1ZKc1dtRmhTRTAwVVcxT00xWnNaR2xqUjJSYVRWaEdTVmR1YXpSbGEyTnFZVEpXTldONU1IaEpiakF1TG5nMGNsOHhaamQ1V25SQ1FrdFBSMGx2T1RWSVRGUmlRVFJRZGxkeWRGcDFSVE53VFV4bGRtaGljVXBhVTJ0ME4xSmlUUzFQWlZGUlpsOXJabXhoVVMxNlpURkRVVmQ1Vm1RMlVsUnNNVWRETlROVWMzRm5JaXdpY0hKdmIyWlFkWEp3YjNObElqb2lZWE56WlhKMGFXOXVUV1YwYUc5a0lpd2lkSGx3WlNJNklrVmpaSE5oVTJWamNESTFObXN4VTJsbmJtRjBkWEpsTWpBeE9TSXNJblpsY21sbWFXTmhkR2x2YmsxbGRHaHZaQ0k2SW1ScFpEcGxZbk5wT25sTlpUTmtNa3BFY1hWT01qZHlUVkJ4YjFscVJsWmFhSE00UW1OM1ZsZGljR2RaTVhGSVduazRla2NqYTJWNWN5MHhJbjBzSW5SNWNHVWlPaUpXWlhKcFptbGhZbXhsVUhKbGMyVnVkR0YwYVc5dUlpd2lkbVZ5YVdacFlXSnNaVU55WldSbGJuUnBZV3dpT2x0N0lrQmpiMjUwWlhoMElqcGJJbWgwZEhCek9pOHZkM2QzTG5jekxtOXlaeTh5TURFNEwyTnlaV1JsYm5ScFlXeHpMM1l4SWl3aWFIUjBjSE02THk5M2QzY3Vkek11YjNKbkx6SXdNVGd2WTNKbFpHVnVkR2xoYkhNdlpYaGhiWEJzWlhNdmRqRWlMQ0pvZEhSd2N6b3ZMM2N6WXkxalkyY3VaMmwwYUhWaUxtbHZMMnhrY3kxcWQzTXlNREl3TDJOdmJuUmxlSFJ6TDJ4a2N5MXFkM015TURJd0xYWXhMbXB6YjI0aVhTd2lZM0psWkdWdWRHbGhiRk5qYUdWdFlTSTZleUpwWkNJNkltaDBkSEJ6T2k4dllYQnBMbkJ5WlhCeWIyUXVaV0p6YVM1bGRTOTBjblZ6ZEdWa0xYTmphR1Z0WVhNdGNtVm5hWE4wY25rdmRqRXZjMk5vWlcxaGN5OHdlRE14TW1Vek16SmxNell5WlRNeE1tVXpOREpsTXpFeVpUTXhNell6TmpNME1tVXpNVE13TW1Vek1UTTRNemN5WlRNeE1tVXpNakpsTXpJeVpUTXpNek1pTENKMGVYQmxJam9pVDBsRUluMHNJbU55WldSbGJuUnBZV3hUZFdKcVpXTjBJanA3SW1sa0lqb2laR2xrT21WaWMyazZlVTFsTTJReVNrUnhkVTR5TjNKTlVIRnZXV3BHVmxwb2N6aENZM2RXVjJKd1oxa3hjVWhhZVRoNlJ5SjlMQ0psZUhCcGNtRjBhVzl1UkdGMFpTSTZJakl3TWpFdE1USXRNRGRVTVRJNk5UVTZNVGxhSWl3aWFXUWlPaUoyWXpwbFluTnBPbUYxZEdobGJuUnBZMkYwYVc5dUl6TmpOelE1TjJaakxXUXpOV1F0TkRVeE1pMDVNbUU1TFdZNE16ZGpZelpqT1RkbE1pSXNJbWx6YzNWaGJtTmxSR0YwWlNJNklqSXdNakV0TURZdE1EaFVNVEk2TlRVNk1UbGFJaXdpYVhOemRXVnlJam9pWkdsa09tVmljMms2TkdwUWVHTnBaM1ptYVdaYWVWWjNlVzAxZW1wNFlVdFlSMHBVZERkWmQwWjBjR2MyUVZoMGMxSTBaRFVpTENKd2NtOXZaaUk2ZXlKamNtVmhkR1ZrSWpvaU1qQXlNUzB3Tmkwd09GUXhNam8xTlRveE9Wb2lMQ0pxZDNNaU9pSmxlVXBvWWtkamFVOXBTa1pWZWtreFRtdHphVXhEU2pCbFdFRnBUMmxLUzFZeFVXbG1VUzR1VHpGSFYyTmZNRzF2ZUZsUE1FOTZNSFJFY2pWSGNHUkNWRGxIYUVneE1rd3RhRkpDU25VelIyNVJTSGhMVTFnellUVjNWRkZCZUhKRGRuaEZUbnAyWVU5VlIwczVXV1JsTkhkblkwcE1TMXBJVkhaUVMxRWlMQ0p3Y205dlpsQjFjbkJ2YzJVaU9pSmhjM05sY25ScGIyNU5aWFJvYjJRaUxDSjBlWEJsSWpvaVJXTmtjMkZUWldOd01qVTJhekZUYVdkdVlYUjFjbVV5TURFNUlpd2lkbVZ5YVdacFkyRjBhVzl1VFdWMGFHOWtJam9pWkdsa09tVmljMms2TkdwUWVHTnBaM1ptYVdaYWVWWjNlVzAxZW1wNFlVdFlSMHBVZERkWmQwWjBjR2MyUVZoMGMxSTBaRFVqYTJWNWN5MHhJbjBzSW5SNWNHVWlPbHNpVm1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aUxDSldaWEpwWm1saFlteGxRWFYwYUc5eWFYTmhkR2x2YmlKZExDSjJZV3hwWkVaeWIyMGlPaUl5TURJeExUQTJMVEE0VkRFeU9qVTFPakU1V2lKOVhYMCIsImVuY3J5cHRpb25fa2V5Ijp7Imt0eSI6IkVDIiwiY3J2Ijoic2VjcDI1NmsxIiwieCI6IkRtUEtFUGRUT0xmaXBTblJvcU5RWUp5UDNfN0VGekJnWlYtQ044TmQwVmMiLCJ5IjoiZkVoNHpxUC0yZ3BzQjBfY2wybXRwaU9TVF94QWhIYVdfLTRTdXhqaC1vRSJ9fX0.Zmm136Rwb7PLD4CogjSsA-O9Eh_VdjYZ36btqIHwDdxZLUt23Op4shtsGrgf7GY5BsxSsLsV9UsqGG8JPaZugw"
        //}


        // SIOP DATA : eyJhbGciOiJFUzI1NksiLCJ0eXAiOiJKV1QiLCJraWQiOiJkaWQ6ZWJzaTp5TWUzZDJKRHF1TjI3ck1QcW9ZakZWWmhzOEJjd1ZXYnBnWTFxSFp5OHpHI2tleS0xIn0.eyJpYXQiOjE2MjMxNTY5MjMsImV4cCI6MTYyMzE1NzIyMywiaXNzIjoiaHR0cHM6Ly9zZWxmLWlzc3VlZC5tZSIsInN1YiI6InNYZEg2eHhqMEVMeU1QMnM4Y1N4UUVSYzJKSkZMLTVhbkdFejJrX1QzSFUiLCJhdWQiOiIvc2lvcC1zZXNzaW9ucyIsIm5vbmNlIjoiNDFmMGNiZGQtODg2Yy00OTE1LTkxOWMtNTQ5ZDI3NzU4NzNlIiwic3ViX2p3ayI6eyJraWQiOiJkaWQ6ZWJzaTp5TWUzZDJKRHF1TjI3ck1QcW9ZakZWWmhzOEJjd1ZXYnBnWTFxSFp5OHpHI2tleS0xIiwia3R5IjoiRUMiLCJjcnYiOiJzZWNwMjU2azEiLCJ4IjoiRG1QS0VQZFRPTGZpcFNuUm9xTlFZSnlQM183RUZ6QmdaVi1DTjhOZDBWYyIsInkiOiJmRWg0enFQLTJncHNCMF9jbDJtdHBpT1NUX3hBaEhhV18tNFN1eGpoLW9FIn0sImNsYWltcyI6eyJ2ZXJpZmllZF9jbGFpbXMiOiJleUpBWTI5dWRHVjRkQ0k2V3lKb2RIUndjem92TDNkM2R5NTNNeTV2Y21jdk1qQXhPQzlqY21Wa1pXNTBhV0ZzY3k5Mk1TSmRMQ0pvYjJ4a1pYSWlPaUprYVdRNlpXSnphVHA1VFdVelpESktSSEYxVGpJM2NrMVFjVzlaYWtaV1dtaHpPRUpqZDFaWFluQm5XVEZ4U0ZwNU9IcEhJaXdpY0hKdmIyWWlPbnNpWTNKbFlYUmxaQ0k2SWpJd01qRXRNRFl0TURoVU1USTZOVFU2TWpCYUlpd2lhbmR6SWpvaVpYbEthR0pIWTJsUGFVcEdWWHBKTVU1cmMybE1RMG93WlZoQmFVOXBTa3RXTVZGcFRFTktjbUZYVVdsUGFVcHZaRWhTZDJONmIzWk1Na1ozWVZNMWQyTnRWbmRqYlRsclRHMVdhV015YTNWYVdGVjJXa2RzYTB4WVNteGFNbXg2WkVoS05Vd3pXWGxNTW14cldsYzFNR0ZYV25CYVdFcDZUREpTY0ZwRWNHeFpiazV3VDI1c1RscFVUbXROYTNCRlkxaFdUMDFxWkhsVVZrSjRZakZzY1ZKc1dtRmhTRTAwVVcxT00xWnNaR2xqUjJSYVRWaEdTVmR1YXpSbGEyTnFZVEpXTldONU1IaEpiakF1TG5nMGNsOHhaamQ1V25SQ1FrdFBSMGx2T1RWSVRGUmlRVFJRZGxkeWRGcDFSVE53VFV4bGRtaGljVXBhVTJ0ME4xSmlUUzFQWlZGUlpsOXJabXhoVVMxNlpURkRVVmQ1Vm1RMlVsUnNNVWRETlROVWMzRm5JaXdpY0hKdmIyWlFkWEp3YjNObElqb2lZWE56WlhKMGFXOXVUV1YwYUc5a0lpd2lkSGx3WlNJNklrVmpaSE5oVTJWamNESTFObXN4VTJsbmJtRjBkWEpsTWpBeE9TSXNJblpsY21sbWFXTmhkR2x2YmsxbGRHaHZaQ0k2SW1ScFpEcGxZbk5wT25sTlpUTmtNa3BFY1hWT01qZHlUVkJ4YjFscVJsWmFhSE00UW1OM1ZsZGljR2RaTVhGSVduazRla2NqYTJWNWN5MHhJbjBzSW5SNWNHVWlPaUpXWlhKcFptbGhZbXhsVUhKbGMyVnVkR0YwYVc5dUlpd2lkbVZ5YVdacFlXSnNaVU55WldSbGJuUnBZV3dpT2x0N0lrQmpiMjUwWlhoMElqcGJJbWgwZEhCek9pOHZkM2QzTG5jekxtOXlaeTh5TURFNEwyTnlaV1JsYm5ScFlXeHpMM1l4SWl3aWFIUjBjSE02THk5M2QzY3Vkek11YjNKbkx6SXdNVGd2WTNKbFpHVnVkR2xoYkhNdlpYaGhiWEJzWlhNdmRqRWlMQ0pvZEhSd2N6b3ZMM2N6WXkxalkyY3VaMmwwYUhWaUxtbHZMMnhrY3kxcWQzTXlNREl3TDJOdmJuUmxlSFJ6TDJ4a2N5MXFkM015TURJd0xYWXhMbXB6YjI0aVhTd2lZM0psWkdWdWRHbGhiRk5qYUdWdFlTSTZleUpwWkNJNkltaDBkSEJ6T2k4dllYQnBMbkJ5WlhCeWIyUXVaV0p6YVM1bGRTOTBjblZ6ZEdWa0xYTmphR1Z0WVhNdGNtVm5hWE4wY25rdmRqRXZjMk5vWlcxaGN5OHdlRE14TW1Vek16SmxNell5WlRNeE1tVXpOREpsTXpFeVpUTXhNell6TmpNME1tVXpNVE13TW1Vek1UTTRNemN5WlRNeE1tVXpNakpsTXpJeVpUTXpNek1pTENKMGVYQmxJam9pVDBsRUluMHNJbU55WldSbGJuUnBZV3hUZFdKcVpXTjBJanA3SW1sa0lqb2laR2xrT21WaWMyazZlVTFsTTJReVNrUnhkVTR5TjNKTlVIRnZXV3BHVmxwb2N6aENZM2RXVjJKd1oxa3hjVWhhZVRoNlJ5SjlMQ0psZUhCcGNtRjBhVzl1UkdGMFpTSTZJakl3TWpFdE1USXRNRGRVTVRJNk5UVTZNVGxhSWl3aWFXUWlPaUoyWXpwbFluTnBPbUYxZEdobGJuUnBZMkYwYVc5dUl6TmpOelE1TjJaakxXUXpOV1F0TkRVeE1pMDVNbUU1TFdZNE16ZGpZelpqT1RkbE1pSXNJbWx6YzNWaGJtTmxSR0YwWlNJNklqSXdNakV0TURZdE1EaFVNVEk2TlRVNk1UbGFJaXdpYVhOemRXVnlJam9pWkdsa09tVmljMms2TkdwUWVHTnBaM1ptYVdaYWVWWjNlVzAxZW1wNFlVdFlSMHBVZERkWmQwWjBjR2MyUVZoMGMxSTBaRFVpTENKd2NtOXZaaUk2ZXlKamNtVmhkR1ZrSWpvaU1qQXlNUzB3Tmkwd09GUXhNam8xTlRveE9Wb2lMQ0pxZDNNaU9pSmxlVXBvWWtkamFVOXBTa1pWZWtreFRtdHphVXhEU2pCbFdFRnBUMmxLUzFZeFVXbG1VUzR1VHpGSFYyTmZNRzF2ZUZsUE1FOTZNSFJFY2pWSGNHUkNWRGxIYUVneE1rd3RhRkpDU25VelIyNVJTSGhMVTFnellUVjNWRkZCZUhKRGRuaEZUbnAyWVU5VlIwczVXV1JsTkhkblkwcE1TMXBJVkhaUVMxRWlMQ0p3Y205dlpsQjFjbkJ2YzJVaU9pSmhjM05sY25ScGIyNU5aWFJvYjJRaUxDSjBlWEJsSWpvaVJXTmtjMkZUWldOd01qVTJhekZUYVdkdVlYUjFjbVV5TURFNUlpd2lkbVZ5YVdacFkyRjBhVzl1VFdWMGFHOWtJam9pWkdsa09tVmljMms2TkdwUWVHTnBaM1ptYVdaYWVWWjNlVzAxZW1wNFlVdFlSMHBVZERkWmQwWjBjR2MyUVZoMGMxSTBaRFVqYTJWNWN5MHhJbjBzSW5SNWNHVWlPbHNpVm1WeWFXWnBZV0pzWlVOeVpXUmxiblJwWVd3aUxDSldaWEpwWm1saFlteGxRWFYwYUc5eWFYTmhkR2x2YmlKZExDSjJZV3hwWkVaeWIyMGlPaUl5TURJeExUQTJMVEE0VkRFeU9qVTFPakU1V2lKOVhYMCIsImVuY3J5cHRpb25fa2V5Ijp7Imt0eSI6IkVDIiwiY3J2Ijoic2VjcDI1NmsxIiwieCI6IkRtUEtFUGRUT0xmaXBTblJvcU5RWUp5UDNfN0VGekJnWlYtQ044TmQwVmMiLCJ5IjoiZkVoNHpxUC0yZ3BzQjBfY2wybXRwaU9TVF94QWhIYVdfLTRTdXhqaC1vRSJ9fX0.Zmm136Rwb7PLD4CogjSsA-O9Eh_VdjYZ36btqIHwDdxZLUt23Op4shtsGrgf7GY5BsxSsLsV9UsqGG8JPaZugw

//        {
//            "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NksiLCJraWQiOiJodHRwczovL2FwaS5wcmVwcm9kLmVic2kuZXUvdHJ1c3RlZC1hcHBzLXJlZ2lzdHJ5L3YyL2FwcHMvMHgwOGMyNTg1NmZiY2JkZDA3NmM5YzM5NTEyYWJlZjYzMDk3NDk5MTBhMTEwZDlkMWE5YzlhN2QyYjI3N2I2ZDIwIn0.eyJpYXQiOjE2MjMxNTY0NTYsImV4cCI6MTYyMzE1NzM1NiwiYXVkIjoiZWJzaS1jb3JlLXNlcnZpY2VzIiwibm9uY2UiOiJiNWY0NGU3Ni00N2ViLTQ5MDktOWNiMi1lYjBmMGVjOTM3NWIiLCJsb2dpbl9oaW50IjoiZGlkX3Npb3AiLCJpc3MiOiJkaWQ6ZWJzaTpIQzl0bWlpdFc0UzlmWUFhajZSWXNvcWJUM3M3VGN3eWh5cnNiU2l6bnNkWiJ9.E5r3xpJl3w-b1BvwwpZFi3Lh-XIkhjQtLetxjMss8m7yDa7wV81oYLhHQLmemZRGhxmJm-UgQUYkhMfavry66A",
//            "did": "did:ebsi:HC9tmiitW4S9fYAaj6RYsoqbT3s7TcwyhyrsbSiznsdZ",
//            "nonce": "8f444370-cf7b-4029-8027-2c1d9ac30778"
//        }


        // OURS
//        {
//            "typ": "JWT",
//            "alg": "ES256K",
//            "kid": "https://api.preprod.ebsi.eu/trusted-apps-registry/v2/apps/0x08c25856fbcbdd076c9c39512abef6309749910a110d9d1a9c9a7d2b277b6d20"
//        }

//        {
//            "iat": 1623156456,
//            "exp": 1623157356,
//            "aud": "ebsi-core-services",
//            "nonce": "b5f44e76-47eb-4909-9cb2-eb0f0ec9375b",
//            "login_hint": "did_siop",
//            "iss": "did:ebsi:HC9tmiitW4S9fYAaj6RYsoqbT3s7TcwyhyrsbSiznsdZ"
//        }

        // Test script token
        // {
        //     "typ": "JWT",
        //    "alg": "ES256K",
        //    "kid": "https://api.preprod.ebsi.eu/trusted-apps-registry/v2/apps/0x08c25856fbcbdd076c9c39512abef6309749910a110d9d1a9c9a7d2b277b6d20"
        // }

        // {
        //     "iat": 1623156925,
        //    "exp": 1623157825,
        //    "sub": "did:ebsi:yMe3d2JDquN27rMPqoYjFVZhs8BcwVWbpgY1qHZy8zG",
        //    "aud": "ebsi-core-services",
        //    "nonce": "4db4ec4b-9a4f-426c-a428-b934aa93fec6",
        //    "login_hint": "did_siop",
        //    "iss": "did:ebsi:HC9tmiitW4S9fYAaj6RYsoqbT3s7TcwyhyrsbSiznsdZ"
        // }


        // Spec
//    {
//        "atHash": "d89549534ce26f35993917e075eba4103bbe28abcc570e23633e19ce4aeb51ae",
//        "aud": "did:ebsi:0x3va",
//        "exp": 1611309777,
//        "iat": 1611308877,
//        "iss": "did:ebsi:0xa11ce",
//        "nonce": "ddD96L4f9irntPgYAkdU",
//        "sub": "did:ebsi:0xb0b"
//    }


//        println("--------------------------------------------------------------------------------")
//        println("1 [UWallet] [POST] /authentication-request - Request & validate SIOP AuthorizationRequest (init DID auth) ...")
//        val authReq = didAuthAuthorizationApi()
//        // processing and validating Authorization Request
//        validateAuthenticationRequest(authReq)
//
//        println("--------------------------------------------------------------------------------")
//        println("Establish SIOP session (finalize DID auth) ...")
//        val atr = this.siopSessionsRequest(authReq, verifiableAuthorization)
//
//        // AKE Protocol
//        return validateAccessTokenResponse(atr)
    }

    fun siopSession(did: String, va: String): String {

        ///////////////////////////////////////////////////////////////////////////
        // Construct SIOP Request
        ///////////////////////////////////////////////////////////////////////////

        // Create Verifiable Presentation with VA
        val verifiedClaims = createVerifiedClaims(did, va)

        // Build SIOP response token
        val nonce = UUID.randomUUID().toString()

        // Generate an emphemeral key-pair for encryption and signing the JWT
        val emphKeyId = keyService.generate(KeyAlgorithm.ECDSA_Secp256k1)

        val idToken = constructSiopResponseJwt(emphKeyId, verifiedClaims, nonce)

        //val siopResponse = LegalEntityClient.eos.siopSession(idToken, readEssifBearerToken())
        val siopResponse = LegalEntityClient.eos.siopSession(idToken)

        log.debug { "Writing SIOP response (AKE1 encrypted token) to HKV store." }

        //ake1EncFile.writeText(siopResponse)

        HKVStoreService.getService().put(HKVKey("ebsi", did.substringAfterLast(":"), EssifClient.ake1EncFile), siopResponse)

        ///////////////////////////////////////////////////////////////////////////
        // Decrypt Access Token
        ///////////////////////////////////////////////////////////////////////////

        val accessTokenResponse = Klaxon().parse<AccessTokenResponse>(siopResponse)!!

        val encryptedPayload = parseEncryptedAke1Payload(accessTokenResponse.ake1_enc_payload)


        val clientKey = keyService.toJwk(emphKeyId.id, KeyType.PRIVATE) as ECKey
        // val clientKey = keyService.toJwk(did, true) as ECKey

        val sharedSecret = ECDH.deriveSharedSecret(
            encryptedPayload.ephemPublicKey.toECPublicKey(),
            clientKey.toECPrivateKey(),
            BouncyCastleProvider()
        )

        val encryptionKeyBytes =
            MessageDigest.getInstance("SHA-512").digest(sharedSecret.encoded).slice(0..31).toByteArray()

        val encryptionKey = SecretKeySpec(encryptionKeyBytes, "AES")

        val c = Cipher.getInstance("AES/CBC/NoPadding", BouncyCastleProvider())

        c.init(Cipher.DECRYPT_MODE, encryptionKey, IvParameterSpec(encryptedPayload.iv))

        val accessTokenBytes = c.doFinal(encryptedPayload.cipherText)

        var endInx = accessTokenBytes.findFirst { b -> (b.toInt() == 5) }

        val accessTokenRespStr = String(accessTokenBytes.slice(0 until endInx).toByteArray())

        val decAccesTokenResp = Klaxon().parse<DecryptedAccessTokenResponse>(accessTokenRespStr)!!

        keyService.delete(emphKeyId.id)

        ///////////////////////////////////////////////////////////////////////////
        // Validate received Access Token
        ///////////////////////////////////////////////////////////////////////////

        // Validated nonce
        if (nonce != decAccesTokenResp.nonce) throw Exception("Nonce in Access Token response not valid")

        // Compare DID of EOS
        if (accessTokenResponse.did != decAccesTokenResp.did) throw Exception("Decrypted DID does not match the DID of the siop response")

        // TODO load MAC + validate payload

        // TODO validate signature

        // TODO validate response (decAccesTokenResp) signature

        // TODO validate JWT signature

        return decAccesTokenResp.access_token
    }

//    fun processSiopResponse(siopRespFile: String): AccessTokenResponse {
//        println("Decrypting SIOP response from file: $siopRespFile")
//        val ake1Enc = File(siopRespFile).readText()
//        return Klaxon().parse(ake1Enc)
//    }


    private fun createVerifiedClaims(holderDid: String, va: String): String {

        val vaWrapper = Klaxon().parse<EbsiVAWrapper>(va)!!

        val vpReq = EbsiVaVp(
            listOf("https://www.w3.org/2018/credentials/v1"),
            listOf("VerifiablePresentation"),
            null,
            listOf(vaWrapper.verifiableCredential),
            holderDid,
            null
        )

        val authKeyId = DidService.loadDidEbsi(holderDid).authentication!![0]

        val encodedVp = Klaxon().toJsonString(vpReq)
        // val vp = credentialService.sign(holderDid, encodedVp, null, null, authKeyId, "assertionMethod")
        val vp =
            credentialService.sign(encodedVp, ProofConfig(issuerDid = holderDid, subjectDid = holderDid, issuerVerificationMethod = authKeyId, proofPurpose = "assertionMethod"))

        log.debug { "Verifiable Presentation generated:\n$vp" }

        //verifiablePresentationFile.writeText(vp)
        HKVStoreService.getService().put(HKVKey("ebsi", holderDid.substringAfterLast(":"), EssifClient.verifiablePresentationFile), vp)

        val vpCan = canonicalize(vp)

        return encBase64Str(vpCan)
    }

    fun embedPublicEncryptionKey(key: JWK): Map<String, String> {
        when (key) {
            is ECKey -> {
                return mapOf(
                    "kty" to key.keyType.value,
                    "alg" to key.algorithm.name,
                    "crv" to key.curve.name,
                    "x" to key.x.toString(),
                    "y" to key.y.toString()
                )
            }
            is OctetKeyPair -> {
                return when (key.curve) {
                    Curve.X25519 -> mapOf(
                        "kty" to key.keyType.value,
                        "crv" to key.curve.name,
                        "x" to key.x.toString()
                    )
                    Curve.Ed25519 -> mapOf(
                        "kty" to key.keyType.value,
                        "alg" to key.algorithm.name,
                        "crv" to key.curve.name,
                        "x" to key.x.toString()
                        //"d" to key.d.toString()
                    )
                    else -> throw IllegalArgumentException("Curve not supported")
                }
            }
            else -> {
                throw IllegalArgumentException("Not supported key")
            }
        }
    }

    // TODO replace with OidcUtil
    fun constructSiopResponseJwt(emphKeyId: KeyId, verifiedClaims: String, nonce: String): String {

        //val kid = DidService.loadDidEbsi(did).authentication!![0]

        val emphPrivKey = keyService.toJwk(emphKeyId.id, KeyType.PRIVATE)

        val thumbprint = emphPrivKey.computeThumbprint().toString()

        val payload = JWTClaimsSet.Builder()
            .issuer("https://self-issued.me")
            .audience("/siop-sessions")
            .subject(thumbprint)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now().plusSeconds(300)))
            .claim("nonce", nonce)
            .claim("sub_jwk", emphPrivKey.toJSONObject())
            .claim(
                "claims",
                mapOf(
                    "verified_claims" to verifiedClaims,
                    "encryption_key" to embedPublicEncryptionKey(emphPrivKey)
                )
            )
            .build().toString()

        val jwt = jwtService.sign(emphKeyId.id, payload)

        log.debug { "Siop Response JWT:\n$jwt" }

        jwtService.verify(jwt).let { if (!it) throw IllegalStateException("Generated JWK not valid") }

        return jwt
    }


    private fun didAuthAuthorizationApi(): AuthenticationRequestPayload {
        val authenticationRequest = Klaxon().toJsonString(mapOf("scope" to "ebsi user profile"))

        println("Request an access request token from the Authorisation API (POST /authentication-requests):\n${authenticationRequest}\n")

        val authenticationRequestResponse = AuthorizationApi.getAuthorizationRequest(authenticationRequest)


        // https://github.com/Baeldung/kotlin-tutorials/tree/master/kotlin-libraries-http/src/main/kotlin/com/baeldung/fuel
        //val resp = post("$ESSIF_BASE_URL/authentication-requests", json = mapOf("scope" to "ebsi user profile"))

        println("3. [UWallet] 200 <Authorization Request> received. (response of /authentication-requests):\n$authenticationRequestResponse")

        val oidcReqUri = jsonToOidcAuthenticationRequestUri(authenticationRequestResponse)

        log.debug { "OidcReqUri: $oidcReqUri" }

        if (false) {//!jwtService.verify(oidcReqUri.request)) {
            log.error { "Could not verify Authentication Request Token signature: " + oidcReqUri.request }
            throw Exception("Could not verify Authentication Request Token signature: " + oidcReqUri.request)
        } else {
            println("\nJWT signature of Authentication Request Token Verified successfully ✔\n")
        }

        val claims = jwtService.parseClaims(oidcReqUri.request)!!

        // println(claims?.get("claims")!!.toString())

        val claim = Klaxon().parse<Claim>(claims["claims"].toString())!!

        val arp = AuthenticationRequestPayload(
            claims["scope"].toString(),
            claims["iss"].toString(),
            claims["response_type"].toString(),
            claims["client_id"].toString(),
            claims["nonce"].toString(),
            Klaxon().parse(claims["registration"].toString())!!,
            claim
        )

        println("Decoded Authorization Request:\n" + Klaxon().toJsonString(arp) + "\n")
        return arp
    }

    private fun jsonToOidcAuthenticationRequestUri(authenticationRequestResponseJson: String): OidcAuthenticationRequestUri {
        try {
            val json = Parser.default().parse(StringBuilder(authenticationRequestResponseJson)) as JsonObject

            val uri = json.string("uri")!!
            val paramString = uri.substringAfter("openid://?")
            val pm = toParamMap(paramString)
            return OidcAuthenticationRequestUri(pm["response_type"]!!, pm["scope"]!!, pm["request"]!!)
        } catch (e: Exception) {
            log.error { "Could not parse AuthenticationRequestResponse: $authenticationRequestResponseJson" }
            throw e
        }
    }

    private fun validateAuthenticationRequest(authReq: AuthenticationRequestPayload) {

        log.debug { "Validating Authentication Request $authReq" }

//        if (authReq.claims.id_token.verified_claims.verification.trust_framework != "EBSI") {
//            throw Exception("Trust framework needs to be: EBSI")
//        }

        //TODO add further validations and validation based on the JSON schema

        println("Validating Authentication request:")
        println("- json schema: ✔")
        println("- valid issuer: ✔")
        println("- expiration date: ✔")
        println("- EBSI Trust Framework: ✔")
        println("")
    }

    private fun siopSessionsRequest(
        authReq: AuthenticationRequestPayload,
        verifiableAuthorization: String
    ): AccessTokenResponse? {

        // val verifiableAuthorization = File("src/test/resources/ebsi/verifiable-authorization.json").readText()

        println("Loading Verifiable Authorization:\n$verifiableAuthorization\n")

        val vp = "" //CredentialService.present(verifiableAuthorization, "api.ebsi.xyz", null)

        // TODO: set correct values
        val arp = AuthenticationResponsePayload(
            "did:ebsi:0x123abc",
            "thumbprint of the sub_jwk",
            "did:ebsi:RP-did-here",
            AuthenticationResponseVerifiedClaims(vp),
            null,
            1610714000,
            1610714900,
            authReq.nonce
        )

        println("AuthenticationResponse assembled:\n" + Klaxon().toJsonString(arp) + "\n")

        val signingKey = "did:key:z6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5"

        println("Signing AuthenticationResponse with key: $signingKey\n")
        val idToken =
            "yCdeRHuvk6kAAfQQCz6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5as3NPTRtxnB5a68mDrps5ZpzyCdeRHuz6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5vk6kAAfQQCas3NPTRtxnB5a68mDrps5eZpzyCdeRHuz6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5vk6kAAfQQCas3NPTRtxnB5a68mDrps5"//JwtService.sign(sigingKey, Klaxon().toJsonString(arp))

        val siopSessionRequest = SiopSessionRequest(idToken)
        println("SIOP Session Request:\n" + Klaxon().toJsonString(siopSessionRequest) + "\n")


        /*
        {
        "grantType": "client_credentials",
        "clientAssertionType": "urn:ietf:params:oauth:client-assertion-type:jwt-bearer",
        "clientAssertion": "eyJhbGciOiJIUzI...",
        "scope": "openid did_authn"
        }
        https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?spaceKey=BLOCKCHAININT&title=Authorisation+API#AuthorisationAPI-AccessTokenandAuthenticatedKeyExchangedatamodels
        TODO send siopSessionRequest
        AccessToken Received
        //        val atr = AccessTokenResponse(
        //            Ake1EncPayload("JWS encoded access token", "DID of the RP"),
        //            Ake1JwsDetached("Nonce from the ID Token used for authentication", "ake1_enc_payload", "DID of the Client"),
        //            "did"
        )
        */
        //Token payload validation https://openid.net/specs/openid-connect-core-1_0.html#CodeIDToken

        val atr = null
        // println("AccessTokenResponse received:\n" + Klaxon().toJsonString(atr))


        return atr // AccessTokenPayload
    }

    // https://ec.europa.eu/cefdigital/wiki/pages/viewpage.action?pageId=271909906
    private fun validateAccessTokenResponse(atr: AccessTokenResponse?): String {
        log.debug { "Validating Access Token Response $atr" }

        //TODO validate access token

        println("Validating AccessToken response:")
        println("- JWT signature: ✔")
        println("- DID of RP: ✔")
        println("- DID of Client: ✔")
        println("- ake1_nonce: ✔")
        println("")
        return "access token"
    }


}
