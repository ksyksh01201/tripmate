package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.model.*
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen {
    LOGIN,
    HOME,
    CREATE_TRIP,
    TRIP_DETAIL,
    BALANCE_GAME,
    PERSONAL_RESULT,
    GROUP_ANALYSIS,
    RECOMMENDED_PLACES,
    SELECT_PLACES,
    SCHEDULE_PLAN,
    FULL_SCHEDULE,
    MAP_ROUTE,
    EXPENSE_TRIP_SELECT,
    SCHEDULE_TRIP_SELECT,
    EXPENSES,
    ADD_EXPENSE,
    MY_PAGE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                TripMateApp()
            }
        }
    }
}

@Composable
fun TripMateApp() {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.LOGIN) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var selectedTrip by remember { mutableStateOf<Trip?>(null) }
    var userEmail by remember { mutableStateOf("") }
    var expenseEntrySource by remember { mutableStateOf(AppScreen.HOME) }
    var selectedScheduleTripId by remember { mutableStateOf<String?>(null) }
    var scheduleEntrySource by remember { mutableStateOf(AppScreen.HOME) }
    var selectedDayForMap by remember { mutableIntStateOf(0) }

    // Helper to navigate to Schedule based on trip count
    val navigateToSchedule: () -> Unit = {
        if (trips.isEmpty()) {
            scheduleEntrySource = AppScreen.HOME
            currentScreen = AppScreen.SCHEDULE_TRIP_SELECT
        } else if (trips.size == 1) {
            val singleTrip = trips[0]
            selectedTrip = singleTrip
            selectedScheduleTripId = singleTrip.id
            scheduleEntrySource = AppScreen.HOME
            currentScreen = AppScreen.FULL_SCHEDULE
        } else {
            selectedScheduleTripId = null
            scheduleEntrySource = AppScreen.SCHEDULE_TRIP_SELECT
            currentScreen = AppScreen.SCHEDULE_TRIP_SELECT
        }
    }

    // Helper to update current selected trip and synchronize trips list
    fun updateCurrentTrip(updated: Trip) {
        selectedTrip = updated
        trips = trips.map { if (it.id == updated.id) updated else it }
    }

    // Dynamic Group Result for currently selected trip
    val groupResult = remember(selectedTrip?.id, selectedTrip?.personalResult, selectedTrip?.members) {
        val trip = selectedTrip
        if (trip == null) {
            GroupPreferenceResult(
                summary = "아직 완료된 취향 결과가 없어요.",
                topCategories = emptyList(),
                consensusList = emptyList(),
                conflictList = emptyList(),
                completedMemberCount = 0
            )
        } else {
            val completedMembers = trip.members.filter { it.preferenceCompleted }.map { member ->
                if (member.isCurrentUser && member.preferenceResult == null && trip.personalResult != null) {
                    member.copy(
                        preferenceCompleted = true,
                        preferenceResult = trip.personalResult,
                        preferenceAnswers = trip.personalAnswers
                    )
                } else member
            }.filter { it.preferenceResult != null }

            if (completedMembers.isNotEmpty()) {
                PreferenceQuestions.calculateGroupResult(completedMembers)
            } else if (trip.personalResult != null) {
                val selfMember = TripMember(
                    id = "user_me",
                    name = "나",
                    isCurrentUser = true,
                    preferenceCompleted = true,
                    preferenceResult = trip.personalResult,
                    preferenceAnswers = trip.personalAnswers
                )
                PreferenceQuestions.calculateGroupResult(listOf(selfMember))
            } else {
                GroupPreferenceResult(
                    summary = "아직 완료된 취향 결과가 없어요.",
                    topCategories = emptyList(),
                    consensusList = emptyList(),
                    conflictList = emptyList(),
                    completedMemberCount = 0
                )
            }
        }
    }

    // Dynamic Places list for current trip's destination and group result
    val places = remember(selectedTrip?.id, selectedTrip?.destination, groupResult, selectedTrip?.selectedPlaces) {
        val scores = groupResult.topCategories.associate { it.category to it.percentage }
        val base = PlaceDataProvider.getInitialPlaces(selectedTrip?.destination ?: "부산", scores)
        val selectedIds = selectedTrip?.selectedPlaces?.map { it.id }?.toSet() ?: emptySet()
        base.map { if (it.id in selectedIds) it.copy(isSelected = true) else it.copy(isSelected = false) }
    }

    // Android System Back Button Handler
    BackHandler(enabled = currentScreen != AppScreen.LOGIN) {
        when (currentScreen) {
            AppScreen.CREATE_TRIP -> currentScreen = AppScreen.HOME
            AppScreen.TRIP_DETAIL -> currentScreen = AppScreen.HOME
            AppScreen.BALANCE_GAME -> currentScreen = AppScreen.TRIP_DETAIL
            AppScreen.PERSONAL_RESULT -> currentScreen = AppScreen.TRIP_DETAIL
            AppScreen.GROUP_ANALYSIS -> currentScreen = AppScreen.TRIP_DETAIL
            AppScreen.RECOMMENDED_PLACES -> currentScreen = AppScreen.GROUP_ANALYSIS
            AppScreen.SELECT_PLACES -> currentScreen = AppScreen.TRIP_DETAIL
            AppScreen.SCHEDULE_PLAN -> currentScreen = AppScreen.TRIP_DETAIL
            AppScreen.FULL_SCHEDULE -> {
                currentScreen = if (scheduleEntrySource == AppScreen.SCHEDULE_TRIP_SELECT) {
                    AppScreen.SCHEDULE_TRIP_SELECT
                } else {
                    AppScreen.TRIP_DETAIL
                }
            }
            AppScreen.MAP_ROUTE -> {
                val activeTrip = trips.find { it.id == selectedScheduleTripId } ?: selectedTrip
                currentScreen = if (activeTrip?.scheduleConfirmed == true) AppScreen.FULL_SCHEDULE else AppScreen.SCHEDULE_PLAN
            }
            AppScreen.SCHEDULE_TRIP_SELECT -> currentScreen = AppScreen.HOME
            AppScreen.EXPENSE_TRIP_SELECT -> currentScreen = AppScreen.HOME
            AppScreen.EXPENSES -> {
                currentScreen = if (expenseEntrySource == AppScreen.EXPENSE_TRIP_SELECT) AppScreen.EXPENSE_TRIP_SELECT else AppScreen.TRIP_DETAIL
            }
            AppScreen.ADD_EXPENSE -> currentScreen = AppScreen.EXPENSES
            AppScreen.MY_PAGE -> currentScreen = AppScreen.HOME
            AppScreen.HOME -> currentScreen = AppScreen.LOGIN
            AppScreen.LOGIN -> { /* Exit */ }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            AppScreen.LOGIN -> {
                LoginScreen(
                    onLoginSuccess = { email ->
                        userEmail = email
                        currentScreen = AppScreen.HOME
                    }
                )
            }

            AppScreen.HOME -> {
                HomeScreen(
                    trips = trips,
                    onTripClick = { trip ->
                        selectedTrip = trip
                        currentScreen = AppScreen.TRIP_DETAIL
                    },
                    onCreateTripClick = {
                        currentScreen = AppScreen.CREATE_TRIP
                    },
                    onJoinByCode = { code ->
                        val trimmedCode = code.trim()
                        val targetTrip = trips.find { it.inviteCode.equals(trimmedCode, ignoreCase = true) }

                        // [8. 잘못된 코드]
                        if (targetTrip == null) {
                            Toast.makeText(context, "존재하지 않는 초대코드입니다.", Toast.LENGTH_SHORT).show()
                            return@HomeScreen
                        }

                        // [7. 중복 참여 방지]
                        if (targetTrip.members.any { it.id == "user_joined" }) {
                            Toast.makeText(context, "이미 참여 중인 여행입니다.", Toast.LENGTH_SHORT).show()
                            return@HomeScreen
                        }

                        // [9. 인원 제한]
                        if (targetTrip.members.size >= targetTrip.expectedCount) {
                            Toast.makeText(context, "참여 가능한 인원이 모두 찼습니다.", Toast.LENGTH_SHORT).show()
                            return@HomeScreen
                        }

                        // [4. 정상적인 코드 입력 & 6. 실제 참여자]
                        val newMember = TripMember(
                            id = "user_joined",
                            name = "사용자B",
                            isCurrentUser = false,
                            isOwner = false,
                            preferenceCompleted = false
                        )
                        val updatedTrip = targetTrip.copy(
                            members = targetTrip.members + newMember
                        )
                        updateCurrentTrip(updatedTrip)
                        currentScreen = AppScreen.TRIP_DETAIL
                        Toast.makeText(context, "'${targetTrip.title}' 여행에 참여했습니다!", Toast.LENGTH_SHORT).show()
                    },
                    onViewScheduleClick = { _ ->
                        navigateToSchedule()
                    },
                    onExpenseTabClick = {
                        currentScreen = AppScreen.EXPENSE_TRIP_SELECT
                    },
                    onMyPageClick = {
                        currentScreen = AppScreen.MY_PAGE
                    },
                    onLogoutClick = {
                        currentScreen = AppScreen.LOGIN
                        Toast.makeText(context, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            AppScreen.CREATE_TRIP -> {
                CreateTripScreen(
                    onBackClick = {
                        currentScreen = AppScreen.HOME
                    },
                    onTripCreated = { newTrip ->
                        trips = listOf(newTrip) + trips
                        selectedTrip = newTrip
                        currentScreen = AppScreen.TRIP_DETAIL
                        Toast.makeText(context, "새로운 여행방이 생성되었습니다!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            AppScreen.TRIP_DETAIL -> {
                selectedTrip?.let { trip ->
                    TripDetailScreen(
                        trip = trip,
                        onBackClick = {
                            currentScreen = AppScreen.HOME
                        },
                        onStartPreferenceGame = {
                            currentScreen = AppScreen.BALANCE_GAME
                        },
                        onViewGroupPreference = {
                            currentScreen = AppScreen.GROUP_ANALYSIS
                        },
                        onSelectPlaces = {
                            currentScreen = AppScreen.SELECT_PLACES
                        },
                        onViewSchedule = {
                            selectedScheduleTripId = trip.id
                            scheduleEntrySource = AppScreen.TRIP_DETAIL
                            if (trip.scheduleConfirmed) {
                                currentScreen = AppScreen.FULL_SCHEDULE
                            } else {
                                currentScreen = AppScreen.SCHEDULE_PLAN
                            }
                        },
                        onViewExpense = {
                            expenseEntrySource = AppScreen.TRIP_DETAIL
                            currentScreen = AppScreen.EXPENSES
                        }
                    )
                } ?: run {
                    currentScreen = AppScreen.HOME
                }
            }

            AppScreen.BALANCE_GAME -> {
                BalanceGameScreen(
                    onBackClick = {
                        currentScreen = AppScreen.TRIP_DETAIL
                    },
                    onGameComplete = { answers ->
                        val result = PreferenceQuestions.calculatePersonalResult(answers)

                        // Update current trip's preferences and current user's status
                        selectedTrip?.let { currentTrip ->
                            val updatedMembers = currentTrip.members.map { member ->
                                if (member.isCurrentUser) member.copy(
                                    preferenceCompleted = true,
                                    preferenceResult = result,
                                    preferenceAnswers = answers
                                ) else member
                            }
                            val updatedTrip = currentTrip.copy(
                                personalResult = result,
                                personalAnswers = answers,
                                members = updatedMembers,
                                status = if (updatedMembers.all { it.preferenceCompleted }) "일정 조율 중" else currentTrip.status
                            )
                            updateCurrentTrip(updatedTrip)
                        }

                        Toast.makeText(context, "여행 취향 조사가 완료되었습니다!", Toast.LENGTH_SHORT).show()
                        currentScreen = AppScreen.PERSONAL_RESULT
                    }
                )
            }

            AppScreen.PERSONAL_RESULT -> {
                val result = selectedTrip?.personalResult ?: PreferenceQuestions.calculatePersonalResult(emptyMap())
                PersonalResultScreen(
                    result = result,
                    onReturnToTrip = {
                        currentScreen = AppScreen.TRIP_DETAIL
                    },
                    onViewGroupPreference = {
                        selectedTrip?.let { currentTrip ->
                            val updatedTrip = currentTrip.copy(groupResultViewed = true)
                            updateCurrentTrip(updatedTrip)
                        }
                        currentScreen = AppScreen.GROUP_ANALYSIS
                    }
                )
            }

            AppScreen.GROUP_ANALYSIS -> {
                LaunchedEffect(selectedTrip?.id) {
                    selectedTrip?.let { currentTrip ->
                        if (!currentTrip.groupResultViewed) {
                            val updatedTrip = currentTrip.copy(groupResultViewed = true)
                            updateCurrentTrip(updatedTrip)
                        }
                    }
                }
                GroupAnalysisScreen(
                    groupResult = groupResult,
                    onBackClick = {
                        currentScreen = AppScreen.TRIP_DETAIL
                    },
                    onFindPlacesClick = {
                        selectedTrip?.let { currentTrip ->
                            val updatedTrip = currentTrip.copy(groupResultViewed = true)
                            updateCurrentTrip(updatedTrip)
                        }
                        currentScreen = AppScreen.SELECT_PLACES
                    }
                )
            }

            AppScreen.RECOMMENDED_PLACES -> {
                RecommendedPlacesScreen(
                    places = places,
                    onBackClick = {
                        currentScreen = AppScreen.GROUP_ANALYSIS
                    },
                    onProceedToSelectPlaces = {
                        currentScreen = AppScreen.SELECT_PLACES
                    }
                )
            }

            AppScreen.SELECT_PLACES -> {
                SelectPlacesScreen(
                    places = places,
                    onBackClick = {
                        currentScreen = AppScreen.TRIP_DETAIL
                    },
                    onConfirmedSelection = { selected ->
                        selectedTrip?.let { currentTrip ->
                            val emptyDays = currentTrip.daySchedules.ifEmpty {
                                PlaceDataProvider.generateDays(currentTrip.startDate, currentTrip.endDate)
                            }
                            val updatedTrip = currentTrip.copy(
                                placesSelected = true,
                                selectedPlaces = selected,
                                daySchedules = emptyDays
                            )
                            updateCurrentTrip(updatedTrip)
                        }
                        currentScreen = AppScreen.TRIP_DETAIL
                    }
                )
            }

            AppScreen.SCHEDULE_PLAN -> {
                selectedTrip?.let { currentTrip ->
                    SchedulePlanScreen(
                        tripId = currentTrip.id,
                        initialSchedules = currentTrip.daySchedules,
                        selectedPlaces = currentTrip.selectedPlaces,
                        startDate = currentTrip.startDate,
                        endDate = currentTrip.endDate,
                        onBackClick = {
                            currentScreen = AppScreen.TRIP_DETAIL
                        },
                        onScheduleConfirmed = { confirmedDaySchedules ->
                            val updatedTrip = currentTrip.copy(
                                scheduleConfirmed = true,
                                status = "✓ 여행 준비 완료",
                                daySchedules = confirmedDaySchedules
                            )
                            updateCurrentTrip(updatedTrip)
                            selectedScheduleTripId = currentTrip.id
                        },
                        onViewFullSchedule = {
                            selectedScheduleTripId = currentTrip.id
                            scheduleEntrySource = AppScreen.TRIP_DETAIL
                            currentScreen = AppScreen.FULL_SCHEDULE
                        },
                        onViewMapRoute = {
                            selectedDayForMap = 0
                            currentScreen = AppScreen.MAP_ROUTE
                        }
                    )
                } ?: run {
                    currentScreen = AppScreen.HOME
                }
            }

            AppScreen.FULL_SCHEDULE -> {
                val activeTrip = trips.find { it.id == selectedScheduleTripId } ?: selectedTrip
                activeTrip?.let { trip ->
                    val currentTripId = trip.id
                    val baseDays = if (trip.daySchedules.isNotEmpty()) {
                        trip.daySchedules
                    } else {
                        PlaceDataProvider.generateDays(trip.startDate, trip.endDate)
                    }
                    val tripSpecificDays = baseDays.map { day ->
                        day.copy(items = day.items.filter { it.tripId.isEmpty() || it.tripId == currentTripId })
                    }
                    FullScheduleScreen(
                        trip = trip,
                        daySchedules = tripSpecificDays,
                        onBackClick = {
                            currentScreen = if (scheduleEntrySource == AppScreen.SCHEDULE_TRIP_SELECT) {
                                AppScreen.SCHEDULE_TRIP_SELECT
                            } else {
                                AppScreen.TRIP_DETAIL
                            }
                        },
                        onViewMapRoute = { selectedDayIndex ->
                            selectedDayForMap = selectedDayIndex
                            currentScreen = AppScreen.MAP_ROUTE
                        },
                        onViewExpense = {
                            selectedTrip = trip
                            expenseEntrySource = AppScreen.TRIP_DETAIL
                            currentScreen = AppScreen.EXPENSES
                        }
                    )
                } ?: run {
                    currentScreen = AppScreen.HOME
                }
            }

            AppScreen.MAP_ROUTE -> {
                val activeTrip = trips.find { it.id == selectedScheduleTripId } ?: selectedTrip
                val currentTripId = activeTrip?.id ?: ""
                val baseDays = if (activeTrip?.daySchedules?.isNotEmpty() == true) {
                    activeTrip.daySchedules
                } else {
                    PlaceDataProvider.generateDays(activeTrip?.startDate ?: "", activeTrip?.endDate ?: "")
                }
                val tripSpecificDays = baseDays.map { day ->
                    day.copy(items = day.items.filter { it.tripId.isEmpty() || it.tripId == currentTripId })
                }
                MapRouteScreen(
                    tripTitle = activeTrip?.title.orEmpty().ifBlank { "여행" },
                    daySchedules = tripSpecificDays,
                    initialDayIndex = selectedDayForMap,
                    onBackClick = {
                        currentScreen = if (activeTrip?.scheduleConfirmed == true) AppScreen.FULL_SCHEDULE else AppScreen.SCHEDULE_PLAN
                    }
                )
            }

            AppScreen.SCHEDULE_TRIP_SELECT -> {
                TripScheduleSelectScreen(
                    trips = trips,
                    onSelectTrip = { trip ->
                        selectedTrip = trip
                        selectedScheduleTripId = trip.id
                        scheduleEntrySource = AppScreen.SCHEDULE_TRIP_SELECT
                        currentScreen = AppScreen.FULL_SCHEDULE
                    },
                    onCreateTripClick = {
                        currentScreen = AppScreen.CREATE_TRIP
                    },
                    onHomeTabClick = {
                        currentScreen = AppScreen.HOME
                    },
                    onExpenseTabClick = {
                        currentScreen = AppScreen.EXPENSE_TRIP_SELECT
                    },
                    onMyPageTabClick = {
                        currentScreen = AppScreen.MY_PAGE
                    }
                )
            }

            AppScreen.EXPENSE_TRIP_SELECT -> {
                TripExpenseSelectScreen(
                    trips = trips,
                    onSelectTrip = { trip ->
                        selectedTrip = trip
                        expenseEntrySource = AppScreen.EXPENSE_TRIP_SELECT
                        currentScreen = AppScreen.EXPENSES
                    },
                    onCreateTripClick = {
                        currentScreen = AppScreen.CREATE_TRIP
                    },
                    onHomeTabClick = {
                        currentScreen = AppScreen.HOME
                    },
                    onScheduleTabClick = {
                        navigateToSchedule()
                    },
                    onMyPageTabClick = {
                        currentScreen = AppScreen.MY_PAGE
                    }
                )
            }

            AppScreen.EXPENSES -> {
                selectedTrip?.let { trip ->
                    ExpenseScreen(
                        trip = trip,
                        expenses = trip.expenses,
                        onBackClick = {
                            currentScreen = if (expenseEntrySource == AppScreen.EXPENSE_TRIP_SELECT) {
                                AppScreen.EXPENSE_TRIP_SELECT
                            } else {
                                AppScreen.TRIP_DETAIL
                            }
                        },
                        onAddExpenseClick = {
                            currentScreen = AppScreen.ADD_EXPENSE
                        },
                        onDeleteExpense = { exp ->
                            val updatedExpenses = trip.expenses.filter { it.expenseId != exp.expenseId }
                            val updatedTrip = trip.copy(expenses = updatedExpenses)
                            updateCurrentTrip(updatedTrip)
                            Toast.makeText(context, "지출 내역이 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        },
                        onUpdateExpense = { updatedExp ->
                            val updatedExpenses = trip.expenses.map {
                                if (it.expenseId == updatedExp.expenseId) updatedExp else it
                            }
                            val updatedTrip = trip.copy(expenses = updatedExpenses)
                            updateCurrentTrip(updatedTrip)
                            Toast.makeText(context, "지출 내역이 수정되었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    )
                } ?: run {
                    currentScreen = AppScreen.HOME
                }
            }

            AppScreen.ADD_EXPENSE -> {
                selectedTrip?.let { trip ->
                    AddExpenseScreen(
                        tripId = trip.id,
                        members = trip.members,
                        onBackClick = {
                            currentScreen = AppScreen.EXPENSES
                        },
                        onSaveExpense = { newExpense ->
                            val updatedExpenses = listOf(newExpense) + trip.expenses
                            val updatedTrip = trip.copy(expenses = updatedExpenses)
                            updateCurrentTrip(updatedTrip)
                            currentScreen = AppScreen.EXPENSES
                            Toast.makeText(context, "새로운 지출이 등록되었습니다!", Toast.LENGTH_SHORT).show()
                        }
                    )
                } ?: run {
                    currentScreen = AppScreen.HOME
                }
            }

            AppScreen.MY_PAGE -> {
                MyPageScreen(
                    userEmail = userEmail,
                    onBackClick = {
                        currentScreen = AppScreen.HOME
                    },
                    onLogoutClick = {
                        userEmail = ""
                        currentScreen = AppScreen.LOGIN
                        Toast.makeText(context, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}
