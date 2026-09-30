package com.example.model

data class TripMember(
    val id: String,
    val name: String,
    val isCurrentUser: Boolean = false,
    val isOwner: Boolean = false,
    val preferenceCompleted: Boolean = false,
    val preferenceAnswers: Map<Int, Boolean> = emptyMap(),
    val preferenceResult: PersonalPreferenceResult? = null
)

data class UserTripPreference(
    val tripId: String,
    val userId: String,
    val scores: List<CategoryScore> = emptyList(),
    val answers: Map<Int, Boolean> = emptyMap(),
    val completed: Boolean = false
)

data class Trip(
    val id: String,
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val expectedCount: Int,
    val inviteCode: String,
    val status: String = "취향 분석 중",
    val scheduleConfirmed: Boolean = false,
    val groupResultViewed: Boolean = false,
    val placesSelected: Boolean = false,
    val members: List<TripMember> = emptyList(),
    val personalResult: PersonalPreferenceResult? = null,
    val personalAnswers: Map<Int, Boolean> = emptyMap(),
    val selectedPlaces: List<Place> = emptyList(),
    val daySchedules: List<DaySchedule> = emptyList(),
    val expenses: List<Expense> = emptyList()
)

object MockDataProvider {
    fun getInitialTrips(): List<Trip> = emptyList()
}
