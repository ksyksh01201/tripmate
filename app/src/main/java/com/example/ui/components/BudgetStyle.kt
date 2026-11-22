package com.example.ui.components

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color

/**
 * 가계부 분류(해시태그)별 고유 색.
 * 지출 추가 팝업의 분류 칩, 지출 목록의 태그, 소비 분석 그래프가 모두 이 색을 같이 씁니다.
 *
 * @param main 그래프 조각 / 선택된 칩 배경
 * @param text 연한 태그 위의 글자색
 * @param soft 연한 태그 배경
 */
data class CategoryColor(val main: Color, val text: Color, val soft: Color)

object CategoryPalette {
    private val map = mapOf(
        "숙박" to CategoryColor(Color(0xFF5B8FDE), Color(0xFF197CF0), Color(0x3D197CF0)),
        "식비" to CategoryColor(Color(0xFFF0825A), Color(0xFFE35626), Color(0x2EFF7A4D)),
        "카페" to CategoryColor(Color(0xFFA9744F), Color(0xFF8B5E3C), Color(0x2E8B5E3C)),
        "교통" to CategoryColor(Color(0xFF5FB3A8), Color(0xFF2E8B7E), Color(0x2E5FB3A8)),
        "관광" to CategoryColor(Color(0xFF9187CF), Color(0xFF7A6CC4), Color(0x2E9187CF)),
        "쇼핑" to CategoryColor(Color(0xFFF5B942), Color(0xFFD08A00), Color(0x2EF5B942))
    )
    private val other = CategoryColor(Color(0xFFB0B0B0), Color(0xFF727272), Color(0x29B0B0B0))

    fun of(category: String): CategoryColor = map[category] ?: other
}

/**
 * 여행 멤버 이름 (uid → 이름). 여행 문서의 memberNames를 받아 채워 둡니다.
 * Compose 상태라서 이름이 들어오면 화면이 자동으로 다시 그려져요.
 */
object MemberNameCache {
    val names = mutableStateMapOf<String, String>()
}
