package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import android.widget.Toast
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.RecommendedScheduleImporter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
    val tripId = trip.tripId
    val appContext = LocalContext.current.applicationContext
    // 다른 탭에 갔다 와도 유지되도록 앱이 켜져 있는 동안 기억 (BalanceGameMemory)
    val firstVisit = remember(tripId) { !BalanceGameMemory.has(tripId, "stage") }
    var stage by remember(tripId) { BalanceGameMemory.state(tripId, "stage") { BalanceStage.Game } }
    // 결과 화면부터 열기 (여행홈의 "궁합 결과 보기", 또는 저장된 결과가 있을 때)
    var openResult by remember(tripId) { BalanceGameMemory.state(tripId, "openResult") { false } }
    var chosenPlaces by remember(tripId) {
        BalanceGameMemory.state<List<RecommendedPlace>>(tripId, "chosenPlaces") { emptyList() }
    }
    // 처음 들어왔을 때만 저장된 결과를 확인하고, 확인될 때까지는 헤더만 보여 줌
    var ready by remember(tripId) { mutableStateOf(!firstVisit) }
    val manager = remember { BalanceGameManager() }

    // 앱을 새로 켠 뒤 처음 들어왔는데 이미 결과가 저장돼 있으면 결과 화면부터
    LaunchedEffect(tripId) {
        if (!firstVisit) return@LaunchedEffect
        manager.getCachedAnalysisResult(
            tripId = tripId,
            onResult = { cached ->
                if (cached != null && stage == BalanceStage.Game) openResult = true
                ready = true
            },
            onFailure = { ready = true }
        )
    }
    if (!ready) {
        Column(Modifier.fillMaxSize().background(Color(0xFFF3F5F8))) { header() }
        return
    }
    // 결과 화면을 연 뒤에는 표시를 끔 (그 뒤로는 게임 화면이 보던 단계를 그대로 기억)
    LaunchedEffect(openResult) {
        if (openResult) {
            kotlinx.coroutines.delay(500)
            openResult = false
        }
    }
    // 밸런스 게임 화면은 안쪽 여백이 있어서, 헤더를 가장자리까지 꽉 차게
    val fullHeader: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth().bleed(horizontal = 16.dp, top = 12.dp)) { header() }
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
            // 각자 자기 계정에서만 고르기 (나 먼저, 나머지는 memberNames 와 같은 순서)
            memberUids = myUid?.let { me -> listOf(me) + trip.members.filter { it != me } },
            startAtChemiResult = openResult,
            onBack = onBack,
            onExitToTripHome = {
                openResult = false
                stage = BalanceStage.Home
            },
            onPlacesRecommended = { places -> if (places.isNotEmpty()) chosenPlaces = places },
            onCreateScheduleWithPlaces = { places ->
                chosenPlaces = places
                // 고른 장소를 일정 탭에도 넣기
                RecommendedScheduleImporter.import(trip, places) { added ->
                    val msg = if (added > 0) "일정 탭에 ${added}곳을 넣었어요" else "이미 일정에 있는 장소예요"
                    Toast.makeText(appContext, msg, Toast.LENGTH_SHORT).show()
                }
                openResult = false
                stage = BalanceStage.Home
            },
            header = fullHeader,
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
            header = header,
            bottomBar = roomBottomBar
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
