package com.swyp.runveal.network.dong

import kotlin.test.Test
import kotlin.test.assertEquals

class DongResponseTest {

    private fun dong(name: String) = DongResponse(code = "4413152000", gu = "44131", name = name)

    @Test
    fun `시도를 떼고 시군구부터 보여준다`() {
        assertEquals("천안시서북구 부성2동", dong("충청남도 천안시서북구 부성2동").shortName)
        assertEquals("종로구 사직동", dong("서울특별시 종로구 사직동").shortName)
        assertEquals("아산시 둔포면", dong("충청남도 아산시 둔포면").shortName)
    }

    /** 시와 구가 따로 떨어진 이름(경기도 수원시 장안구 …)도 앞 한 토막만 뗀다. */
    @Test
    fun `시군구가 두 토막이어도 시도만 뗀다`() {
        assertEquals("수원시 장안구 파장동", dong("경기도 수원시 장안구 파장동").shortName)
    }

    /** 세종은 시군구 단계가 없지만 서버가 "세종특별자치시 세종시 …"로 채워 내려준다(실제 응답 확인). */
    @Test
    fun `세종도 같은 규칙으로 처리된다`() {
        assertEquals("세종시 보람동", dong("세종특별자치시 세종시 보람동").shortName)
    }

    /** 혹시 시군구 토막이 없는 이름이 오면 앞을 떼지 않는다 — 떼면 어느 시의 동인지 알 수 없다. */
    @Test
    fun `토막이 둘뿐이면 그대로 둔다`() {
        assertEquals("세종특별자치시 한솔동", dong("세종특별자치시 한솔동").shortName)
    }

    @Test
    fun `토막이 하나뿐이거나 비어 있어도 그대로 둔다`() {
        assertEquals("사직동", dong("사직동").shortName)
        assertEquals("", dong("").shortName)
    }
}
