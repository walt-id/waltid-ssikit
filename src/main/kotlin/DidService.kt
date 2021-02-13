import io.ipfs.multibase.Multibase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.DidKey
import model.Key
import java.util.*


object DidService {

    var kms = KeyManagementService

    //
    fun registerDid(): String {
        val keyId = kms.generateKeyPair("Ed25519")
        // should be fingerprintFromPublicKey
        var identifier = "did:key:z" + kms.getMultiBase58PublicKey(keyId)

        kms.addAlias(keyId, identifier)

        // TODO compute json did-key body
        var didBody = "{}"

        // TODO register did -> store to disk

        print("DID: " + identifier)

        return identifier
    }

    fun registerDid(keyId: String): String {

        // TODO register DID
        var identifier = "did:dummy:" + UUID.randomUUID().toString().replace("-", "")

        // TODO compute json did-key body
        var didBody = "{}"
        kms.addAlias(keyId, identifier)

        return identifier
    }


//    async keyToDidDoc(edKey) {
//        const did = `did:key:${edKey.fingerprint()}`;
//        const keyId = `${did}#${edKey.fingerprint()}`;
//        edKey.controller = did;
//
//        const dhKey = await X25519KeyPair.fromEdKeyPair(edKey);
//        dhKey.id = `${did}#${dhKey.fingerprint()}`;
//
//        const didDoc = {
//            '@context': ['https://w3id.org/did/v0.11'],
//            id: did,
//            publicKey: [{
//                id: keyId,
//                type: edKey.type,
//                controller: did,
//                publicKeyBase58: edKey.publicKeyBase58
//        }],
//            authentication: [keyId],
//            assertionMethod: [keyId],
//            capabilityDelegation: [keyId],
//            capabilityInvocation: [keyId],
//            keyAgreement: [{
//                id: dhKey.id,
//                type: dhKey.type,
//                controller: did,
//                publicKeyBase58: dhKey.publicKeyBase58
//        }]
//        };
//        Object.defineProperty(didDoc, 'keys', {
//            value: {
//            [keyId]: edKey,
//            [dhKey.id]: dhKey
//        },
//            enumerable: false
//        });


    // https://w3c-ccg.github.io/did-method-key/
    // https://github.com/digitalbazaar/did-method-key-js#example-did-document
    fun resolveDidKey(identifier: String): String? {
        val fingerprint = identifier.substringAfter("did:key:")
        // fetch b58 key out of fingerprintFromPublicKey

         // FIX, not working yet
        var pubKeyBase58 = Multibase.encode(Multibase.Base.Base64, fingerprint.toByteArray())
        pubKeyBase58 = "B12NYF8RrR3h41TDCTJojY59usg3mbtbjnFs7Eud1Y6u"
        var keyAgreement58 = "JhNWeSVLMYccCk7iopQW4guaSJTojqpMEELgSLhKwRr"

        val publicKeys = listOf(Key("did:key:${fingerprint}#${fingerprint}", "Ed25519VerificationKey2018", identifier, pubKeyBase58))
        val keyAgreement = listOf(Key("did:key:${fingerprint}#zBzoR5sqFgi6q3iFia8JPNfENCpi7RNSTKF7XNXX96SBY4", "X25519KeyAgreementKey2019", identifier, keyAgreement58))
        val keyId = "did:key:${fingerprint}#${fingerprint}"
        val didKey = DidKey("https://w3id.org/did/v1", identifier, publicKeys, listOf(keyId), listOf(keyId), listOf(keyId), listOf(keyId), keyAgreement)

//        val map = mapOf(
//            "@context" to "https://w3id.org/did/v1",
//            "id" to identifier,
//            "publicKey" to listOf(
//                mapOf(
//                    "id" to "did:key:${fingerprint}#${fingerprint}",
//                    "type" to "Ed25519VerificationKey2018",
//                    "controller" to identifier,
//                    "publicKeyBase58" to "B12NYF8RrR3h41TDCTJojY59usg3mbtbjnFs7Eud1Y6u" // fetch b58 key out of fingerprintFromPublicKey
//                )
//            ),
//            "authentication" to listOf("did:key:${fingerprint}#${fingerprint}"),
//            "assertionMethod" to listOf("did:key:${fingerprint}#${fingerprint}"),
//            "capabilityDelegation" to listOf("did:key:${fingerprint}#${fingerprint}"),
//            "capabilityInvocation" to listOf("did:key:${fingerprint}#${fingerprint}"),
//            "keyAgreement" to listOf(
//                mapOf(
//                    // TODO keyAgreement key -> controller + https://github.com/digitalbazaar/x25519-key-agreement-key-2019/blob/2bf26987623c16776085de137b7daef3e3e73b15/lib/X25519KeyPair.js#L235
//                    "id" to "did:key:${fingerprint}#zBzoR5sqFgi6q3iFia8JPNfENCpi7RNSTKF7XNXX96SBY4",
//                    "type" to "X25519KeyAgreementKey2019",
//                    "controller" to identifier,
//                    "publicKeyBase58" to "JhNWeSVLMYccCk7iopQW4guaSJTojqpMEELgSLhKwRr"
//                )
//            ),
//        )
//        val mapper = jacksonObjectMapper()
//        val didSerialized = mapper.writeValueAsString(map)
//        println(didSerialized)

        return Json.encodeToString(didKey)
    }

    fun createDidKey() {
        val keyId = kms.generateKeyPair("Ed25519")
        var identifier = "did:key:" + kms.getMultiBase58PublicKey(keyId)
        kms.addAlias(keyId, identifier)
    }

    fun createDidWeb(): Map<String, Any> {
//        async createIdentifier(
//                { kms, alias }: { kms?: string; alias?: string },
//        context: IContext,
//        ): Promise<Omit<IIdentifier, 'provider'>> {
//            const key = await context.agent.keyManagerCreate({ kms: kms || this.defaultKms, type: 'Secp256k1' })
//
//            const identifier: Omit<IIdentifier, 'provider'> = {
//            did: 'did:web:' + alias,
//            controllerKeyId: key.kid,
//            keys: [key],
//            services: [],
//        }
//            debug('Created', identifier.did)
//            return identifier
//        }

        return mapOf(
            "active" to true,
            "scope" to "read write email",
            "client_id" to "SlAV32hkKG",
            "username" to "phil",
            "exp" to 1437275311,
        )
    }
}
