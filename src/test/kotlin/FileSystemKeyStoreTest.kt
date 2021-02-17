import org.junit.Before
import org.letstrust.FileSystemKeyStore

open class FileSystemKeyStoreTest : KeyStoreTest() {

    @Before
    fun setUp() {
        kms.setKeyStore(FileSystemKeyStore)
    }
}
