package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * 여행 일정 한 칸 (예: 1일차의 "부산역").
 * Firestore 경로: trips/{tripId}/schedules/{scheduleId}
 *
 * 장소 정보(address, lat, lng, placeId)는 카카오맵에서 고른 경우에만 채워집니다.
 */
data class Schedule(
    val scheduleId: String = "",
    val day: Int = 0,               // 0 = 1일차, 1 = 2일차 …
    val title: String = "",         // 장소 이름 또는 할 일 (예: 부산역)
    val memo: String = "",          // 메모 (선택)
    val address: String = "",       // 카카오 장소 주소 (선택)
    val lat: Double? = null,
    val lng: Double? = null,
    val placeId: String = "",       // 카카오 장소 id (선택)
    val order: Long = 0L,           // 같은 날 안에서의 순서 (작을수록 위)
    val createdBy: String = "",
    val createdAt: Long = 0L
)

/** 여행 날짜 하루 (일정 화면 상단 "1일차 8.1 (토)" 탭 한 칸) */
data class TripDay(
    val index: Int,        // 0부터
    val month: Int,        // 1~12
    val dayOfMonth: Int,
    val weekday: String    // "토"
) {
    val label: String get() = "${index + 1}일차"
    val shortDate: String get() = "$month.$dayOfMonth ($weekday)"
    val longDate: String get() = "${month}월 ${dayOfMonth}일 ${weekday}요일"
}

object TripDays {
    private val WEEKDAYS = listOf("일", "월", "화", "수", "목", "금", "토")
    private const val MAX_DAYS = 60

    /** "2026-08-01" ~ "2026-08-03" → 1일차, 2일차, 3일차 */
    fun of(trip: Trip): List<TripDay> {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
            isLenient = false
        }
        val start = runCatching { format.parse(trip.startDate) }.getOrNull() ?: return emptyList()
        val end = runCatching { format.parse(trip.endDate) }.getOrNull() ?: start
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = start }
        val days = mutableListOf<TripDay>()
        while (!cal.time.after(end) && days.size < MAX_DAYS) {
            days += TripDay(
                index = days.size,
                month = cal.get(Calendar.MONTH) + 1,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                weekday = WEEKDAYS[cal.get(Calendar.DAY_OF_WEEK) - 1]
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return days
    }
}
