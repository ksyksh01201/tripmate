package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.data.BalanceGameManager
import com.example.data.RecommendedPlace
import com.example.data.Trip
import com.example.data.TripMemberNames
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.flowOf

/** 여행홈 탭(2명 이상)에서 보여 줄 단계: 밸런스 게임 흐름 → 결과 후 여행홈 */
private enum class BalanceStage { Game, Home }

/**
 * 팀원이 만든 밸런스 게임 화면들을 여행방에 연결합니다.
 * - Game: BalanceGameFlowScreen (질문 → 완료 → AI 분석 → 궁합 결과 → 추천 장소 고르기)
 * - Home: TripHomeScreen (결과 후 여행홈, 고른 장소 표시)
 * 이미 분석 결과가 저장된 여행이면 바로 Home 으로 시작해요.
 * 위쪽 헤더와 아래 탭은 여행방 공통 것을 씁니다.
 */
@Composable
internal fun BalanceGameRoomHome(
    trip: Trip,
    myUid: String?,
    onBack: () -> Unit,
    onSelectTab: (RoomTab) -> Unit,
    // 여행방과 같은 파란 헤더 (tripmate / 여행 이름 / 날짜 / 참여자 바)
    header: @Composable () -> Unit
) {
    var stage by rememberSaveable(trip.tripId) { mutableStateOf(BalanceStage.Game) }
    // 여행홈에서 "궁합 결과 보기"를 누르면 결과 화면부터 열기
    var openResult by rememberSaveable(trip.tripId) { mutableStateOf(false) }
    var chosenPlaces by remember(trip.tripId) { mutableStateOf<List<RecommendedPlace>>(emptyList()) }
    val manager = remember { BalanceGameManager() }

    // 이미 결과가 있으면 결과 후 여행홈부터
    LaunchedEffect(trip.tripId) {
        manager.getCachedAnalysisResult(
            tripId = trip.tripId,
            onResult = { cached ->
                if (cached != null && stage == BalanceStage.Game && !openResult) stage = BalanceStage.Home
            },
            onFailure = { }
        )
    }

    // 이름: 여행 문서의 memberNames (각자 여행방에 들어오면 저장됨). 나를 맨 앞에 "(나)"로
    val names by remember(trip.tripId) {
        trip.tripId.takeIf { it.isNotBlank() }?.let { TripMemberNames.observe(it) } ?: flowOf(emptyMap())
    }.collectAsState(initial = emptyMap())
    val user = FirebaseAuth.getInstance().currentUser
    val myName = names[myUid]?.takeIf { it.isNotBlank() }
        ?: user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore("@")
        ?: "나"
    val memberNames = buildList {
        add("$myName(나)")
        trip.members.filter { it != myUid }.forEachIndexed { i, uid ->
            add(names[uid]?.takeIf { it.isNotBlank() } ?: "멤버${i + 2}")
        }
    }
    val dateText = balanceDateText(trip.startDate, trip.endDate)
    val destination = trip.destination.ifBlank { trip.title }
    val roomBottomBar: @Composable () -> Unit = {
        TripRoomBottomBar(selected = RoomTab.Home, onSelect = onSelectTab)
    }

    when (stage) {
        BalanceStage.Game -> BalanceGameFlowScreen(
            tripId = trip.tripId,
            destination = destination,
            dateRangeText = dateText,
            initialMemberCount = trip.members.size,
            initialMemberNames = memberNames,
            startAtChemiResult = openResult,
            onBack = onBack,
            onExitToTripHome = {
                openResult = false
                stage = BalanceStage.Home
            },
            onPlacesRecommended = { places -> if (places.isNotEmpty()) chosenPlaces = places },
            onCreateScheduleWithPlaces = { places ->
                chosenPlaces = places
                openResult = false
                stage = BalanceStage.Home
            },
            header = header,
            bottomBar = roomBottomBar
        )

        BalanceStage.Home -> TripHomeScreen(
            tripId = trip.tripId,
            destination = destination,
            tripTitle = trip.title,
            dateRange = dateText,
            selectedSchedulePlaces = chosenPlaces,
            initialMemberCount = trip.members.size,
            onBack = onBack,
            onOpenChemiResult = {
                openResult = true
                stage = BalanceStage.Game
            },
            onRoomTabSelected = { index ->
                when (index) {
                    1 -> onSelectTab(RoomTab.Schedule)
                    2 -> onSelectTab(RoomTab.Budget)
                }
            },
            header = header
        )
    }
}

/** "2026-08-01", "2026-08-03" → "2026년 8월 1일 ~ 8월 3일" */
private fun balanceDateText(start: String, end: String): String {
    fun parts(d: String) = d.split("-").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 3 }
    val s = parts(start) ?: return start
    val e = parts(end) ?: return "${s[0]}년 ${s[1]}월 ${s[2]}일"
    val endText = if (e[0] == s[0]) "${e[1]}월 ${e[2]}일" else "${e[0]}년 ${e[1]}월 ${e[2]}일"
    return "${s[0]}년 ${s[1]}월 ${s[2]}일 ~ $endText"
}
