package com.swyp.runveal.dong.region

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `korea_region_hierarchy.json`을 [RegionHierarchy]로 읽는다. `version`/`sido`만 쓰고 나머지 메타
 * 필드(`source`, `codeRule`, `counts` 등)는 무시한다. */
object RegionHierarchyParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonText: String): RegionHierarchy {
        val root = json.decodeFromString<RegionHierarchyJson>(jsonText)
        val sido = root.sido.map { s ->
            Sido(
                code = s.code,
                name = s.name,
                sigungu = s.sigungu.map { sg ->
                    Sigungu(
                        code = sg.code,
                        name = sg.name,
                        parentCity = sg.parentCity?.let { ParentCity(code = it.code, name = it.name) },
                        dongs = sg.dongs.map { Dong(code = it.code, name = it.name) },
                    )
                },
            )
        }
        return RegionHierarchy(version = root.version, sido = sido)
    }
}

@Serializable
private data class RegionHierarchyJson(val version: String, val sido: List<SidoJson> = emptyList())

@Serializable
private data class SidoJson(val code: String, val name: String, val sigungu: List<SigunguJson> = emptyList())

@Serializable
private data class SigunguJson(
    val code: String,
    val name: String,
    val parentCity: ParentCityJson? = null,
    val dongs: List<DongJson> = emptyList(),
)

@Serializable
private data class ParentCityJson(val code: String, val name: String)

@Serializable
private data class DongJson(val code: String, val name: String)
