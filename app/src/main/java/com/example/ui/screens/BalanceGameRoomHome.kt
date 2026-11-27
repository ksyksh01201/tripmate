package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.BalanceGameManager
import com.example.data.Trip
import com.example.data.TripMemberNames
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.flowOf

/** 여행홈 탭(2명 이상)에서 보여 줄 단계: 밸런스 게임 → 궁합 결과 → 결과 후 여행홈 */
private enum class BalanceStage { Game, Result, Home }

/**
 * 팀원이 만든 밸런스 게임 화면들을 여행방에 연결합니다.
 * - Game  : CreateBalanceGameScreen (투표)
 * - Result: BalanceGameResultScreen (AI 궁합 분석 + 추천 장소)
 * - Home  : TripHomeScreen (결과 후 여행홈)
 * 이미 분석 결과가 저장된 여행이면 바로 Home 으로 시작해요.
 */
@Composable
internal fun BalanceGameRoomHome(
    trip: Trip,
    myUid: String?,
    onBack: () -> Unit,
    onSelectTab: (RoomTab) -> Unit
) {
    var stage by rememberSaveable(trip.tripId) { mutableStateOf(BalanceStage.Game) }
    val manager = remember { BalanceGameManager() }

    // 이미 결과가 있으면 결과 후 여행홈부터
    LaunchedEffect(trip.tripId) {
        manager.getCachedAnalysisResult(
            tripId = trip.tripId,
            onResult = { cached -> if (cached != null && stage == BalanceStage.Game) stage = BalanceStage.Home },
            onFailure = { }
        )
    }

    // 이름: 여행 문서의 memberNames (각자 여행방에 들어오면 저장됨)
    val names by remember(trip.tripId) {
        trip.tripId.takeIf { it.isNotBlank() }?.let { TripMemberNames.observe(it) } ?: flowOf(emptyMap())
    }.collectAsState(initial = emptyMap())
    val user = FirebaseAuth.getInstance().currentUser
    val myName = names[myUid]?.takeIf { it.isNotBlank() }
        ?: user?.displayName?.takeIf { it.isNotBlank() }
        ?: user?.email?.substringBefore("@")
        ?: "나"
    val otherUid = trip.members.firstOrNull { it != myUid }
    val otherName = otherUid?.let { names[it] }?.takeIf { it.isNotBlank() } ?: "메이트"
    val userId = myUid ?: user?.uid ?: "unknown"
    val dateText = balanceDateText(trip.startDate, trip.endDate)
    val destination = trip.destination.ifBlank { trip.title }

    when (stage) {
        BalanceStage.Game -> Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().weight(1f)) {
                CreateBalanceGameScreen(
                    tripId = trip.tripId,
                    userId = userId,
                    tripTitle = trip.title,
                    tripDateText = dateText,
                    otherMemberName = otherName,
                    onBack = onBack,
                    onNavigateToResult = { stage = BalanceStage.Result }
                )
            }
            TripRoomBottomBar(selected = RoomTab.Home, onSelect = onSelectTab)
        }

        BalanceStage.Result -> BalanceGameResultScreen(
            tripId = trip.tripId,
            userId = userId,
            tripTitle = trip.title,
            myName = myName,
            otherMemberName = otherName,
            onClose = { stage = BalanceStage.Home }
        )

        BalanceStage.Home -> TripHomeScreen(
            tripId = trip.tripId,
            destination = destination,
            tripTitle = trip.title,
            dateRange = dateText,
            initialMemberCount = trip.members.size,
            onBack = onBack,
            onOpenChemiResult = { stage = BalanceStage.Result },
            onRoomTabSelected = { index ->
                when (index) {
                    1 -> onSelectTab(RoomTab.Schedule)
                    2 -> onSelectTab(RoomTab.Budget)
                }
            }
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
