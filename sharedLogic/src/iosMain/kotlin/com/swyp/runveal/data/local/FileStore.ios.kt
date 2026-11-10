package com.swyp.runveal.data.local

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSString
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

@OptIn(ExperimentalForeignApi::class)
actual class FileStore actual constructor() {
    private val fileManager = NSFileManager.defaultManager

    private val root: String by lazy {
        NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
            .firstOrNull() as? String
            ?: error("Document 디렉터리를 찾을 수 없습니다.")
    }

    actual fun write(relativePath: String, content: String) {
        val path = resolve(relativePath)
        ensureParentDirectoryExists(path)
        val data = (content as NSString).dataUsingEncoding(NSUTF8StringEncoding)
        fileManager.createFileAtPath(path, data, null)
    }

    actual fun append(relativePath: String, content: String) {
        write(relativePath, read(relativePath).orEmpty() + content)
    }

    actual fun read(relativePath: String): String? {
        val path = resolve(relativePath)
        if (!fileManager.fileExistsAtPath(path)) return null
        val data = fileManager.contentsAtPath(path) ?: return null
        return NSString.create(data, NSUTF8StringEncoding) as String?
    }

    actual fun delete(relativePath: String) {
        fileManager.removeItemAtPath(resolve(relativePath), null)
    }

    actual fun exists(relativePath: String): Boolean = fileManager.fileExistsAtPath(resolve(relativePath))

    private fun resolve(relativePath: String): String = "$root/$relativePath"

    private fun ensureParentDirectoryExists(path: String) {
        val parent = path.substringBeforeLast('/', missingDelimiterValue = "")
        if (parent.isNotEmpty() && !fileManager.fileExistsAtPath(parent)) {
            fileManager.createDirectoryAtPath(parent, true, null, null)
        }
    }
}
