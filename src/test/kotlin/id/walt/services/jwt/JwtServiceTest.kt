package id.walt.services.jwt

import com.beust.klaxon.Klaxon
import com.nimbusds.jose.crypto.impl.AESGCM
import com.nimbusds.jose.crypto.impl.ECDH
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import id.walt.servicematrix.ServiceMatrix
import id.walt.vclib.vclist.Europass
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.util.encoders.Hex
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.decBase64
import id.walt.model.DidEbsi
import id.walt.model.DidMethod
import id.walt.services.crypto.CryptoService
import id.walt.services.did.DidService
import id.walt.services.key.KeyService
import id.walt.services.keystore.KeyType
import id.walt.vclib.Helpers.toCredential
import java.io.File
import java.time.Instant
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

class JwtServiceTest : AnnotationSpec() {

    @Before
    fun setup() {
        ServiceMatrix("service-matrix.properties")
    }

    private val cryptoService = CryptoService.getService()
    private val keyService = KeyService.getService()
    private val jwtService = JwtService.getService()

    @Test
    fun parseClaimsTest() {

        val token = "..--\n"

        //val claims = jwtService.parseClaims(token)!! // TODO WALT0508

        // claims?.iterator()?.forEach { println(it) }

        //"054255a8-b82f-4ded-bd48-95f90f46c53e" shouldBe claims["sub"].toString()
        //"https://api.walt.id" shouldBe claims["iss"].toString() // TODO WALT050

        // TODO be time specific too
        /*println("${claims["exp"].toString()} should be Mon Mar 08 14:08:36 CET 2021")
        "Mon Mar 08 14:08:36 CET 2021" shouldBe claims["exp"].toString()
        println("${claims["iat"].toString()} should be Mon Mar 08 13:18:36 CET 2021")
        "Mon Mar 08 13:18:36 CET 2021" shouldBe claims["iat"].toString()*/

        // TODO quick hack to get around locale/timezone specifics

        // Mon Mar[ 08 ]14[:08:36 ]CET[ 2021]
        /*val exp = claims["exp"].toString()
        exp shouldContain " 08 "
        exp shouldContain ":08:36 "
        exp shouldContain " 2021"*/ // TODO WALT0508

        // Mon Mar[ 08 ]13[:18:36 ]CET[ 2021]
        /*val iat = claims["iat"].toString()
        exp shouldContain " 08 "
        iat shouldContain ":18:36 "
        iat shouldContain " 2021"*/ // TODO WALT0508
    }

    @Test
    fun genJwtSecp256k1() {
        val keyId = cryptoService.generateKey(KeyAlgorithm.ECDSA_Secp256k1)

        val jwt = jwtService.sign(keyId.id)

        val signedJwt = SignedJWT.parse(jwt)
        "ES256K" shouldBe signedJwt.header.algorithm.name
        keyId.id shouldBe signedJwt.header.keyID
        "https://walt.id" shouldBe signedJwt.jwtClaimsSet.claims["iss"]

        val res1 = jwtService.verify(jwt)
        res1 shouldBe true
    }

    @Test
    fun genJwtEd25519() {
        val keyId = cryptoService.generateKey(KeyAlgorithm.EdDSA_Ed25519)

        val jwt = jwtService.sign(keyId.id)

        val signedJwt = SignedJWT.parse(jwt)
        "EdDSA" shouldBe signedJwt.header.algorithm.name
        keyId.id shouldBe signedJwt.header.keyID
        "https://walt.id" shouldBe signedJwt.jwtClaimsSet.claims["iss"]

        val res1 = jwtService.verify(jwt)
        res1 shouldBe true
    }


    @Test
    fun genJwtCustomPayload() {
        val did = DidService.create(DidMethod.ebsi, keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id)
        val kid = DidService.loadDidEbsi(did).verificationMethod!![0].id
        val key = keyService.toJwk(did, jwkKeyId = kid) as ECKey
        val thumbprint = key.computeThumbprint().toString()

        val payload = JWTClaimsSet.Builder()
            .issuer("https://self-issued.me")
            .audience("redirectUri")
            .subject(thumbprint)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(Instant.now().plusSeconds(120)))
            .claim("nonce", "nonce")
            .claim("sub_jwk", key.toJSONObject())
            .build().toString()

        val jwtStr = jwtService.sign(kid, payload)
        val jwt = SignedJWT.parse(jwtStr)
        "ES256K" shouldBe jwt.header.algorithm.name
        kid shouldBe jwt.header.keyID
        "https://self-issued.me" shouldBe jwt.jwtClaimsSet.claims["iss"]
        thumbprint shouldBe jwt.jwtClaimsSet.claims["sub"]

        jwtService.verify(jwtStr) shouldBe true
    }


//    id	jti	direct mapping
//    issuer	iss	direct mapping
//    issuanceDate	iat, nbf	transformation from RFC 3339 to UNIX timestamp
//    expirationDate	exp	transformation from RFC 3339 to UNIX timestamp
//    credentialSubject.id	sub	direct mapping
//    VA JSON Strcucture	vc

    @Test
    fun genEbsiVerifiableAttestationJwtProof() {
        val issuerDid = DidService.create(DidMethod.ebsi, keyService.generate(KeyAlgorithm.ECDSA_Secp256k1).id)

        val input = File("src/test/resources/verifiable-credentials/VerifiableAttestation-Europass.json").readText()
        val verifiableAttestationJsonLd = input.toCredential() as Europass
        verifiableAttestationJsonLd.issuer = issuerDid

        val jti = verifiableAttestationJsonLd.id
        val iss = verifiableAttestationJsonLd.issuer
        val nbf =
            Date.from(Instant.now()) //TODO transformation verifiableAttestationJsonLd.issuanceDate from RFC 3339 to UNIX timestamp
        val iat = nbf
        val exp = Date.from(
            Instant.now().plusSeconds(120)
        ) //TODO transformation verifiableAttestationJsonLd.expirationDate from RFC 3339 to UNIX timestamp
        val sub = verifiableAttestationJsonLd.credentialSubject!!.id

        val payload = JWTClaimsSet.Builder()
            .jwtID(jti)
            .issuer(iss)
            .subject(sub)
            .issueTime(iat)
            .expirationTime(exp)
            .notBeforeTime(nbf)
            .claim("vc", verifiableAttestationJsonLd) // TODO reduce payload of VA
            .build().toString()

        println(payload)

        val jwtStr = jwtService.sign(issuerDid, payload)
        val jwt = SignedJWT.parse(jwtStr)

        println(jwt.serialize())

        "ES256K" shouldBe jwt.header.algorithm.name
        issuerDid shouldBe jwt.header.keyID
        issuerDid shouldBe jwt.jwtClaimsSet.claims["iss"]
        sub shouldBe jwt.jwtClaimsSet.claims["sub"]

        jwtService.verify(jwtStr) shouldBe true

    }

    //TODO consider the following @Test
//    fun signAuthenticationRequest() {
//        val payload = File("src/test/resources/ebsi/authentication-request-payload-dummy.json").readText()
//
//        val arp = Klaxon().parse<AuthenticationRequestPayload>(payload)
//
//        val keyId = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
//
//        val jwt = jwtService.sign(keyId.id, Klaxon().toJsonString(arp))
//
//        println(jwt)
//
//        val claims = jwtService.parseClaims(jwt)
//
//        "openid did_authn" shouldBe claims?.get("scope")!!.toString()
//
//        val res = jwtService.verify(jwt)
//
//        (res, "JWT verification failed") shouldBe true
//    }

    // This test-case depends on the associated subject-key in verifiable-authorization2.json, which needs to be available in the keystore
    //@Test
//    fun signAuthenticationResponseTest() {
//        val verifiableAuthorization = File("src/test/resources/ebsi/verifiable-authorization2.json").readText()
//
//        val vp = CredentialService.present(verifiableAuthorization, "api.ebsi.xyz", null)
//
//        val arp = AuthenticationResponsePayload(
//            "did:ebsi:0x123abc",
//            "thumbprint of the sub_jwk",
//            "did:ebsi:RP-did-here",
//            1610714000,
//            1610714900,
//            "signing JWK",
//            "did:ebsi:0x123abc#authentication-key-proof-3",
//            "n-0S6_WzA2M",
//            AuthenticationResponseVerifiedClaims(vp, "enc_key")
//        )
//
//        println(Klaxon().toJsonString(arp))
//
//        val keyId = KeyService.generate(KeyAlgorithm.ECDSA_Secp256k1)
//
//        val jwt = jwtService.sign(keyId.id, Klaxon().toJsonString(arp))
//
//        println(jwt)
//
//        val resJwt = jwtService.verify(jwt)
//
//        (resJwt, "JWT verification failed") shouldBe true
//
//        val claims = jwtService.parseClaims(jwt)!!
//
//        val childClaims = claims["claims"] as JSONObject
//
//        vp shouldBe childClaims["verified_claims"]
//
//        val resVP = CredentialService.verifyVp(vp)
//
//        (resVP, "LD-Proof verification failed") shouldBe true
//
//    }

    // https://ec.europa.eu/cefdigital/wiki/display/BLOCKCHAININT/Authorisation+API
//    @Test
//    fun authenticatedKeyExchangeTest() {
//        val accessToken =
//            "eyJraWQiOiJMZXRzVHJ1c3QtS2V5LTFhZjNlMWIzNGIxNjQ3NzViNGQwY2IwMDJkODRlZmQwIiwidHlwIjoiSldUIiwiYWxnIjoiRVM1MTIifQ.eyJzdWIiOiIwNTQyNTVhOC1iODJmLTRkZWQtYmQ0OC05NWY5MGY0NmM1M2UiLCJpc3MiOiJodHRwczovL2FwaS5sZXRzdHJ1c3QuaW8iLCJleHAiOjE2MTUyMDg5MTYsImlhdCI6MTYxNTIwNTkxNn0.ARUKAO0f6vpRyUXWWEeL4xPegzl66eaC-AeEXswhsrs1OREae81JPNqnWs8e3rTrRCLCfRTcVS658hV8jfjAAY6vASwtNjV9HwJcmUGmpanBjAuQkJLkmv6Sn3lqzF5PU3hFv3GnVznvcDDyLRlsI8OooPZmM6p-FWUR8tAYKpvzAdMB"
//
//        val did_of_rp = DidService.create(DidMethod.key) // Creates a Ed25519 key, as well as an derived X25519 key
//        val did_of_client = DidService.create(DidMethod.key)
//
//        // ake1_enc_payload(Access Token, DID(Q)) and encrypts it: c = Enc(Access Token, DID(Q)) -> https://connect2id.com/products/nimbus-jose-jwt/algorithm-selection-guide#encryption
//        val ake1_enc_payload = Klaxon().toJsonString(Ake1EncPayload(accessToken, did_of_rp))
//        val ake1_enc_payload_ENC = jwtService.encrypt(did_of_rp, ake1_enc_payload)
//
//        // ake1_sig_payload(nonce, ake1_enc_payload, did(P))
//        val ake1_nonce = UUID.randomUUID().toString()
//        val ake1_jws_detached = Klaxon().toJsonString(Ake1JwsDetached(ake1_nonce, ake1_enc_payload_ENC, did_of_client))
//        val ake1_jws_detached_SIG = jwtService.sign(did_of_rp, ake1_jws_detached)
//
//        // AKE response (ake1_jws_detached, ake1_enc_payload, did(Q))
//        val access_token_response = Klaxon().toJsonString(AccessTokenResponse(ake1_enc_payload_ENC, ake1_jws_detached_SIG, did_of_rp))
//
//        println("access_token_response:\n" + access_token_response)
//        println("ake1_enc_payload:\n" + ake1_enc_payload)
//        println("ake1_jws_detached:\n" + ake1_jws_detached)
//
//
//        // Received AccessTokenResponse
//        val received_access_token_response = Klaxon().parse<AccessTokenResponse>(access_token_response)
//        // Verifies the signature ake1_sig_payload
//        val verified = jwtService.verify(received_access_token_response.ake1_jws_detached)
//        verified shouldBe true
//
//        // encrypted payload ake1_enc_payload
//        val received_ake1_enc_payload = jwtService.decrypt(received_access_token_response.ake1_enc_payload)
//        val received_ake1_enc_payload_obj = Klaxon().parse<Ake1EncPayload>(received_ake1_enc_payload)
//        val received_access_token = received_ake1_enc_payload_obj.access_token
//
//        accessToken shouldBe received_access_token
//        // Creates an ake1_sig_payload(nonce, ake1_enc_payload, did(P))
//
//    }


    // com.nimbusds.jose.crypto.ECDHDecrypter.decrypt


//    export function decompress(startsWith02Or03) {
//
//        // if already decompressed an not has trailing 04
//        const testBuffer = Buffer.from(startsWith02Or03, 'hex');
//        if (testBuffer.length === 64) startsWith02Or03 = '04' + startsWith02Or03;
//
//        let decompressed = uint8ArrayToHex(publicKeyConvert(
//            hexToUnit8Array(startsWith02Or03),
//            false
//        ));
//
//        // remove trailing 04
//        decompressed = decompressed.substring(2);
//        return decompressed;
//    }


//    export function parse(str) {
//        if (typeof str !== 'string')
//        return str;
//
//        const buf = Buffer.from(str, 'hex');
//
//        const ret = {
//            iv: buf.toString('hex', 0, 16),
//            ephemPublicKey: buf.toString('hex', 16, 49),
//            mac: buf.toString('hex', 49, 81),
//            ciphertext: buf.toString('hex', 81, buf.length)
//        };
//
//        // decompress publicKey
//        ret.ephemPublicKey = '04' + decompress(ret.ephemPublicKey);
//
//        return ret;
//    }

//    export default function decryptWithPrivateKey(privateKey, encrypted) {
//
//        encrypted = parse(encrypted);
//
//        // remove trailing '0x' from privateKey
//        const twoStripped = removeLeading0x(privateKey);
//
//        const encryptedBuffer = {
//            iv: Buffer.from(encrypted.iv, 'hex'),
//            ephemPublicKey: Buffer.from(encrypted.ephemPublicKey, 'hex'),
//            ciphertext: Buffer.from(encrypted.ciphertext, 'hex'),
//            mac: Buffer.from(encrypted.mac, 'hex')
//        };
//
//
//        return decrypt(
//            Buffer.from(twoStripped, 'hex'),
//            encryptedBuffer
//        ).then(decryptedBuffer => decryptedBuffer.toString());
//    }

//    exports.decrypt = function(privateKey, opts) {
//        // Tmp variable to save context from flat promises;
//        var encryptionKey;
//        return derive(privateKey, opts.ephemPublicKey).then(function(Px) {
//            return sha512(Px);
//        }).then(function(hash) {
//            encryptionKey = hash.slice(0, 32);
//            var macKey = hash.slice(32);
//            var dataToMac = Buffer.concat([
//                opts.iv,
//                opts.ephemPublicKey,
//                opts.ciphertext
//            ]);
//            return hmacSha256Verify(macKey, dataToMac, opts.mac);
//        }).then(function(macGood) {
//            assert(macGood, "Bad MAC");
//            return aesCbcDecrypt(opts.iv, encryptionKey, opts.ciphertext);
//        }).then(function(msg) {
//            return Buffer.from(new Uint8Array(msg));
//        });
//    };

// /home/pp/dev/ebsi/ebsi-did-authentication-lib/node_modules/@cef-ebsi/oauth2-auth/src/utils.ts
//    export async function decrypt(
//    privateKey: string,
//    encrypted: string
//    ): Promise<string> {
//        // using eth-crypto lib for decryption
//        const encryptedObject = EthCrypto.cipher.parse(encrypted);
//        const decrypted = await EthCrypto.decryptWithPrivateKey(
//            privateKey,
//            encryptedObject
//        );
//        return decrypted;
//    }


// * Validate an SIOP Authentication response using the AKE protocol including the Access Token in the response and returns the decrypted Access Token.
// */
//    async verifyAuthenticationResponse(response, nonce) {
//        const { ake1_enc_payload: ake1EncPayload } = response;
//        const ake1DecPayload = JSON.parse(await Util_1.decrypt(__classPrivateFieldGet(this, _privateKey), ake1EncPayload));
//        if (typeof ake1DecPayload.did !== "string" ||
//        typeof ake1DecPayload.access_token !== "string")
//        throw new Error("did or access_token invalid type");
//        if (nonce !== (ake1DecPayload === null || ake1DecPayload === void 0 ? void 0 : ake1DecPayload.nonce))
//        throw new Error(`Expected nonce ${nonce}. Received ${ake1DecPayload === null || ake1DecPayload === void 0 ? void 0 : ake1DecPayload.nonce}`);
//        await did_jwt_1.verifyEbsiJWT(ake1DecPayload === null || ake1DecPayload === void 0 ? void 0 : ake1DecPayload.access_token, {
//            didRegistry: this.didRegistry,
//            audience: "ebsi-core-services",
//        });
//        return ake1DecPayload.access_token;
//    }

    fun decrypt(
        privateKey: ECKey,
        publicKey: ECKey,
        payload: String
    ): String {
        // val jweObj = JWEObject.parse(jwe)

        // val keyId = jweObj.header.keyID

        val sharedKey =
            ECDH.deriveSharedSecret(publicKey.toECPublicKey(), privateKey.toECPrivateKey(), BouncyCastleProvider())

        println(sharedKey.algorithm)

        println(sharedKey.format)

        val iv = Hex.decode("bad07c7fd77371cc1eb59630d7d8f918")

        val mac = Hex.decode("5ebec0cbd832a9da5b24a801b7e37e4774fb3fc2efe7deaeb4fa12a76744130c")

        val c = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProvider())
        //val c = Cipher.getInstance("AES/CBC/PKCS7Padding", BouncyCastleProvider())

        c.init(Cipher.DECRYPT_MODE, sharedKey, GCMParameterSpec(AESGCM.AUTH_TAG_BIT_LENGTH, iv))
        // c.init(Cipher.DECRYPT_MODE, sharedKey, IvParameterSpec(iv))
        c.updateAAD(mac)
        return String(c.doFinal(decBase64(payload)))

    }

    // @Test
    fun ake1DecryptPayload() {
        val ebsiDid = Klaxon().parse<DidEbsi>(File("src/test/resources/ebsi/ake1-did.json").readText())!!
        val jwkStr = Klaxon().toJsonString(ebsiDid.verificationMethod!![0].publicKeyJwk)
        println(jwkStr)
        val publicKey = JWK.parse(jwkStr) as ECKey
        print(publicKey)

        val key = keyService.load("did:ebsi:2LEi74mCZpgC8EqcngLCzUCL5d8W3dxfdjiy9XhaVoDyi259", KeyType.PRIVATE)
        val privateKey = keyService.toSecp256Jwk(key)

        val payload =
            "bad07c7fd77371cc1eb59630d7d8f918032bbc53e70d6e19a8758f7924b252bcfa9d5c8c100a5977dbbd4d2980985705785ebec0cbd832a9da5b24a801b7e37e4774fb3fc2efe7deaeb4fa12a76744130cc8ad5a3bcdc5ea1fc3ffa792d472aa50c3c8efd236b6362701e705688d6059c9feee96e10e816ddfb9e8d1444da731d459d43af71cb5556a5bcf7d20d9d9521e3668282952ebeed295bbf10fffa5e4dc8a26283e3efa9befcaef2b8bdccc39169924217b9d446c1bc033d15f12b96a257cbd46e25d65f870a38e19b17cbe9801e7f44f30d4e61a6e5298702054347731cd9763743679c05a769d72238f9abad773b8f2254de9ac6c8644c9198d6f136388920c6492fe77ffdb6327134dcbc36615f6cd58c5f9630547b31dcb925d72f7b0a173896a87d4564ab72dcfa51ecd0806d6f3dcdfb113a6bb7d11aa80cdded9387371d2bcb9ef07f527a5deb7b9a144e987bf246780ac5e3b7d5c3cb7ad34ca74328a68f6a5faed643a3320742046b5ea7ae17582fc0e7fdc08f9380afc512f18975c0b6280c900f2d9e2e5b354b273d8cbbae0a1787c5741a5f8b114623a2496294c3658c1ec1c7f1c04ea9626ec88f4cb05ead350cc4a9f1f8cd9545a55372bfc7a1d90917dcd8fe7bef3d9bac07d990e2eabdb1db2c652bdc1c2a301420d9d49635d655d0762ef7f8fde1ad18efff8e91bb062f451d1ad9921f77604cf0c0d51b36ff99e17cf568f47cd96a4bd62c0eaf867e6176175381e393c804b0187fb781a0df72cabb260189e07429d6214c047ad05b83d2576006a7965c81dcbe7956196b2623df255542ccc192bbe2e71b43433b7c63392a3d2a0ed00c5bdc4ed08fcc891665742d9aedda44176cccc47eb2abd6ac486b9b47d2726d932a9d14ee22d1da27322d797d837909b49d315cd7f2980cd3fc138693f3259355927c05c718b89a0b15c0e0ce258997c6a187622de7a6d93e0d3acf9e24c13dc26c720af9e51db74d7b55f87db3e267bb53641edc2f8d6a29a351bba334d370a481ca17bbae89083823604a98eaa38117a57724a35f7921845f754b0fbd81fd8c56aef44a34a0f6097cf3c04dd05aaa3b64c7cba"

        val token = decrypt(privateKey, publicKey, payload)

        println(token)
    }

}
