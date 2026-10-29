package com.example.data.model

import java.util.Calendar
import java.util.UUID

data class Participant(
    val id: String,
    val name: String,
    val initial: String,
    val backgroundColorHex: Long
)

enum class TripThemeType {
    Busan,
    Jeju,
    Generic
}

data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val destination: String = "",
    val startDate: String,
    val endDate: String,
    val memberCount: Int = 1,
    val dDay: Int? = null,
    val participants: List<Participant> = emptyList(),
    val isCompleted: Boolean = false,
    val isPrimaryFeatured: Boolean = false,
    val themeType: TripThemeType = TripThemeType.Generic,
    val isMock: Boolean = false
) {
    val dateRangeText: String
        get() = "$startDate ~ $endDate"

    val dDayText: String
        get() {
            return when {
                dDay == null -> ""
                dDay == 0 -> "D-Day"
                dDay < 0 -> "D${dDay}" // e.g. D-10
                else -> "D+${dDay}"
            }
        }
}

object TripHelper {
    /**
     * 시작일 문자열을 기반으로 오늘 기준 D-Day를 계산합니다.
     * (예: 미래 날짜는 -10 -> D-10, 오늘은 0 -> D-Day)
     */
    fun calculateDDay(startDateStr: String): Int {
        try {
            val regex = Regex("(\\d{4})[^0-9]+(\\d{1,2})[^0-9]+(\\d{1,2})")
            val match = regex.find(startDateStr)
            if (match != null) {
                val (year, month, day) = match.destructured
                val startCal = Calendar.getInstance().apply {
                    set(year.toInt(), month.toInt() - 1, day.toInt(), 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val diffMillis = startCal.timeInMillis - todayCal.timeInMillis
                val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
                return -diffDays
            }
        } catch (_: Exception) {}
        return -10
    }

    /**
     * 목적지/이름 텍스트에 따라 테마 타입을 감지합니다.
     */
    fun detectThemeType(destination: String, title: String): TripThemeType {
        val combined = (destination + title).lowercase()
        return when {
            combined.contains("부산") -> TripThemeType.Busan
            combined.contains("제주") -> TripThemeType.Jeju
            else -> TripThemeType.Generic
        }
    }
}
