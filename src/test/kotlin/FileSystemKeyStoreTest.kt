import org.junit.Before
import org.junit.Test

open class FileSystemKeyStoreTest : KeyStoreTest() {

    @Before
    fun setUp() {
        kms.setKeyStore(FileSystemKeyStore)
    }
}
