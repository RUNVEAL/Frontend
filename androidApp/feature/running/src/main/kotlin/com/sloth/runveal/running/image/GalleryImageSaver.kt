package com.sloth.runveal.running.image

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GalleryImageSaver"

/** 갤러리에서 한 묶음으로 보이도록 앱 이름 폴더에 넣는다. */
private const val ALBUM_NAME = "Runveal"

private const val MIME_TYPE = "image/png"

/**
 * 비트맵을 기기 갤러리에 PNG로 저장한다. 성공하면 true.
 *
 * **Android 10(Q)부터는 권한이 필요 없다** — `MediaStore`에 넣는 건 앱이 만든 항목이라 저장소 권한
 * 없이 된다. 그 아래(이 앱의 최소 버전인 9 포함)에서는 `WRITE_EXTERNAL_STORAGE`가 있어야 해서
 * 호출부가 먼저 권한을 받아 둬야 한다([com.sloth.runveal.running.image.isLegacyStoragePermissionNeeded]).
 *
 * Q 이상에서는 `IS_PENDING`으로 반쯤 쓰다 만 파일이 갤러리에 보이지 않게 한다 — 저장 중에 갤러리를
 * 열면 깨진 썸네일이 보이는 걸 막는다.
 *
 * 실패는 예외로 던지지 않고 false로 알린다. 저장이 안 되는 건 앱을 멈출 일이 아니라 "저장하지
 * 못했어요"만 알리면 되는 상황이다.
 */
suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, bitmap, fileName)
            } else {
                saveToLegacyPicturesDir(context, bitmap, fileName)
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "이미지 저장 실패", e)
            false
        }
    }

private fun saveWithMediaStore(context: Context, bitmap: Bitmap, fileName: String) {
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE)
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM_NAME")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: error("MediaStore가 저장할 자리를 내주지 않았다")

    try {
        resolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            ?: error("저장 스트림을 열지 못했다")
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    } catch (e: Exception) {
        // 반쯤 쓰다 만 항목을 지운다 — 안 지우면 갤러리에 0바이트 파일이 남는다.
        resolver.delete(uri, null, null)
        throw e
    }
}

/**
 * Android 9 이하 — `MediaStore.RELATIVE_PATH`가 없어서 파일을 직접 쓰고 미디어 스캐너에 알린다.
 * 알리지 않으면 파일은 있는데 갤러리에 안 보인다.
 */
@Suppress("DEPRECATION")
private fun saveToLegacyPicturesDir(context: Context, bitmap: Bitmap, fileName: String) {
    val album = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM_NAME)
    if (!album.exists() && !album.mkdirs()) error("저장 폴더를 만들지 못했다")

    val file = File(album, fileName)
    FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }

    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DATA, file.absolutePath)
        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
        put(MediaStore.Images.Media.MIME_TYPE, MIME_TYPE)
    }
    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
}

/**
 * 저장 전에 `WRITE_EXTERNAL_STORAGE`를 받아야 하는 기기인지. Android 10(Q)부터는 필요 없다 —
 * 거기서 권한을 요청하면 시스템이 조용히 무시해서, 안 받고 저장하면 그만이다.
 */
fun isLegacyStoragePermissionNeeded(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
