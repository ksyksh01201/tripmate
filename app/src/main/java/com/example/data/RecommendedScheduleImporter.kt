package com.example.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 밸런스 게임 결과에서 "일정 만들기"로 고른 장소를 일정 탭에 넣어 줍니다.
 * - 카카오 장소 검색으로 주소와 좌표를 찾아 지도/동선에도 나오게 해요 (못 찾으면 이름만).
 * - 여행 날짜에 고르게 나눠 넣어요 (예: 2박 3일에 6곳이면 하루 2곳).
 * - 이미 같은 이름의 일정이 있으면 다시 넣지 않아요.
 * 탭을 바로 옮겨도 끝까지 저장되도록 화면과 상관없는 범위에서 실행합니다.
 */
object RecommendedScheduleImporter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun import(
        trip: Trip,
        places: List<RecommendedPlace>,
        onDone: (added: Int) -> Unit = {}
    ) {
        if (trip.tripId.isBlank() || places.isEmpty()) return
        scope.launch {
            val added = runCatching { importNow(trip, places) }.getOrDefault(0)
            onDone(added)
        }
    }

    private suspend fun importNow(trip: Trip, places: List<RecommendedPlace>): Int {
        val repo = ScheduleRepository()
        val existing = withTimeoutOrNull(5_000) { repo.getSchedules(trip.tripId).first() }.orEmpty()
        val existingTitles = existing.map { it.title.trim() }.toSet()
        val todo = places.filter { it.name.isNotBlank() && it.name.trim() !in existingTitles }
            .distinctBy { it.name.trim() }
        if (todo.isEmpty()) return 0

        val dayCount = TripDays.of(trip).size.coerceAtLeast(1)
        val perDay = (todo.size + dayCount - 1) / dayCount
        val destination = trip.destination.ifBlank { trip.title }
        val base = System.currentTimeMillis()
        var added = 0
        todo.forEachIndexed { i, place ->
            val found = if (KakaoPlaceSearch.hasKey) {
                runCatching { KakaoPlaceSearch.search("$destination ${place.name}").firstOrNull() }.getOrNull()
                    ?: runCatching { KakaoPlaceSearch.search(place.name).firstOrNull() }.getOrNull()
            } else null
            val schedule = Schedule(
                day = (i / perDay).coerceAtMost(dayCount - 1),
                title = place.name,
                memo = place.reason,
                address = found?.address.orEmpty(),
                lat = found?.lat,
                lng = found?.lng,
                placeId = found?.id.orEmpty(),
                order = base + i
            )
            if (runCatching { repo.addSchedule(trip.tripId, schedule) }.isSuccess) added++
        }
        return added
    }
}
