package com.sloth.runveal.data.local

import com.sloth.runveal.auth.AndroidAppContextHolder
import java.io.File

actual class FileStore actual constructor() {
    private val root: File get() = AndroidAppContextHolder.context.filesDir

    actual fun write(relativePath: String, content: String) {
        resolve(relativePath).also { it.parentFile?.mkdirs() }.writeText(content)
    }

    actual fun append(relativePath: String, content: String) {
        resolve(relativePath).also { it.parentFile?.mkdirs() }.appendText(content)
    }

    actual fun read(relativePath: String): String? {
        val file = resolve(relativePath)
        return if (file.exists()) file.readText() else null
    }

    actual fun delete(relativePath: String) {
        resolve(relativePath).delete()
    }

    actual fun exists(relativePath: String): Boolean = resolve(relativePath).exists()

    private fun resolve(relativePath: String): File = File(root, relativePath)
}
