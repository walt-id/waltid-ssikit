package id.walt.crypto

import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStoreSpi
import java.security.cert.Certificate
import java.util.*

open class LtKeyStoreSpi : KeyStoreSpi() {

    private val keys = Properties()

    override fun engineGetKey(alias: String?, password: CharArray?): Key {
        // val value = keys.getProperty(alias) ?: throw UnrecoverableKeyException("Unknown key: $alias")
        TODO("Not yet implemented")
    }

    override fun engineGetCertificateChain(alias: String?): Array<Certificate> {
        TODO("Not yet implemented")
    }

    override fun engineGetCertificate(alias: String?): Certificate {
        TODO("Not yet implemented")
    }

    override fun engineGetCreationDate(alias: String?): Date {
        TODO("Not yet implemented")
    }

    override fun engineSetKeyEntry(alias: String?, key: Key?, password: CharArray?, chain: Array<out Certificate>?) {
        TODO("Not yet implemented")
    }

    override fun engineSetKeyEntry(alias: String?, key: ByteArray?, chain: Array<out Certificate>?) {
        TODO("Not yet implemented")
    }

    override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) {
        TODO("Not yet implemented")
    }

    override fun engineDeleteEntry(alias: String?) {
        TODO("Not yet implemented")
    }

    override fun engineAliases(): Enumeration<String> {
        TODO("Not yet implemented")
    }

    override fun engineContainsAlias(alias: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun engineSize(): Int {
        TODO("Not yet implemented")
    }

    override fun engineIsKeyEntry(alias: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun engineIsCertificateEntry(alias: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun engineGetCertificateAlias(cert: Certificate?): String {
        TODO("Not yet implemented")
    }

    override fun engineStore(stream: OutputStream?, password: CharArray?) {
        keys.store(stream, null)
    }

    override fun engineLoad(stream: InputStream?, password: CharArray?) {
        if (stream != null) {
            keys.load(stream)
        }
    }
}
