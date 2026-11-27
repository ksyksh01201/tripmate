package com.example.ui.screens

import java.util.UUID
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BalanceGame
import com.example.data.BalanceGameManager
import androidx.compose.runtime.rememberCoroutineScope
import com.example.data.BalanceGameTemplates
import com.example.data.GeminiHelper
import com.example.data.TripMemberSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CreateBalanceGameScreen(
    tripId: String = remember { "trip_${UUID.randomUUID()}" },
    userId: String = "test_user_1",
    tripTitle: String = "부산 바캉스🏖️",
    tripDateText: String = "2026년 8월 1일 ~ 8월 3일",
    otherMemberName: String = "김땡땡",
    onBack: () -> Unit = {},
    onNavigateToResult: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val manager = remember { BalanceGameManager() }
    val geminiHelper = remember { GeminiHelper() }
    val coroutineScope = rememberCoroutineScope()

    val gameListState = remember { mutableStateOf<List<BalanceGame>>(emptyList()) }
    val savedVoteMapState = remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val pendingOptionMapState = remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val currentIndexState = remember { mutableIntStateOf(0) }
    val isLoadingState = remember { mutableStateOf(true) }
    val isSavingState = remember { mutableStateOf(false) }
    val isFinishedState = remember { mutableStateOf(false) }
    val requestedVoteIds = remember { mutableSetOf<String>() }
    val seededState = remember { mutableStateOf(false) }

    // 실제 방 인원수 (DB에 방 문서가 없으면 기본 2명)
    val memberSummaryState = remember {
        mutableStateOf(
            TripMemberSummary(
                memberCount = 2,
                memberNames = listOf("홍길동", otherMemberName)
            )
        )
    }

    // 파이어베이스 데이터 실시간 연동
    DisposableEffect(tripId, userId) {
        requestedVoteIds.clear()
        savedVoteMapState.value = emptyMap()
        pendingOptionMapState.value = emptyMap()
        currentIndexState.intValue = 0
        isFinishedState.value = false
        seededState.value = false
        isLoadingState.value = true

        // 방 인원수 실시간 감지
        val tripListener = manager.listenToTripMembers(
            tripId = tripId,
            fallbackCount = 1,
            onUpdate = { summary ->
                val resolvedNames = if (summary.memberNames.isNotEmpty()) {
                    summary.memberNames
                } else {
                    List(summary.memberCount) { idx ->
                        when (idx) {
                            0 -> "홍길동"
                            1 -> otherMemberName
                            else -> "메이트${idx}"
                        }
                    }
                }
                memberSummaryState.value = summary.copy(memberNames = resolvedNames)
            }
        )

        val listener = manager.listenToGames(
            tripId = tripId,
            onUpdate = { games: List<BalanceGame> ->
                gameListState.value = games
                isLoadingState.value = false

                if (games.isEmpty() && !seededState.value) {
                    seededState.value = true
                    isLoadingState.value = true

                    manager.isSeeded(
                        tripId = tripId,
                        onResult = { alreadySeeded ->
                            if (!alreadySeeded) {
                                coroutineScope.launch {
                                    val questions = try {
                                        geminiHelper.getRandom5BalanceGames(tripTitle)
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "AI 에러: ${e.message}", Toast.LENGTH_LONG).show()
                                        BalanceGameTemplates.list
                                    }
                                    manager.seedQuestionsIfAbsent(
                                        tripId = tripId,
                                        templates = questions,
                                        onSuccess = { },
                                        onFailure = { e: Exception ->
                                            isLoadingState.value = false
                                            Toast.makeText(context, "질문 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                        },
                        onFailure = { e: Exception ->
                            isLoadingState.value = false
                            Toast.makeText(context, "확인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                    return@listenToGames
                }

                isLoadingState.value = false
                games.forEach { game ->
                    if (requestedVoteIds.add(game.gameId)) {
                        manager.getUserVote(
                            tripId = tripId,
                            gameId = game.gameId,
                            userId = userId,
                            onResult = { option: String? ->
                                if (option != null) {
                                    savedVoteMapState.value =
                                        savedVoteMapState.value + (game.gameId to option)
                                }
                            },
                            onFailure = { _: Exception ->
                                requestedVoteIds.remove(game.gameId)
                            }
                        )
                    }
                }
            },
            onError = { e: Exception ->
                isLoadingState.value = false
                Toast.makeText(context, "불러오기 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
        onDispose {
            tripListener.remove()
            listener.remove()
        }
    }

    val mainBlue = Color(0xFF3385FF)
    val lightOrangeBg = Color(0xFFFFF0EC)
    val borderBlue = Color(0xFF3385FF)
    val buttonOrange = Color(0xFFF66136)
    val vsCircleColor = Color(0xFF1E2849)

    val totalMemberCount = memberSummaryState.value.memberCount
    val memberNames = memberSummaryState.value.memberNames
    val avatarColors = listOf(
        Color(0xFF1E2849), buttonOrange, Color(0xFF22A055), Color(0xFF7B61FF), Color(0xFF009688)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .navigationBarsPadding()
    ) {
        // 상단 여행룸 헤더
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = mainBlue,
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "←",
                        color = Color.White,
                        fontSize = 22.sp,
                        modifier = Modifier.clickable { onBack() }
                    )
                    Text(
                        text = "tripmate",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "편집",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = tripTitle,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tripDateText,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val visibleCount = totalMemberCount.coerceAtMost(5)
                    for (idx in 0 until visibleCount) {
                        val initial = memberNames.getOrNull(idx)?.firstOrNull()?.toString() ?: "${idx + 1}"
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(avatarColors[idx % avatarColors.size], CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initial, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (totalMemberCount > 5) "+${totalMemberCount - 5}" else "+",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "현재 ${totalMemberCount}명",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
        }
        // 본문 영역
        val games = gameListState.value
        val answeredAll = games.isNotEmpty() && games.all { savedVoteMapState.value.containsKey(it.gameId) }
        val completedMemberCount = if (games.isEmpty()) {
            0
        } else {
            games.minOf { it.countA + it.countB }.coerceIn(0, totalMemberCount)
        }
        val allMembersFinished = games.isNotEmpty() && completedMemberCount >= totalMemberCount

        val mateProgressLabel = when {
            totalMemberCount <= 1 -> "혼자 여행 · 준비 완료"
            totalMemberCount == 2 -> {
                val partnerName = memberNames.firstOrNull { it != "홍길동" } ?: otherMemberName
                if (allMembersFinished) "$partnerName · 참여 완료" else "$partnerName · 진행중"
            }
            else -> {
                val otherTotal = totalMemberCount - 1
                val otherCompleted = (completedMemberCount - (if (answeredAll) 1 else 0)).coerceIn(0, otherTotal)
                if (allMembersFinished) {
                    "메이트 ${otherTotal}명 · 전원 완료"
                } else {
                    "메이트 $otherCompleted / ${otherTotal}명 완료"
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                isLoadingState.value -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = mainBlue)
                    }
                }

                games.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("등록된 밸런스 게임이 없습니다.")
                    }
                }

                // ✅ 피그마 완료 화면 (메이트 진행중 vs 전원 완료 자동 전환)
                games.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("등록된 밸런스 게임이 없습니다.")
                    }
                }

                isFinishedState.value -> {
                    BalanceGameCompletedCard(
                        totalCount = games.size,
                        mateBadgeLabel = mateProgressLabel,
                        isAllCompleted = allMembersFinished,
                        mainBlue = mainBlue,
                        buttonOrange = buttonOrange,
                        onRetryClick = {
                            currentIndexState.intValue = 0
                            isFinishedState.value = false
                        },
                        onViewResultClick = {
                            onNavigateToResult(tripId)
                        }
                    )
                }

                else -> {
                    val currentIndex = currentIndexState.intValue.coerceIn(0, games.size - 1)
                    val currentGame = games[currentIndex]
                    val savedVote = savedVoteMapState.value[currentGame.gameId]
                    val myVote = pendingOptionMapState.value[currentGame.gameId] ?: savedVote

                    val selectOption: (String) -> Unit = { option ->
                        if (!isSavingState.value) {
                            pendingOptionMapState.value =
                                pendingOptionMapState.value + (currentGame.gameId to option)
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("여행 밸런스 게임", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    Text("${currentIndex + 1} / ${games.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = mainBlue)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { (currentIndex + 1).toFloat() / games.size.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = mainBlue,
                                    trackColor = Color(0xFFE0E8F5)
                                )

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = currentGame.title,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        OptionCard(
                                            text = currentGame.optionA,
                                            selected = myVote == "A",
                                            selectedColor = Color(0xFFE8F1FF),
                                            selectedBorderColor = borderBlue,
                                            unselectedColor = lightOrangeBg,
                                            unselectedBorderWidth = 0.dp,
                                            unselectedBorderColor = Color.Transparent,
                                            onClick = { selectOption("A") }
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        OptionCard(
                                            text = currentGame.optionB,
                                            selected = myVote == "B",
                                            selectedColor = Color(0xFFE8F1FF),
                                            selectedBorderColor = borderBlue,
                                            unselectedColor = Color(0xFFF3F6FA),
                                            unselectedBorderWidth = 1.dp,
                                            unselectedBorderColor = borderBlue,
                                            onClick = { selectOption("B") }
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .background(vsCircleColor, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("VS", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Button(
                                    onClick = {
                                        val selectedOpt = myVote
                                        when {
                                            isSavingState.value -> Unit

                                            selectedOpt == null ->
                                                Toast.makeText(context, "옵션을 먼저 선택해주세요!", Toast.LENGTH_SHORT).show()

                                            else -> {
                                                isSavingState.value = true
                                                manager.vote(
                                                    tripId = tripId,
                                                    gameId = currentGame.gameId,
                                                    userId = userId,
                                                    selectedOption = selectedOpt,
                                                    onSuccess = {
                                                        isSavingState.value = false
                                                        savedVoteMapState.value =
                                                            savedVoteMapState.value + (currentGame.gameId to selectedOpt)
                                                        pendingOptionMapState.value =
                                                            pendingOptionMapState.value - currentGame.gameId

                                                        if (currentIndex < games.size - 1) {
                                                            currentIndexState.intValue = currentIndex + 1
                                                        } else {
                                                            isFinishedState.value = true
                                                        }
                                                    },
                                                    onFailure = { e ->
                                                        isSavingState.value = false
                                                        Toast.makeText(context, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }
                                        }
                                    },
                                    enabled = !isSavingState.value,
                                    colors = ButtonDefaults.buttonColors(containerColor = buttonOrange),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Text(
                                        text = if (isSavingState.value) "저장 중..." else "선택 완료",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                val myStatusText = if (answeredAll) "나 - 참여 완료" else "나 - 진행중"
                                Text(
                                    text = "$myStatusText  ·  $mateProgressLabel",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 피그마 디자인 기반: 밸런스 게임 완료 카드 (메이트 진행중 / 전원 완료 상태 전환) */
@Composable
private fun BalanceGameCompletedCard(
    totalCount: Int,
    mateBadgeLabel: String,
    isAllCompleted: Boolean,
    mainBlue: Color,
    buttonOrange: Color,
    onRetryClick: () -> Unit,
    onViewResultClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "여행 밸런스 게임",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF222222)
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = mainBlue, fontWeight = FontWeight.Bold)) {
                                append("$totalCount")
                            }
                            withStyle(SpanStyle(color = Color.Gray, fontWeight = FontWeight.Medium)) {
                                append(" / $totalCount")
                            }
                        },
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = buttonOrange,
                    trackColor = Color(0xFFFFE5DC)
                )

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = if (isAllCompleted) "밸런스 게임 완료! 🎉" else "밸런스 게임 완료!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1A1A1A)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isAllCompleted) "결과를 확인해 보세요." else "메이트가 아직 진행중이에요.",
                    fontSize = 14.sp,
                    color = Color(0xFF777777)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = if (isAllCompleted) onViewResultClick else onRetryClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAllCompleted) buttonOrange else mainBlue
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = if (isAllCompleted) "결과보기" else "다시 선택",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MemberStatusBadge(
                        modifier = Modifier.weight(1f),
                        label = "나 · 참여 완료",
                        isCompleted = true
                    )
                    MemberStatusBadge(
                        modifier = Modifier.weight(1f),
                        label = mateBadgeLabel,
                        isCompleted = isAllCompleted
                    )
                }
            }
        }
    }
}
@Composable
private fun MemberStatusBadge(
    modifier: Modifier = Modifier,
    label: String,
    isCompleted: Boolean
) {
    val bgColor = if (isCompleted) Color(0xFFEAF7EE) else Color(0xFFF3F4F6)
    val iconColor = if (isCompleted) Color(0xFF22A055) else Color(0xFF888888)

    Row(
        modifier = modifier
            .height(38.dp)
            .background(color = bgColor, shape = RoundedCornerShape(19.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF222222),
            maxLines = 1
        )
    }
}

@Composable
private fun OptionCard(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    selectedBorderColor: Color,
    unselectedColor: Color,
    unselectedBorderWidth: Dp,
    unselectedBorderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(
                color = if (selected) selectedColor else unselectedColor,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = if (selected) 2.dp else unselectedBorderWidth,
                color = if (selected) selectedBorderColor else unselectedBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2C2C2C),
            textAlign = TextAlign.Center
        )
    }
}