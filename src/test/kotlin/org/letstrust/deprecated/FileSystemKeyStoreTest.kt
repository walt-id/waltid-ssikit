package org.letstrust.deprecated

import org.junit.Before
import org.letstrust.services.key.FileSystemKeyStore

open class FileSystemKeyStoreTest : KeyStoreTest() {

    @Before
    fun setUp() {
        kms.setKeyStore(FileSystemKeyStore)
    }
}
