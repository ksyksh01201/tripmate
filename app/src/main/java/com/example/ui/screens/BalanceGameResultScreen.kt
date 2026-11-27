package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BalanceGame
import com.example.data.BalanceGameAnalysisResult
import com.example.data.BalanceGameManager
import com.example.data.GeminiHelper
import com.example.data.PlaceCriteria
import com.example.data.RecommendedPlace
import com.example.data.TripMemberSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun BalanceGameResultScreen(
    tripId: String,
    userId: String = "test_user_1",
    tripTitle: String = "부산 바캉스🏖️",
    myName: String = "홍길동",
    otherMemberName: String = "김땡땡",
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val manager = remember { BalanceGameManager() }
    val geminiHelper = remember { GeminiHelper() }
    val coroutineScope = rememberCoroutineScope()

    val isLoadingState = remember { mutableStateOf(true) }
    val errorMessageState = remember { mutableStateOf<String?>(null) }
    val gamesState = remember { mutableStateOf<List<BalanceGame>>(emptyList()) }
    val myVotesState = remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val analysisResultState = remember { mutableStateOf<BalanceGameAnalysisResult?>(null) }
    val triggerReloadState = remember { mutableIntStateOf(0) }
    val memberSummaryState = remember {
        mutableStateOf(
            TripMemberSummary(
                memberCount = 2,
                memberNames = listOf(myName, otherMemberName)
            )
        )
    }

    DisposableEffect(tripId) {
        val listener = manager.listenToTripMembers(
            tripId = tripId,
            fallbackCount = 1,
            onUpdate = { summary ->
                val resolvedNames = if (summary.memberNames.isNotEmpty()) {
                    summary.memberNames
                } else {
                    List(summary.memberCount) { idx ->
                        when (idx) {
                            0 -> myName
                            1 -> otherMemberName
                            else -> "메이트${idx}"
                        }
                    }
                }
                memberSummaryState.value = summary.copy(memberNames = resolvedNames)
            }
        )
        onDispose { listener.remove() }
    }

    LaunchedEffect(tripId, triggerReloadState.intValue) {
        isLoadingState.value = true
        errorMessageState.value = null

        manager.getGamesOnce(
            tripId = tripId,
            onSuccess = { games ->
                if (games.isEmpty()) {
                    isLoadingState.value = false
                    errorMessageState.value = "투표된 밸런스 게임 데이터가 없습니다."
                    return@getGamesOnce
                }

                gamesState.value = games

                games.forEach { game ->
                    manager.getUserVote(
                        tripId = tripId,
                        gameId = game.gameId,
                        userId = userId,
                        onResult = { option ->
                            if (option != null) {
                                myVotesState.value = myVotesState.value + (game.gameId to option)
                            }
                        },
                        onFailure = {}
                    )
                }

                manager.getCachedAnalysisResult(
                    tripId = tripId,
                    onResult = { cached ->
                        if (cached != null && cached.compromiseTips.isNotEmpty()) {
                            analysisResultState.value = cached
                            isLoadingState.value = false
                        } else {
                            coroutineScope.launch {
                                try {
                                    val generated = geminiHelper.getRecommendedPlaces(
                                        tripTitle = tripTitle,
                                        games = games
                                    )
                                    analysisResultState.value = generated
                                    isLoadingState.value = false
                                    manager.saveAnalysisResult(tripId = tripId, result = generated)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    if (cached != null) {
                                        analysisResultState.value = cached
                                        isLoadingState.value = false
                                    } else {
                                        isLoadingState.value = false
                                        errorMessageState.value = "AI 결과 분석 실패: ${e.message}"
                                    }
                                }
                            }
                        }
                    },
                    onFailure = { e ->
                        isLoadingState.value = false
                        errorMessageState.value = "결과 불러오기 실패: ${e.message}"
                    }
                )
            },
            onFailure = { e ->
                isLoadingState.value = false
                errorMessageState.value = "투표 데이터 조회 실패: ${e.message}"
            }
        )
    }

    val mainBlue = Color(0xFF2E7DF6)
    val buttonOrange = Color(0xFFF66136)
    val dimBgColor = Color(0xFF7B8494)

    val games = gamesState.value
    val memberCount = memberSummaryState.value.memberCount
    val memberNames = memberSummaryState.value.memberNames
    val partnerName = memberNames.firstOrNull { it != myName } ?: otherMemberName
    val isGroup = memberCount >= 3

    val pagerState = rememberPagerState(pageCount = { 2 })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dimBgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = if (pagerState.currentPage == 0) {
                                "메이트와의 케미 분석 결과"
                            } else {
                                "이런 곳 어떠세요?"
                            },
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (pagerState.currentPage == 0) {
                                "밸런스 게임 ${games.size.coerceAtLeast(10)}문항 기준"
                            } else {
                                "밸런스 게임 취향 반영 맞춤 추천"
                            },
                            fontSize = 12.sp,
                            color = Color(0xFF8E95A2),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color(0xFF444444),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { onClose() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when {
                    isLoadingState.value -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = mainBlue)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "AI가 케미와 맞춤 장소를 분석하고 있어요...",
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }

                    errorMessageState.value != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = errorMessageState.value ?: "오류가 발생했습니다.",
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { triggerReloadState.intValue += 1 },
                                    colors = ButtonDefaults.buttonColors(containerColor = mainBlue),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("다시 시도", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    else -> {
                        val analysis = analysisResultState.value

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) { page ->
                            if (page == 0) {
                                ChemiAnalysisPage(
                                    games = games,
                                    myVotes = myVotesState.value,
                                    isGroup = isGroup,
                                    memberCount = memberCount,
                                    partnerName = partnerName,
                                    analysis = analysis,
                                    mainBlue = mainBlue,
                                    buttonOrange = buttonOrange,
                                    onNextPageClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(1)
                                        }
                                    }
                                )
                            } else {
                                RecommendedPlacesPage(
                                    places = analysis?.places.orEmpty(),
                                    mainBlue = mainBlue,
                                    accentOrange = buttonOrange,
                                    onBackToAnalysisClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(0)
                                        }
                                    },
                                    onPlaceClick = { place ->
                                        Toast.makeText(
                                            context,
                                            "${place.name} - ${place.reason}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(2) { index ->
                                val isSelected = pagerState.currentPage == index
                                val indicatorWidth by animateDpAsState(
                                    targetValue = if (isSelected) 18.dp else 6.dp,
                                    label = "indicatorWidth"
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 3.dp)
                                        .height(5.dp)
                                        .width(indicatorWidth)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) mainBlue else Color(0xFFD5DCE6)
                                        )
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChemiAnalysisPage(
    games: List<BalanceGame>,
    myVotes: Map<String, String>,
    isGroup: Boolean,
    memberCount: Int,
    partnerName: String,
    analysis: BalanceGameAnalysisResult?,
    mainBlue: Color,
    buttonOrange: Color,
    onNextPageClick: () -> Unit
) {
    val votedGames = games.filter { (it.countA + it.countB) > 0 }
    val totalQuestions = votedGames.size.coerceAtLeast(1)

    val unanimousCount = votedGames.count { it.countA == 0 || it.countB == 0 }
    val splitCount = votedGames.count { it.countA > 0 && it.countB > 0 && it.countA == it.countB }
    val majorityCount = (votedGames.size - unanimousCount - splitCount).coerceAtLeast(0)

    val matchPercent = if (votedGames.isEmpty()) {
        50
    } else {
        ((unanimousCount.toFloat() / totalQuestions.toFloat()) * 100f).toInt()
    }

    val defaultSummary = if (isGroup) {
        "취향 통하는 곳은 확실하고,\n갈리는 곳은 조율 중!"
    } else {
        "통하는 취향은 확실하고,\n갈리는 곳은 타협 중!"
    }
    val headlineText = analysis?.summary?.takeIf { it.isNotBlank() } ?: defaultSummary

    val fallbackTips = listOf(
        "만장일치로 통일된 취향 장소는 일정의 핵심 코스로 먼저 넣어보세요.",
        "의견이 반반 갈린 테마는 오전·오후로 나누어 번갈아 방문하면 모두 만족해요."
    )
    val tips = analysis?.compromiseTips?.takeIf { it.isNotEmpty() } ?: fallbackTips

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF7B61FF), Color(0xFF5D46D6))
                    ),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .background(color = buttonOrange, shape = RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGroup) "단체 케미 타입" else "우리의 케미 타입",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = headlineText,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 23.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!isGroup) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${matchPercent}%",
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "장소 취향 일치도 ${votedGames.size}문항 중 ${unanimousCount}개 일치",
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GroupSummaryChip(
                            label = "만장일치 $unanimousCount",
                            bgColor = Color.White.copy(alpha = 0.22f)
                        )
                        GroupSummaryChip(
                            label = "다수결 $majorityCount",
                            bgColor = Color.White.copy(alpha = 0.22f)
                        )
                        GroupSummaryChip(
                            label = "대립 $splitCount",
                            bgColor = buttonOrange
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "문항별 결과",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )

        Spacer(modifier = Modifier.height(12.dp))

        val displayGames = votedGames
            .sortedBy { if (it.countA > 0 && it.countB > 0) 0 else 1 }
            .take(5)

        displayGames.forEachIndexed { idx, game ->
            val originalIndex = games.indexOfFirst { it.gameId == game.gameId }.takeIf { it >= 0 } ?: idx
            val (leftKeyword, rightKeyword) = extractShortKeywords(game, originalIndex)

            if (!isGroup) {
                TwoPersonQuestionRow(
                    leftKeyword = leftKeyword,
                    rightKeyword = rightKeyword,
                    game = game,
                    myVote = myVotes[game.gameId],
                    partnerName = partnerName,
                    mainBlue = mainBlue
                )
            } else {
                GroupQuestionRow(
                    leftKeyword = leftKeyword,
                    rightKeyword = rightKeyword,
                    game = game,
                    totalMembers = memberCount,
                    mainBlue = mainBlue,
                    buttonOrange = buttonOrange
                )
            }

            if (idx < displayGames.lastIndex) {
                HorizontalDivider(
                    color = Color(0xFFF1F3F6),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color(0xFFF3F6FB), shape = RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Text(
                text = "이렇게 타협해봐요",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = mainBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            tips.take(2).forEachIndexed { idx, tip ->
                Text(
                    text = "${idx + 1}. $tip",
                    fontSize = 12.sp,
                    color = Color(0xFF333A48),
                    lineHeight = 17.sp
                )
                if (idx == 0 && tips.size > 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNextPageClick,
            colors = ButtonDefaults.buttonColors(containerColor = mainBlue),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text(
                text = "우리에게 맞는 추천 장소 보기 →",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun GroupSummaryChip(label: String, bgColor: Color) {
    Box(
        modifier = Modifier
            .background(color = bgColor, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TwoPersonQuestionRow(
    leftKeyword: String,
    rightKeyword: String,
    game: BalanceGame,
    myVote: String?,
    partnerName: String,
    mainBlue: Color
) {
    val isAgreed = game.countA == 0 || game.countB == 0
    val agreedOnA = game.countA >= game.countB

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = leftKeyword,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(42.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            ) {
                drawLine(
                    color = Color(0xFFDCE2EC),
                    start = Offset(12.dp.toPx(), size.height / 2f),
                    end = Offset(size.width - 12.dp.toPx(), size.height / 2f),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            if (isAgreed) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (agreedOnA) Arrangement.Start else Arrangement.End
                ) {
                    PersonNode(name = "나", tint = mainBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    PersonNode(name = partnerName, tint = mainBlue)
                }
            } else {
                val leftName = if (myVote == "B") partnerName else "나"
                val rightName = if (myVote == "B") "나" else partnerName
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PersonNode(name = leftName, tint = mainBlue)
                    PersonNode(name = rightName, tint = mainBlue)
                }
            }
        }

        Text(
            text = rightKeyword,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF222222),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.width(42.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        StatusPillBadge(
            text = if (isAgreed) "일치" else "타협 필요",
            bgColor = if (isAgreed) Color(0xFFE7F7EC) else Color(0xFFFFECE8),
            textColor = if (isAgreed) Color(0xFF239B56) else Color(0xFFE0533C)
        )
    }
}

@Composable
private fun PersonNode(name: String, tint: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = name,
            fontSize = 9.sp,
            color = Color(0xFF7A828F),
            maxLines = 1
        )
    }
}

@Composable
private fun GroupQuestionRow(
    leftKeyword: String,
    rightKeyword: String,
    game: BalanceGame,
    totalMembers: Int,
    mainBlue: Color,
    buttonOrange: Color
) {
    val countA = game.countA
    val countB = game.countB
    val sum = (countA + countB).coerceAtLeast(1)

    val (badgeText, badgeBg, badgeColor) = when {
        countA == 0 || countB == 0 -> Triple("만장일치", Color(0xFFE7F7EC), Color(0xFF239B56))
        countA == countB -> Triple("대립", Color(0xFFFFECE8), Color(0xFFE0533C))
        else -> Triple("다수결", Color(0xFFEAF1FF), mainBlue)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$leftKeyword vs $rightKeyword",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
            StatusPillBadge(text = badgeText, bgColor = badgeBg, textColor = badgeColor)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp)),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (countA > 0) {
                Row(
                    modifier = Modifier
                        .weight(countA.toFloat() / sum.toFloat())
                        .fillMaxHeight()
                        .background(mainBlue)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = leftKeyword,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    repeat(countA.coerceAtMost(totalMembers.coerceAtMost(5))) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            if (countB > 0) {
                Row(
                    modifier = Modifier
                        .weight(countB.toFloat() / sum.toFloat())
                        .fillMaxHeight()
                        .background(buttonOrange)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    repeat(countB.coerceAtMost(totalMembers.coerceAtMost(5))) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = rightKeyword,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPillBadge(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .background(color = bgColor, shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1
        )
    }
}

private fun extractShortKeywords(game: BalanceGame, index: Int): Pair<String, String> {
    val presetKeywords = listOf(
        "핫플" to "숨은곳",
        "바다" to "숲길",
        "야외" to "실내",
        "맛집" to "로컬",
        "뷰카페" to "감성",
        "액티비티" to "힐링",
        "역사" to "쇼핑",
        "포토" to "풍경",
        "낮명소" to "야경",
        "평지" to "전망대"
    )
    if (index in presetKeywords.indices && index < PlaceCriteria.list.size) {
        return presetKeywords[index]
    }
    val left = game.optionA.trim().take(3).ifBlank { "A안" }
    val right = game.optionB.trim().take(3).ifBlank { "B안" }
    return left to right
}

/**
 * 2페이지: 2번 시안 파랑·초록 카드 스타일의 AI 맞춤 추천 장소 화면
 */
@Composable
private fun RecommendedPlacesPage(
    places: List<RecommendedPlace>,
    mainBlue: Color,
    accentOrange: Color,
    onBackToAnalysisClick: () -> Unit,
    onPlaceClick: (RecommendedPlace) -> Unit
) {
    val cardStyles = listOf(
        Pair(Color(0xFF68A5F8), Color(0xFF2563EB)),
        Pair(Color(0xFF7FD19C), Color(0xFF16A34A))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "이런 곳 어떠세요?",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = "${places.size}곳 추천",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = mainBlue
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "밸런스 게임 결과를 바탕으로 골라봤어요",
            fontSize = 12.sp,
            color = Color(0xFF888888)
        )

        Spacer(modifier = Modifier.height(14.dp))

        if (places.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(Color(0xFFF7F9FC), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "추천 장소 데이터가 없습니다.",
                    fontSize = 13.sp,
                    color = Color(0xFF777777)
                )
            }
        } else {
            val rows = places.chunked(2)
            rows.forEachIndexed { rowIndex, rowPlaces ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPlaces.forEachIndexed { colIndex, place ->
                        val styleIndex = rowIndex * 2 + colIndex
                        val (bgColor, badgeColor) = cardStyles[styleIndex % cardStyles.size]
                        PlaceGridCard(
                            place = place,
                            cardBgColor = bgColor,
                            badgeTextColor = badgeColor,
                            modifier = Modifier.weight(1f),
                            onClick = { onPlaceClick(place) }
                        )
                    }
                    if (rowPlaces.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                if (rowIndex < rows.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onBackToAnalysisClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F4F9)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Text(
                text = "← 케미 분석 결과 다시 보기",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333A48)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun PlaceGridCard(
    place: RecommendedPlace,
    cardBgColor: Color,
    badgeTextColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(178.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBgColor)
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Place,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = place.category.ifBlank { "추천" },
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = badgeTextColor,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = place.name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = place.reason,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.92f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp
        )
    }
}