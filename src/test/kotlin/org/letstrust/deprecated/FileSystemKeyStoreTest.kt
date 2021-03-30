package org.letstrust.deprecated

import org.junit.Before

@Deprecated(message = "We proably remove FileSystemKeyStore at some point")
open class FileSystemKeyStoreTest {//: KeyStoreTest() {

    @Before
    fun setUp() {
       // kms.setKeyStore(FileSystemKeyStore)
    }
}
