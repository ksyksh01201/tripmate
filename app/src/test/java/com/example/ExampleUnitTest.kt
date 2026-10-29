package com.example

import com.example.data.model.Trip
import com.example.data.model.TripHelper
import com.example.data.model.TripThemeType
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun detectThemeType_returnsCorrectTheme() {
        assertEquals(TripThemeType.Busan, TripHelper.detectThemeType("부산", "여름 휴가"))
        assertEquals(TripThemeType.Jeju, TripHelper.detectThemeType("제주도", "힐링 여행"))
        assertEquals(TripThemeType.Generic, TripHelper.detectThemeType("강릉", "맛집 투어"))
    }

    @Test
    fun trip_dDayText_formatsProperly() {
        val tripUpcoming = Trip(
            title = "부산 바캉스",
            startDate = "2026-08-01",
            endDate = "2026-08-03",
            dDay = -10
        )
        assertEquals("D-10", tripUpcoming.dDayText)

        val tripToday = Trip(
            title = "오늘 여행",
            startDate = "2026-10-28",
            endDate = "2026-10-30",
            dDay = 0
        )
        assertEquals("D-Day", tripToday.dDayText)
    }
}
