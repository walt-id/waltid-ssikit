package id.walt.custodian

import id.walt.crypto.Key
import id.walt.crypto.KeyAlgorithm
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

object CustodianKeyTestUtils {

    private lateinit var key1: Key
    private lateinit var key2: Key

    fun StringSpec.standardKeyTests(custodian: Custodian) {
        "1.1: Generate EdDSA_Ed25519 key" {
            key1 = custodian.generateKey(KeyAlgorithm.EdDSA_Ed25519)
        }

        "1.2: Generate ECDSA_Secp256k1 key" {
            key2 = custodian.generateKey(KeyAlgorithm.ECDSA_Secp256k1)
        }

        "2.1: Load EdDSA_Ed25519 key" {
            val loadedKey = custodian.getKey(key1.keyId.id)

            loadedKey.keyId.id shouldBe key1.keyId.id
            loadedKey.algorithm shouldBe KeyAlgorithm.EdDSA_Ed25519
        }

        "2.2: Load ECDSA_Secp256k1 key" {
            val loadedKey = custodian.getKey(key2.keyId.id)

            loadedKey.keyId.id shouldBe key2.keyId.id
            loadedKey.algorithm shouldBe KeyAlgorithm.ECDSA_Secp256k1
        }

//        "3: List keys size" {
//            custodian.listKeys().size shouldBeGreaterThanOrEqual 2
//        }
//
//        "4: Delete keys" {
//            custodian.listKeys().forEach {
//                custodian.deleteKey(it.keyId.id)
//            }
//
//            custodian.listKeys().size shouldBeExactly 0
//        }
//
//        "5.1: Store EdDSA_Ed25519 key" {
//            custodian.storeKey(key1)
//        }
//
//        "5.2: Store ECDSA_Secp256k1 key" {
//            custodian.storeKey(key2)
//        }
//
//        "6.1: Retrieve stored EdDSA_Ed25519 key" {
//            val loadedKey = custodian.getKey(key1.keyId.id)
//
//            loadedKey.keyId.id shouldBe key1.keyId.id
//            loadedKey.algorithm shouldBe KeyAlgorithm.EdDSA_Ed25519
//        }
//
//        "6.2: Retrieve stored ECDSA_Secp256k1 key" {
//            val loadedKey = custodian.getKey(key2.keyId.id)
//
//            loadedKey.keyId.id shouldBe key2.keyId.id
//            loadedKey.algorithm shouldBe KeyAlgorithm.ECDSA_Secp256k1
//
//            custodian.listKeys().forEach {
//                custodian.deleteKey(it.keyId.id)
//            }
//        }
    }

}
