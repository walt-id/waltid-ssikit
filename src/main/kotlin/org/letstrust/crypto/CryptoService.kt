package org.letstrust.crypto

import org.web3j.crypto.ECDSASignature

interface CryptoService {

    fun generateKey(algorithm: KeyAlgorithm): KeyId
    fun sign(keyId: KeyId, data: ByteArray): ByteArray
    fun verify(keyId: KeyId, sig: ByteArray, data: ByteArray): Boolean
    fun encrypt(keyId: KeyId, algorithm: String, plainText: ByteArray, authData: ByteArray?, iv: ByteArray?): ByteArray
    fun decrypt(keyId: KeyId, algorithm: String, plainText: ByteArray, authData: ByteArray?, iv: ByteArray?): ByteArray


    /******
     * The following method produces the right signature, that is required for signing the ETH transaction for registering the DID EBSI.
     *
     * TODO The method should be removed, once the main sign() method support the correct configuration. Going forward we will:
     *  1.) implement a test-case so that we generate the ECDSASignature in ASN1. DER format
     *  2.) implement and validate the signature using JCA
     *  3.) remove the method and replace it with the main sign interface
     */
    fun signEthTransaction(keyId: KeyId, encodedTx: ByteArray): ECDSASignature?
}



