package com.swyp.runveal.data.local

/**
 * 루트 디렉터리(Android `Context.filesDir`, iOS `NSDocumentDirectory`) 기준 상대경로로 GPX 파일을
 * 읽고 쓰는 최소 인터페이스(docs/local-db/local-db-schema-design.md §4). DB 컬럼(`gpx_filtered_path`
 * 등)엔 이 상대경로만 저장한다 — 절대경로는 재설치/기기변경마다 바뀐다.
 */
expect class FileStore() {
    fun write(relativePath: String, content: String)

    /** 녹화 중 포인트가 들어올 때마다 호출 — GPX를 통째로 다시 쓰지 않고 이어붙인다. */
    fun append(relativePath: String, content: String)

    fun read(relativePath: String): String?

    fun delete(relativePath: String)

    fun exists(relativePath: String): Boolean
}
