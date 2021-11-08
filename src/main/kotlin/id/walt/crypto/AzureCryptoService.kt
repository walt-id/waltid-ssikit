package id.walt.crypto

import com.microsoft.azure.keyvault.KeyVaultClient
import com.microsoft.azure.keyvault.webkey.JsonWebKey
import com.microsoft.azure.keyvault.webkey.JsonWebKeyCurveName
import com.microsoft.azure.keyvault.webkey.JsonWebKeySignatureAlgorithm
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.impl.ECDSA
import id.walt.servicematrix.ServiceConfiguration
import id.walt.services.crypto.CryptoService
import id.walt.services.keystore.azure.TokenKeyVaultCredentials
import org.web3j.crypto.ECDSASignature
import org.web3j.crypto.Hash
import java.math.BigInteger

data class AzureKeyVaultConfig(val baseURL: String, val id: String, val secret: String): ServiceConfiguration

open class AzureCryptoService(configurationPath: String) : CryptoService() {

    final override val configuration: AzureKeyVaultConfig = fromConfiguration(configurationPath)
    private val client = KeyVaultClient(TokenKeyVaultCredentials(configuration.id, configuration.secret))

    override fun generateKey(algorithm: KeyAlgorithm): KeyId {
        TODO("Not yet implemented")
    }

    override fun sign(keyId: KeyId, data: ByteArray): ByteArray =
        signWithClient(keyId, Hash.sha256(data)).let { ECDSA.transcodeSignatureToDER(it) }

    override fun verify(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean =
        getKey(keyId.id).let {
            client.verify(
                it.kid(),
                getAlgorithm(it.crv()),
                Hash.sha256(data),
                ECDSA.transcodeSignatureToConcat(sig, ECDSA.getSignatureByteArrayLength(JWSAlgorithm.ES256K))
            ).value()
        }

    override fun encrypt(
        keyId: KeyId,
        algorithm: String,
        plainText: ByteArray,
        authData: ByteArray?,
        iv: ByteArray?
    ): ByteArray {
        TODO("Not yet implemented")
    }

    override fun decrypt(
        keyId: KeyId,
        algorithm: String,
        plainText: ByteArray,
        authData: ByteArray?,
        iv: ByteArray?
    ): ByteArray {
        TODO("Not yet implemented")
    }

    override fun signEthTransaction(keyId: KeyId, encodedTx: ByteArray): ECDSASignature =
        signWithClient(keyId, Hash.sha3(encodedTx)).let {
            ECDSASignature(
                BigInteger(1, it.copyOfRange(0, it.size / 2)),
                BigInteger(1, it.copyOfRange(it.size / 2, it.size))
            ).toCanonicalised()
        }

    private fun signWithClient(keyId: KeyId, data: ByteArray): ByteArray =
        getKey(keyId.id).let {
            val keyIdentifier = it.kid()
            val keyOperationResult = client.sign(keyIdentifier, getAlgorithm(it.crv()), data)

            if (keyOperationResult.kid() != keyIdentifier)
                throw IllegalStateException("Actual signer \"${keyOperationResult.kid()}\" was different than expected $keyIdentifier.")

            keyOperationResult.result()
                ?: throw IllegalStateException("Empty response from the keyvault when signing with $keyIdentifier")
        }

    private fun getKey(keyName: String): JsonWebKey =
        client.getKey(configuration.baseURL, keyName)?.key()
            ?: throw IllegalArgumentException("Key $keyName not found.")

    private fun getAlgorithm(curve: JsonWebKeyCurveName) =
        when (curve.toString()) {
            "SECP256K1" -> JsonWebKeySignatureAlgorithm("ECDSA256")
            else -> throw IllegalArgumentException("Curve $curve not supported yet.")
        }
}
