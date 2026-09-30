package com.example.model

data class Place(
    val id: String,
    val name: String,
    val categories: List<String>,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    var matchRate: Int = 0,
    var likeCount: Int = 0,
    val totalVoters: Int = 4,
    var isLikedByMe: Boolean = false,
    var isDislikedByMe: Boolean = false,
    var isSelected: Boolean = false
)

data class ScheduleItem(
    val id: String,
    val tripId: String = "",
    val placeId: String = "",
    var placeName: String,
    var category: String = "관광",
    var time: String,
    var order: Int = 0,
    var memo: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val date: String = ""
) {
    val orderIndex: Int get() = order
}

/**
 * [Tmap API 연동 준비 데이터 구조]
 * 향후 Android Studio에서 Tmap SDK 연동 시:
 * 1) routePlaces -> TMapMarkerItem 마커 표시
 * 2) routePlaces의 위경도(latitude, longitude) 및 order -> TMapData.findPathDataAllType() 경로 탐색
 */
data class RoutePlace(
    val placeName: String,
    val latitude: Double?,
    val longitude: Double?,
    val order: Int,
    val time: String,
    val scheduleItemId: String = "",
    val memo: String = ""
)

object TmapRouteHelper {
    /**
     * [5. 지도 데이터 처리 로직 분리]
     * 현재 DAY의 일정을 시간 및 order 순서로 정렬하여 Tmap 연동용 RoutePlace 목록을 생성합니다.
     */
    fun buildRoutePlaces(items: List<ScheduleItem>): List<RoutePlace> {
        val sorted = items.sortedWith(
            compareBy<ScheduleItem> { PlaceDataProvider.timeToMinutes(it.time) }
                .thenBy { it.order }
        )
        return sorted.mapIndexed { index, item ->
            RoutePlace(
                placeName = item.placeName,
                latitude = item.latitude,
                longitude = item.longitude,
                order = index + 1,
                time = item.time,
                scheduleItemId = item.id,
                memo = item.memo
            )
        }
    }
}

data class DaySchedule(
    val dayNumber: Int,
    val dateStr: String,
    var items: List<ScheduleItem> = emptyList()
)

object PlaceDataProvider {
    /**
     * 장소의 취향 태그(categories)에 해당하는 그룹 취향 점수의 평균을 계산합니다.
     * 예: 광안리 tags [맛집, 사진, 휴식] / 그룹 점수 맛집 85, 사진 75, 휴식 70 -> (85 + 75 + 70) / 3 = 약 77%
     */
    fun calculateMatchRate(categories: List<String>, groupScores: Map<String, Int>): Int {
        if (groupScores.isEmpty() || categories.isEmpty()) return 0
        var sum = 0
        var count = 0
        categories.forEach { tag ->
            val score = groupScores[tag]
            if (score != null) {
                sum += score
                count++
            }
        }
        return if (count > 0) kotlin.math.round(sum.toDouble() / count).toInt() else 0
    }

    /**
     * 목적지 및 실제 그룹 취향 결과를 바탕으로 장소별 적합도를 계산하고
     * 적합도가 높은 순서대로 정렬하여 반환합니다.
     * 그룹 취향 데이터가 없으면 가짜 적합도를 생성하지 않고 matchRate=0으로 반환합니다.
     */
    fun getInitialPlaces(destination: String = "부산", groupScores: Map<String, Int> = emptyMap()): List<Place> {
        val basePlaces = when {
            destination.contains("제주") -> listOf(
                Place("p_jeju_1", "협재 해수욕장 & 비양도", listOf("자연", "휴식"), "에메랄드빛 바다와 은빛 모래사장이 아름다운 대표 해변", 33.3941, 126.2397, 0, 3, isSelected = false),
                Place("p_jeju_2", "성산일출봉", listOf("관광", "자연"), "유네스코 세계자연유산으로 지정된 화산 분화구", 33.4580, 126.9427, 0, 3, isSelected = false),
                Place("p_jeju_3", "비자림 숲길", listOf("휴식", "자연"), "피톤치드 가득한 천년의 숲 산책로", 33.4912, 126.8115, 0, 2, isSelected = false),
                Place("p_jeju_4", "동문시장 야시장", listOf("맛집", "쇼핑"), "딱새우회와 흑돼지 김치말이 등 야식의 성지", 33.5126, 126.5282, 0, 3, isSelected = false),
                Place("p_jeju_5", "애월 카페거리", listOf("사진", "맛집"), "한담해변을 따라 늘어선 감성 오션뷰 카페 거리", 33.4623, 126.3111, 0, 2, isSelected = false),
                Place("p_jeju_6", "쇠소깍 나룻배", listOf("액티비티", "자연"), "투명 카약과 나룻배를 타며 즐기는 신비로운 계곡", 33.2525, 126.6231, 0, 2, isSelected = false)
            )
            destination.contains("강릉") -> listOf(
                Place("p_gang_1", "안목해변 커피거리", listOf("맛집", "휴식"), "바다를 바라보며 커피 한 잔의 여유를 즐기는 곳", 37.7718, 128.9482, 0, 4, isSelected = false),
                Place("p_gang_2", "경포대 & 경포호", listOf("관광", "자연"), "관동팔경의 대표 명소이자 벚꽃과 자전거 산책로", 37.7951, 128.8966, 0, 3, isSelected = false),
                Place("p_gang_3", "정동진 해돋이 공원", listOf("사진", "자연"), "푸른 동해바다와 모래시계 공원 포토존", 37.6908, 129.0326, 0, 3, isSelected = false),
                Place("p_gang_4", "강릉 중앙시장", listOf("맛집", "쇼핑"), "닭강정, 배니닭강정, 오징어순대 먹거리 성지", 37.7540, 128.8988, 0, 3, isSelected = false),
                Place("p_gang_5", "아르떼뮤지엄 강릉", listOf("사진", "체험"), "빛과 소리로 만든 몰입형 미디어아트 전시관", 37.7915, 128.9056, 0, 2, isSelected = false)
            )
            else -> listOf(
                Place("p_gwanganri", "광안리", listOf("맛집", "사진", "휴식"), "야경과 광안대교 뷰 맛집을 함께 즐기기 좋은 대표 핫플레이스", 35.1532, 129.1186, 0, 4, isSelected = false),
                Place("p_haeundae", "해운대", listOf("관광", "맛집", "사진"), "해변 열차와 미포 철길, 주변 해리단길 맛집 탐방", 35.1587, 129.1604, 0, 3, isSelected = false),
                Place("p_gamcheon", "감천문화마을", listOf("사진", "관광"), "골목 곳곳 알록달록한 벽화와 어린왕자 포토존이 있는 명소", 35.0975, 129.0106, 0, 3, isSelected = false),
                Place("p_gukje", "국제시장", listOf("맛집", "쇼핑"), "씨앗호떡, 비빔당면 등 다양한 부산 길거리 먹거리의 성지", 35.1017, 129.0278, 0, 3, isSelected = false),
                Place("p_thebay", "더베이101", listOf("사진", "휴식"), "마린시티 마천루 반영 사진과 시원한 바닷바람을 즐기는 감성 코스", 35.1565, 129.1523, 0, 3, isSelected = false),
                Place("p_huinnyeoul", "흰여울문화마을", listOf("사진", "휴식"), "영도 해안 절벽을 따라 이어지는 감성 카페와 해안 터널", 35.0768, 129.0452, 0, 2, isSelected = false),
                Place("p_yonggungsa", "해동용궁사", listOf("관광", "자연"), "기장 바다 절벽 위에 세워진 아름다운 사찰과 탁 트인 풍경", 35.1884, 129.2234, 0, 2, isSelected = false),
                Place("p_songdo", "송도해수욕장", listOf("액티비티", "자연"), "바다 위를 건너는 크리스탈 케이블카와 용궁구름다리", 35.0789, 129.0202, 0, 2, isSelected = false)
            )
        }

        if (groupScores.isNotEmpty()) {
            basePlaces.forEach { place ->
                place.matchRate = calculateMatchRate(place.categories, groupScores)
            }
            return basePlaces.sortedByDescending { it.matchRate }
        } else {
            basePlaces.forEach { place ->
                place.matchRate = 0
            }
            return basePlaces
        }
    }

    fun parseDate(dateStr: String, defaultYear: Int = 2026): java.time.LocalDate? {
        val clean = dateStr.trim()
        val nums = Regex("\\d+").findAll(clean).map { it.value.toInt() }.toList()
        return try {
            when {
                nums.size >= 3 -> {
                    if (nums[0] > 1000) java.time.LocalDate.of(nums[0], nums[1], nums[2])
                    else java.time.LocalDate.of(defaultYear, nums[0], nums[1])
                }
                nums.size == 2 -> java.time.LocalDate.of(defaultYear, nums[0], nums[1])
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 여행 시작일(startDate)과 종료일(endDate)을 기반으로 DAY 1, DAY 2, DAY 3 등 실제 일자별 빈 일정을 동적으로 생성합니다.
     * 하드코딩하지 않고 여행 날짜를 기준으로 계산합니다.
     */
    fun generateDays(startDate: String, endDate: String): List<DaySchedule> {
        val s = parseDate(startDate)
        val e = parseDate(endDate)
        if (s != null && e != null && !e.isBefore(s)) {
            val days = mutableListOf<DaySchedule>()
            var curr: java.time.LocalDate = s
            var dayNum = 1
            while (!curr.isAfter(e) && dayNum <= 30) {
                val dateStr = String.format(java.util.Locale.KOREA, "%02d.%02d", curr.monthValue, curr.dayOfMonth)
                days.add(DaySchedule(dayNumber = dayNum, dateStr = dateStr, items = emptyList()))
                curr = curr.plusDays(1)
                dayNum++
            }
            if (days.isNotEmpty()) return days
        }
        val startStr = startDate.ifBlank { "DAY 1" }
        return if (endDate.isNotBlank() && endDate != startDate) {
            listOf(
                DaySchedule(dayNumber = 1, dateStr = startStr, items = emptyList()),
                DaySchedule(dayNumber = 2, dateStr = endDate, items = emptyList())
            )
        } else {
            listOf(DaySchedule(dayNumber = 1, dateStr = startStr, items = emptyList()))
        }
    }

    /**
     * "10:00", "14:30" 등의 시간 문자열을 분 단위 정수로 변환하여 시간순 정렬에 사용합니다.
     */
    fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.trim().split(":", "시", ".").mapNotNull { it.trim().toIntOrNull() }
        return when (parts.size) {
            2 -> parts[0] * 60 + parts[1]
            1 -> parts[0] * 60
            else -> 9999
        }
    }

    /**
     * 여행 일정 날짜에 맞는 빈 DAY 목록을 반환합니다. 임의의 mock 일정을 생성하지 않습니다.
     */
    fun createScheduleFromPlaces(
        places: List<Place>,
        startDate: String = "10.10",
        endDate: String = "10.12"
    ): List<DaySchedule> {
        return generateDays(startDate, endDate)
    }

    fun getDefaultSchedule(): List<DaySchedule> = emptyList()
}
