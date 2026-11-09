package com.swyp.runveal.dong

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DongBoundaryParserTest {

    private val samplePolygon = """
        {
          "type": "FeatureCollection",
          "features": [
            {
              "type": "Feature",
              "properties": { "adm_cd2": "4420057000", "name": "온양1동", "sgg": "44200" },
              "geometry": {
                "type": "Polygon",
                "coordinates": [[[127.0, 36.7], [127.1, 36.7], [127.1, 36.8], [127.0, 36.8], [127.0, 36.7]]]
              }
            },
            {
              "type": "Feature",
              "properties": { "adm_cd2": "4420058000", "name": "온양2동", "sgg": "44200" },
              "geometry": {
                "type": "MultiPolygon",
                "coordinates": [
                  [[[128.0, 36.7], [128.1, 36.7], [128.1, 36.8], [128.0, 36.8], [128.0, 36.7]]],
                  [[[129.0, 36.7], [129.1, 36.7], [129.1, 36.8], [129.0, 36.8], [129.0, 36.7]]]
                ]
              }
            }
          ]
        }
    """.trimIndent()

    @Test
    fun parsesPolygonFeatureWithProperties() {
        val boundaries = DongBoundaryParser.parse(samplePolygon)

        val onyang1 = boundaries.first { it.admCd2 == "4420057000" }
        assertEquals("온양1동", onyang1.name)
        assertEquals(1, onyang1.rings.size)
        assertEquals(5, onyang1.rings[0].size)
    }

    @Test
    fun parsesMultipolygonFeatureIntoOneRingPerPart() {
        val boundaries = DongBoundaryParser.parse(samplePolygon)

        val onyang2 = boundaries.first { it.admCd2 == "4420058000" }
        assertEquals(2, onyang2.rings.size)
    }

    @Test
    fun skipsFeaturesWithoutAdmCd2() {
        val missingKey = """
            {"type":"FeatureCollection","features":[
              {"type":"Feature","properties":{"name":"x"},
               "geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}
            ]}
        """.trimIndent()

        val boundaries = DongBoundaryParser.parse(missingKey)

        assertTrue(boundaries.isEmpty())
    }

    @Test
    fun returnsEmptyListForDocumentWithNoFeatures() {
        assertNull(DongBoundaryParser.parse("""{"type":"FeatureCollection"}""").firstOrNull())
    }
}
