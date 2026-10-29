package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Trip
import com.example.data.repository.TripRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("TripMate", appName)
    }

    @Test
    fun `repository returns empty list initially and persists saved trips`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = TripRepository(context)
        val userEmail = "testuser@tripmate.com"

        // Clear existing
        repository.clearTrips(userEmail)

        // 1. Initial state must be empty
        val initialTrips = repository.getTrips(userEmail)
        assertTrue(initialTrips.isEmpty())

        // 2. Save a trip
        val trip1 = Trip(
            title = "제주도 여행",
            destination = "제주",
            startDate = "2026년 11월 1일",
            endDate = "2026년 11월 3일",
            memberCount = 2,
            dDay = -4
        )
        repository.saveTrip(userEmail, trip1)

        val tripsAfter1 = repository.getTrips(userEmail)
        assertEquals(1, tripsAfter1.size)
        assertEquals("제주도 여행", tripsAfter1[0].title)

        // 3. Save a second trip
        val trip2 = Trip(
            title = "부산 바캉스",
            destination = "부산",
            startDate = "2026년 12월 1일",
            endDate = "2026년 12월 3일",
            memberCount = 3,
            dDay = -34
        )
        repository.saveTrip(userEmail, trip2)

        val tripsAfter2 = repository.getTrips(userEmail)
        assertEquals(2, tripsAfter2.size)

        // Clean up
        repository.clearTrips(userEmail)
    }
}
