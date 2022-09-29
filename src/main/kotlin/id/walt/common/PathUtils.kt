package id.walt.common

import java.nio.file.Path
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.isDirectory
import kotlin.io.path.notExists

object PathUtils {

    private fun Path.forEachRecursiveExisting(block: Path.() -> Unit) {
        if (this.isDirectory()) this.forEachDirectoryEntry { it.forEachRecursive(block) }
        else block.invoke(this)
    }

    fun Path.forEachRecursive(block: Path.() -> Unit) {
        if (this.notExists()) return

        this.forEachRecursiveExisting(block)
    }
}