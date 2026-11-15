package com.swyp.runveal.core.map.internal.maplibre

import com.swyp.runveal.designsystem.map.RunvealDong
import com.swyp.runveal.dong.DongBoundary
import com.swyp.runveal.dong.DongPolygonPart
import com.swyp.runveal.dong.Polylabel
import com.swyp.runveal.dong.RingWinding
import com.swyp.runveal.map.LatLon
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.MultiPolygon
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

/**
 * [com.swyp.runveal.dong]의 순수 로직 + 원본 동 경계 GeoJSON(asset)을 받아 core:map이 쓸 fill/line/label
 * GeoJSON 문자열 3개를 만든다. `org.maplibre.geojson`을 쓰는 유일한 지점 — 지도 SDK 타입은 여기서
 * 끝나고 공개 API([com.swyp.runveal.core.map.DongOverlayGeoJson]은 문자열만 들고 있다) 밖으로 안 나간다.
 */
internal object DongGeoJsonBuilder {

    data class Result(
        val fillGeoJson: String,
        val lineGeoJson: String,
        val labelGeoJson: String,
        /** 빛 이미지도 선과 **같은** 정점을 쓰도록 여기서 만든 polygon part를 함께 돌려준다 — 경계
         * GeoJSON을 따로 한 번 더 파싱하면 전국 데이터를 두 번 읽게 되고, 전처리(ring 방향 정규화)가
         * 어긋날 여지도 생긴다. */
        val polygonPartsByAdmCd2: Map<String, List<DongPolygonPart>>,
    )

    fun build(boundaries: List<DongBoundary>, rawBoundaryGeoJson: String, percents: Map<String, Int>): Result {
        val raw = FeatureCollection.fromJson(rawBoundaryGeoJson)
        val partsByAdmCd2 = polygonPartsByAdmCd2(raw)
        val nameByAdmCd2 = boundaries.associate { it.admCd2 to it.name }

        return Result(
            fillGeoJson = buildFillGeoJson(raw, percents),
            lineGeoJson = buildLineGeoJson(partsByAdmCd2, percents),
            labelGeoJson = buildLabelGeoJson(partsByAdmCd2, nameByAdmCd2, percents),
            polygonPartsByAdmCd2 = partsByAdmCd2,
        )
    }

    private fun buildFillGeoJson(raw: FeatureCollection, percents: Map<String, Int>): String {
        val features = raw.features()?.mapNotNull { feature ->
            val admCd2 = feature.getStringProperty("adm_cd2") ?: return@mapNotNull null
            feature.withLevelProperties(admCd2, percents)
        } ?: emptyList()
        return FeatureCollection.fromFeatures(features).toJson()
    }

    /**
     * 동끼리 경계를 공유하면 두 선이 같은 자리에 겹쳐 그려지는데, MapLibre는 피처 배열 순서대로(뒤에
     * 올수록 위에) 그린다. level 오름차순으로 정렬해서 레벨이 높은 쪽 선이 항상 위에 그려지게 한다 —
     * 안 그러면 어느 쪽이 덮느냐가 `partsByAdmCd2`의 순회 순서(우연)에 달려서, 예를 들어 방축동처럼
     * 낮은 레벨 동이 이웃 동 뒤에 그려지면 공유하지 않는 바깥쪽 경계만 보이는 것처럼 보일 수 있다.
     */
    private fun buildLineGeoJson(partsByAdmCd2: Map<String, List<DongPolygonPart>>, percents: Map<String, Int>): String {
        val features = mutableListOf<Feature>()
        partsByAdmCd2.entries
            .sortedBy { (admCd2, _) -> RunvealDong.levelOf(percents[admCd2] ?: 0) }
            .forEach { (admCd2, parts) ->
                val level = RunvealDong.levelOf(percents[admCd2] ?: 0)
                parts.forEach { part ->
                    (listOf(part.exterior) + part.holes).forEach { ring ->
                        val line = Feature.fromGeometry(LineString.fromLngLats(ring.map { Point.fromLngLat(it.lon, it.lat) }))
                        line.addStringProperty("adm_cd2", admCd2)
                        line.addNumberProperty("level", level)
                        features += line
                    }
                }
            }
        return FeatureCollection.fromFeatures(features).toJson()
    }

    private fun buildLabelGeoJson(
        partsByAdmCd2: Map<String, List<DongPolygonPart>>,
        nameByAdmCd2: Map<String, String>,
        percents: Map<String, Int>,
    ): String {
        val features = partsByAdmCd2.mapNotNull { (admCd2, parts) ->
            val name = nameByAdmCd2[admCd2] ?: return@mapNotNull null
            val largest = parts.maxByOrNull { ringAreaAbs(it.exterior) } ?: return@mapNotNull null
            val anchor = Polylabel.pole(largest.exterior, largest.holes)
            val percent = percents[admCd2] ?: 0
            Feature.fromGeometry(Point.fromLngLat(anchor.lon, anchor.lat)).apply {
                addStringProperty("adm_cd2", admCd2)
                addStringProperty("name", name)
                addNumberProperty("level", RunvealDong.levelOf(percent))
                addStringProperty("percentValue", "$percent")
            }
        }
        return FeatureCollection.fromFeatures(features).toJson()
    }

    private fun Feature.withLevelProperties(admCd2: String, percents: Map<String, Int>): Feature {
        val percent = percents[admCd2] ?: 0
        addNumberProperty("level", RunvealDong.levelOf(percent))
        addStringProperty("percentValue", "$percent")
        return this
    }

    /** adm_cd2 -> 폴리곤 part(exterior + 구멍). 원본 GeoJSON에서 구멍·MultiPolygon을 그대로 보존해 뽑고,
     * 각 part는 [RingWinding.normalize]로 외곽은 반시계, 구멍은 시계로 맞춘다.
     *
     * 예전에는 인접 동의 공유 경계 정점을 15m 톨러런스로 합치는 보정(`VertexSnapper`)을 거쳤다 —
     * 서버 `index.geojson`이 동마다 따로 단순화돼 공유 경계가 몇 m씩 어긋났고, 그게 경계선이 두 줄로
     * 보이는 원인이었다. 2026-11-15 지도부터는 서버가 공유 경계를 같은 좌표로 내려주고(어긋난 정점
     * 0개), 번들 에셋도 원래 어긋나지 않아서 보정이 필요 없어졌다. 오히려 15m 안의 멀쩡한 정점까지
     * 합쳐 경계를 뭉갤 수 있어 걷어냈다. */
    private fun polygonPartsByAdmCd2(raw: FeatureCollection): Map<String, List<DongPolygonPart>> {
        val result = mutableMapOf<String, MutableList<DongPolygonPart>>()
        raw.features()?.forEach { feature ->
            val admCd2 = feature.getStringProperty("adm_cd2") ?: return@forEach
            polygonsOf(feature).forEach { polygon ->
                val rings = polygon.coordinates().map { ring ->
                    ring.map { LatLon(lat = it.latitude(), lon = it.longitude()) }
                }
                val part = RingWinding.normalize(exterior = rings.first(), holes = rings.drop(1))
                result.getOrPut(admCd2) { mutableListOf() }.add(part)
            }
        }
        return result
    }

    private fun polygonsOf(feature: Feature): List<Polygon> = when (val geometry = feature.geometry()) {
        is Polygon -> listOf(geometry)
        is MultiPolygon -> geometry.polygons()
        else -> emptyList()
    }

    private fun ringAreaAbs(ring: List<LatLon>): Double {
        var sum = 0.0
        for (i in ring.indices) {
            val a = ring[i]
            val b = ring[(i + 1) % ring.size]
            sum += a.lon * b.lat - b.lon * a.lat
        }
        return kotlin.math.abs(sum) / 2.0
    }
}
