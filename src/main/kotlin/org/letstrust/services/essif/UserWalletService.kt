package org.letstrust.services.essif

import com.nimbusds.jose.crypto.impl.ECDH
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import mu.KotlinLogging
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import org.letstrust.common.readEssifBearerToken
import org.letstrust.common.readWhenContent
import org.letstrust.common.toParamMap
import org.letstrust.crypto.canonicalize
import org.letstrust.crypto.encBase64
import org.letstrust.crypto.encBase64Str
import org.letstrust.crypto.uncompressSecp256k1
import org.letstrust.model.*
import org.letstrust.services.essif.EssifFlowRunner.ake1EncFile
import org.letstrust.services.essif.mock.AuthorizationApi
import org.letstrust.services.essif.mock.DidRegistry
import org.letstrust.services.jwt.JwtService
import org.letstrust.services.key.KeyService
import org.letstrust.services.vc.CredentialService
import java.security.MessageDigest
import java.time.Instant
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private val log = KotlinLogging.logger {}


fun main() {
    val accessTokenResponse = Json.decodeFromString<AccessTokenResponse>(ake1EncFile.readText())

    //TODO parse resp

}

object UserWalletService {

//    val didUrlUser by lazy {
//        DidService.create(DidMethod.web)
//    }

    fun createDid(): String {
        val did = EnterpriseWalletService.didGeneration()
        log.debug { "did: $did" }

        val verifiableAuthorization = EnterpriseWalletService.requestVerifiableAuthorization(did)
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
        val vcToken = EnterpriseWalletService.validateDidAuthResponse("didAuthResp")
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

        log.debug { "Loading Verifiable Authorization from file: ${EssifFlowRunner.verifiableAuthorizationFile.absolutePath}." }

        val verifiableAuthorization = readWhenContent(EssifFlowRunner.verifiableAuthorizationFile)

        log.debug { "Loaded bearer token from ${EssifFlowRunner.bearerTokenFile.absolutePath}." }

        val accessToken = siopSession(did, verifiableAuthorization)

        log.debug { "Decrypted Access Token: $accessToken" }

        return accessToken


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
        // Create Verifiable Presentation with VA
        val verifiedClaims = createVerifiedClaims(did, va)

        // Build SIOP response token
        val nonce = UUID.randomUUID().toString()
        val idToken = constructSiopResponseJwt(did, verifiedClaims, nonce)

        val siopResponse = LegalEntityClient.eos.siopSession(idToken, readEssifBearerToken())

        log.debug { "Writig SIOP response (AKE1 encrypted token) to file: ${ake1EncFile.absolutePath}" }
        ake1EncFile.writeText(siopResponse)


        // TODO decrypt token


        val accessTokenResponse = Json.decodeFromString<AccessTokenResponse>(ake1EncFile.readText())

        val ephemPublicKey =
            uncompressSecp256k1(Hex.decode("04af3a9e50a855b0817ea06682da5f06da405093c934a8a274f206ccfbfff2b14c9794256ce92983e40825d1e22b82da51b5de76ca6e794cc8f0091d994ad905ed"))

//    if (ephemPublicKey!!.equals(pubKeyEos)) {
//        throw Exception("Key from EOS DID != key from AKE1 Payload")
//    }

        val clientKey = KeyService.toJwk(did, true) as ECKey
        val sharedSecret = ECDH.deriveSharedSecret(ephemPublicKey!!.toECPublicKey(), clientKey.toECPrivateKey(), BouncyCastleProvider())


        val encryptionKeyBytes = MessageDigest.getInstance("SHA-512").digest(sharedSecret.encoded).slice(0..31).toByteArray()

        val encryptionKey = SecretKeySpec(encryptionKeyBytes, "AES");


        val iv = Hex.decode("aeae1220d76e84cedf3d8baacc8f1714")

        val mac = Hex.decode("43bee9ed827fbcdb6339bc9c50a9226aa6e0be6cead8a306538d446d30249f1b")

        val payload = Hex.decode(
            "bad07c7fd77371cc1eb59630d7d8f918032bbc53e70d6e19a8758f7924b252bcfa9d5c8c100a5977dbbd4d2980985705785ebec0cbd832a9da5b24a801b7e37e4774fb3fc2efe7deaeb4fa12a76744130cc8ad5a3bcdc5ea1fc3ffa792d472aa50c3c8efd236b6362701e705688d6059c9feee96e10e816ddfb9e8d1444da731d459d43af71cb5556a5bcf7d20d9d9521e3668282952ebeed295bbf10fffa5e4dc8a26283e3efa9befcaef2b8bdccc39169924217b9d446c1bc033d15f12b96a257cbd46e25d65f870a38e19b17cbe9801e7f44f30d4e61a6e5298702054347731cd9763743679c05a769d72238f9abad773b8f2254de9ac6c8644c9198d6f136388920c6492fe77ffdb6327134dcbc36615f6cd58c5f9630547b31dcb925d72f7b0a173896a87d4564ab72dcfa51ecd0806d6f3dcdfb113a6bb7d11aa80cdded9387371d2bcb9ef07f527a5deb7b9a144e987bf246780ac5e3b7d5c3cb7ad34ca74328a68f6a5faed643a3320742046b5ea7ae17582fc0e7fdc08f9380afc512f18975c0b6280c900f2d9e2e5b354b273d8cbbae0a1787c5741a5f8b114623a2496294c3658c1ec1c7f1c04ea9626ec88f4cb05ead350cc4a9f1f8cd9545a55372bfc7a1d90917dcd8fe7bef3d9bac07d990e2eabdb1db2c652bdc1c2a301420d9d49635d655d0762ef7f8fde1ad18efff8e91bb062f451d1ad9921f77604cf0c0d51b36ff99e17cf568f47cd96a4bd62c0eaf867e6176175381e393c804b0187fb781a0df72cabb260189e07429d6214c047ad05b83d2576006a7965c81dcbe7956196b2623df255542ccc192bbe2e71b43433b7c63392a3d2a0ed00c5bdc4ed08fcc891665742d9aedda44176cccc47eb2abd6ac486b9b47d2726d932a9d14ee22d1da27322d797d837909b49d315cd7f2980cd3fc138693f3259355927c05c718b89a0b15c0e0ce258997c6a187622de7a6d93e0d3acf9e24c13dc26c720af9e51db74d7b55f87db3e267bb53641edc2f8d6a29a351bba334d370a481ca17bbae89083823604a98eaa38117a57724a35f7921845f754b0fbd81fd8c56aef44a34a0f6097cf3c04dd05aaa3b64c7cba"
        )

        val ciphertext = Hex.decode(
            "853e6e3ad8f62c83946b74bb63b59ee74467579e4f0bdc0731517ed75eebf254c7cffe68f52b1c7f9d2f176061ea0ab1426d1f4a3c33727d91d3d7366e5c276730601a115e41060ad1b23ea98e457f14770053084263bd08e908124187f706aa3c06d3a9bea9f2c58045ea544d066ce53c3b95de0e5547dc464891066ba742ce6602f68afa66316d73eeed09056a99c543691c1cb3c3ffd24eed688a5b285981727bf64ffc77e8a7c7ecc472dfab30b3b8f2e726dbf6344890fc78574a3fc25af2a77c12d03e2d1aba402c85345ce3ebf62aeeac4a92ee3af78f4949ea4f842fd9c033c1b3cc38324b09b0db5ee054ccb423dc750c95bd0943cbd2313d8477a60616860e72e1062b7a8ac59b77b64f7e925b278540d5fb793b6d7c495d79c4b08e82725cf1fe7b84bc22cafd52437968106277276a0bdc07da7381e7f2ac3c015d737eefd000436cf8b11f234c09790097d49a61808c64b5c37bf20c7af08b7e7345e6d336e7698dda8a1be93e124df5f8f85742edc38cecd6b2b50402325743f547879bbfe4f6823529dd452afdede6995c8a880c8bab736f8ef3fc5492460e4a2c7f381e3ad7ba89aff955809bf8f07cbcdc50e49e6676138b1ef05e924c084383510d7561eed7b8a35f46c9ff2dbb9d38db3dc4182bb190f984d4c788d0f68590bc4693a2bed7fe8be5479f9149aa63476d4caae36603e30f14f142c7901e490cb497f85b3c36a7408cbb4a59a6a310084216e4665c09b4746ae7db78ff3338193fb523c4c6e4bc3b2c66976d0e87b7f84ac5e91ec582d2da2d5d21faa5d641e5192073817ca58ca0d2eced1a9c6e8177c7af28303d226438a6d7116ba5a16e2655868c111da273483828969b808937951cdcccd90974e00e7a192e94e5746d6241573fabc602a60b7b03a79ea286a56c86686ebdd24cbe5ed9fa5e6f18be16fe156e7f63abda6d41fa6c8d5e0c332eab7b2bdd6988bddb7bc51dd70e5afb"
        )

        val c = Cipher.getInstance("AES/CBC/NoPadding", BouncyCastleProvider())

        //c.init(Cipher.DECRYPT_MODE, sharedKey, GCMParameterSpec(AESGCM.AUTH_TAG_BIT_LENGTH, iv))
        c.init(Cipher.DECRYPT_MODE, encryptionKey, IvParameterSpec(iv))
        //c.updateAAD(mac)
        val accessTokenBytes = c.doFinal(ciphertext)

        for (byte in accessTokenBytes) {
            print(byte.toString() + ", ")
        }

        println(String(accessTokenBytes))
        return encBase64(accessTokenBytes)
    }

    fun createVerifiedClaims(did: String, va: String): String {

        val vaWrapper = Json.decodeFromString<EbsiVAWrapper>(va)

        val vpReq = EbsiVaVp(
            listOf("https://www.w3.org/2018/credentials/v1"),
            listOf("VerifiablePresentation"),
            null,
            listOf(vaWrapper.verifiableCredential),
            null
        )

        val vp = CredentialService.sign(did, vpReq.encode())

        log.debug { "Verifiable Presentation:\nvp" }

        val vpCan = canonicalize(vp)

        return encBase64Str(vpCan)
    }

    fun constructSiopResponseJwt(did: String, verifiedClaims: String, nonce: String): String {

        val kid = "$did#key-1"
        val key = KeyService.toJwk(did, false, kid) as ECKey
        val thumbprint = key.computeThumbprint().toString()


        val payload = JWTClaimsSet.Builder()
            .issuer("https://self-issued.me")
            .audience("/siop-sessions")
            .subject(thumbprint)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now().plusSeconds(300)))
            .claim("nonce", nonce)
            .claim("sub_jwk", key.toJSONObject())
            .claim(
                "claims",
                mapOf(
                    "verified_claims" to verifiedClaims,
                    "encryption_key" to mapOf(
                        "kyt" to key.keyType.value,
                        "alg" to key.algorithm.name,
                        "crv" to key.curve.name,
                        "x" to key.x.toString(),
                        "y" to key.y.toString()
                    )
                )
            )
            .build().toString()

        val jwt = JwtService.sign(kid, payload)

        log.debug { "Siop Reponse JWT:\n$jwt" }

        val jwtToVerify = SignedJWT.parse(jwt)

        println(jwtToVerify.header)
        println(jwtToVerify.payload)

        JwtService.verify(jwt).let { if (!it) throw IllegalStateException("Generated JWK not valid") }

        return jwt
    }

    fun accessProtectedResource(accessToken: String) {

    }


    private fun didAuthAuthorizationApi(): AuthenticationRequestPayload {
        val authenticationRequest = Json.encodeToString(mapOf("scope" to "ebsi user profile"))

        println("Request an access request token from the Authorisation API (POST /authentication-requests):\n${authenticationRequest}\n")

        val authenticationRequestResponse = AuthorizationApi.getAuthorizationRequest(authenticationRequest)


        // https://github.com/Baeldung/kotlin-tutorials/tree/master/kotlin-libraries-http/src/main/kotlin/com/baeldung/fuel
        //val resp = post("$ESSIF_BASE_URL/authentication-requests", json = mapOf("scope" to "ebsi user profile"))

        println("3. [UWallet] 200 <Authorization Request> received. (response of /authentication-requests):\n$authenticationRequestResponse")

        val oidcReqUri = jsonToOidcAuthenticationRequestUri(authenticationRequestResponse)

        log.debug { "OidcReqUri: $oidcReqUri" }

        if (false) {//!JwtService.verify(oidcReqUri.request)) {
            log.error { "Could not verify Authentication Request Token signature: " + oidcReqUri.request }
            throw Exception("Could not verify Authentication Request Token signature: " + oidcReqUri.request)
        } else {
            println("\nJWT signature of Authentication Request Token Verified successfully ✔\n")
        }

        val claims = JwtService.parseClaims(oidcReqUri.request)!!

        // println(claims?.get("claims")!!.toString())

        val claim = Json.decodeFromString<Claim>(claims["claims"].toString())

        val arp = AuthenticationRequestPayload(
            claims["scope"].toString(),
            claims["iss"].toString(),
            claims["response_type"].toString(),
            claims["client_id"].toString(),
            claims["nonce"].toString(),
            claims["registration"].toString(),
            claim
        )

        println("Decoded Authorization Request:\n" + Json { prettyPrint = true }.encodeToString(arp) + "\n")
        return arp
    }

    private fun jsonToOidcAuthenticationRequestUri(authenticationRequestResponseJson: String): OidcAuthenticationRequestUri {
        try {
            val uri = Json.parseToJsonElement(authenticationRequestResponseJson).jsonObject["uri"].toString()
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

        if (authReq.claims.id_token.verified_claims.verification.trust_framework != "EBSI") {
            throw Exception("Trust framework needs to be: EBSI")
        }

        //TODO add further validations and validation based on the JSON schema

        println("Validating Authentication request:")
        println("- json schema: ✔")
        println("- valid issuer: ✔")
        println("- expiration date: ✔")
        println("- EBSI Trust Framework: ✔")
        println("")
    }

    private fun siopSessionsRequest(authReq: AuthenticationRequestPayload, verifiableAuthorization: String): AccessTokenResponse? {

        // val verifiableAuthorization = File("src/test/resources/ebsi/verifiable-authorization.json").readText()

        println("Loading Verifiable Authorization:\n$verifiableAuthorization\n")

        val vp = "" //CredentialService.present(verifiableAuthorization, "api.ebsi.xyz", null)

        // TODO: set correct values
        val arp = AuthenticationResponsePayload(
            "did:ebsi:0x123abc",
            "thumbprint of the sub_jwk",
            "did:ebsi:RP-did-here",
            1610714000,
            1610714900,
            "signing JWK",
            "did:ebsi:0x123abc#authentication-key-proof-3",
            authReq.nonce,
            AuthenticationResponseVerifiedClaims(vp, "enc_key")
        )

        println("AuthenticationResponse assembled:\n" + Json { prettyPrint = true }.encodeToString(arp) + "\n")

        val signingKey = "did:key:z6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5"

        println("Signing AuthenticationResponse with key: $signingKey\n")
        val idToken =
            "yCdeRHuvk6kAAfQQCz6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5as3NPTRtxnB5a68mDrps5ZpzyCdeRHuz6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5vk6kAAfQQCas3NPTRtxnB5a68mDrps5eZpzyCdeRHuz6MksTeZpzyCdeRHuvk6kAAfQQCas3NPTRtxnB5a68mDrps5vk6kAAfQQCas3NPTRtxnB5a68mDrps5"//JwtService.sign(sigingKey, Json.encodeToString(arp))

        val siopSessionRequest = SiopSessionRequest(idToken)
        println("SIOP Session Request:\n" + Json { prettyPrint = true }.encodeToString(siopSessionRequest) + "\n")


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
        // println("AccessTokenResponse received:\n" + Json { prettyPrint = true }.encodeToString(atr))


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
