package id.walt.services.keystore

import com.google.common.collect.ImmutableSet
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.JsonKeysetReader
import com.google.crypto.tink.JsonKeysetWriter
import com.google.crypto.tink.KeysetWriter
import com.google.crypto.tink.proto.*
import com.google.crypto.tink.subtle.EllipticCurves
import com.google.crypto.tink.subtle.EllipticCurves.CurveType
import com.google.protobuf.ExtensionRegistry
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.KeyOperation
import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import id.walt.crypto.KeyId
import id.walt.services.CryptoProvider
import id.walt.services.WaltIdServices
import java.io.File
import java.security.PublicKey
import java.security.spec.InvalidKeySpecException


open class TinkKeyStoreService : KeyStoreService() {


    override fun listKeys(): List<Key> = TODO("Not yet implemented")

//    override fun loadKeyPair(keyId: String): Keys? {
//        TODO("Not yet implemented")
//    }

    override fun delete(alias: String): Unit = TODO("Not yet implemented")


    override fun store(key: Key) {
        CleartextKeysetHandle.write(
            key.keysetHandle,
            JsonKeysetWriter.withFile(File("${WaltIdServices.keyDir}/${key.keyId.id}.tink"))
        )

        //TODO: only working for Secp256k1; should be impl. for Ed25519 as well
        // CleartextKeysetHandle.write(key.keysetHandle!!.publicKeysetHandle, JwksWriter.withOutputStream(FileOutputStream("${WaltIdServices.keyDir}/${key.keyId.id}.json")))
    }

    override fun load(alias: String, keyType: KeyType): Key {
        val keysetHandle =
            CleartextKeysetHandle.read(JsonKeysetReader.withFile(File("${WaltIdServices.keyDir}/${alias}.tink")))
        val algorithm = when (keysetHandle.keysetInfo.getKeyInfo(0).typeUrl) {
            "type.googleapis.com/google.crypto.tink.Ed25519PrivateKey" -> KeyAlgorithm.EdDSA_Ed25519
            "type.googleapis.com/google.crypto.tink.EcdsaPrivateKey" -> KeyAlgorithm.ECDSA_Secp256k1
            "type.googleapis.com/google.crypto.tink.EcdsaPrivateKey" -> KeyAlgorithm.ECDSA_Secp256r1
            else -> throw IllegalArgumentException("Could not determine KeyAlgorithm")
        }

        return Key(KeyId(alias), algorithm, CryptoProvider.TINK, keysetHandle)
    }

    // TODO load public https://github.com/google/tink/issues/300
    fun loadPublicKey(key: Key): PublicKey {
        val pubKeyLoader = PubKeyLoader()
        CleartextKeysetHandle.write(key.keysetHandle!!.publicKeysetHandle, pubKeyLoader)
        return pubKeyLoader.publicKey!!
    }

    class PubKeyLoader : KeysetWriter {
        private val ECDSA_PUBLIC_KEY_URL = "type.googleapis.com/google.crypto.tink.EcdsaPublicKey"
        private val ES256_ECDSA_PARAMS = EcdsaParams.newBuilder()
            .setHashType(HashType.SHA256)
            .setCurve(EllipticCurveType.NIST_P256)
            .setEncoding(EcdsaSignatureEncoding.IEEE_P1363)
            .build()

        var publicKey: PublicKey? = null
        var publicKeyJWK: ECKey? = null

        override fun write(keyset: Keyset?) {
            for (key in keyset!!.keyList) {
                if (key.status != KeyStatusType.ENABLED) {
                    continue
                }
                publicKeyJWK = createJwk(key)!!
                publicKey = publicKeyJWK!!.toPublicKey()
            }
        }

        private fun createJwk(key: Keyset.Key): ECKey? {
            if (key.outputPrefixType != OutputPrefixType.RAW) {
                throw InvalidKeySpecException(
                    String.format(
                        "Unsupported output_prefix_type for key_id %d: %s (want RAW)",
                        key.keyId, key.outputPrefixType
                    )
                )
            }
            return when (key.keyData.typeUrl) {
                ECDSA_PUBLIC_KEY_URL -> createEcdsaJwk(key)
                else -> throw InvalidKeySpecException(
                    String.format(
                        "Unsupported type_url for key_id %d: %s",
                        key.keyId, key.keyData.typeUrl
                    )
                )
            }
        }

        private fun createEcdsaJwk(key: Keyset.Key): ECKey? {

            // Parse & validate EcdsaPublicKey.
            val ecdsaPublicKey = EcdsaPublicKey.parseFrom(
                key.keyData.value, ExtensionRegistry.getEmptyRegistry()
            )
            if (ES256_ECDSA_PARAMS != ecdsaPublicKey.params) {
                throw InvalidKeySpecException(
                    String.format(
                        "Unsupported ECDSA params for key_id %d: %s (want %s)",
                        key.keyId, ecdsaPublicKey.params, ES256_ECDSA_PARAMS
                    )
                )
            }

            // Convert to stdlib ECPublicKey, then to NimbusDS ECKey.
            val publicKey = EllipticCurves.getEcPublicKey(
                CurveType.NIST_P256,
                ecdsaPublicKey.x.toByteArray(),
                ecdsaPublicKey.y.toByteArray()
            )
            return ECKey.Builder(Curve.P_256, publicKey)
                .algorithm(JWSAlgorithm.ES256)
                .keyID(key.keyId.toString())
                .keyOperations(ImmutableSet.of(KeyOperation.VERIFY))
                .build()

            // LD Signer would require this key:
//            return ECKey.Builder(Curve.SECP256K1, publicKey)
//                .algorithm(JWSAlgorithm.ES256K)
//                .keyID(key.keyId.toString())
//                .keyOperations(ImmutableSet.of(KeyOperation.VERIFY))
//                .build()
        }

        override fun write(keyset: EncryptedKeyset?): Unit = TODO("Not yet implemented")

    }


    override fun addAlias(keyId: KeyId, alias: String) {
        //TODO remove dependency to FileSystemKeyStoreService
        FileSystemKeyStoreService().addAlias(keyId, alias)
    }

    override fun getKeyId(alias: String): String? {
        //TODO remove dependency to FileSystemKeyStoreService
        return FileSystemKeyStoreService().getKeyId(alias)
    }

}
