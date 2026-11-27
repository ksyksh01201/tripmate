package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.BalanceGame
import com.example.data.BalanceGameAnalysisResult
import com.example.data.BalanceGameManager
import com.example.data.BalanceGameTemplates
import com.example.data.GameTemplate
import com.example.data.GeminiHelper
import com.example.data.RecommendedPlace
import kotlinx.coroutines.launch

private enum class GameFlowStep {
    QUESTIONS,
    COMPLETED,
    ANALYZING,
    CHEMI_RESULT,
    RECOMMENDED_PLACES
}

private data class AxisComparisonItem(
    val leftLabel: String,
    val rightLabel: String,
    val leftVotes: List<String>,
    val rightVotes: List<String>,
    val statusLabel: String,
    val hashtag: String
)

private data class ChemiComputedReport(
    val memberCount: Int,
    val memberNames: List<String>,
    val headline: String,
    val subBadge: String,
    val summaryHashtags: List<String>,
    val axes: List<AxisComparisonItem>,
    val tips: List<String>,
    val places: List<RecommendedPlace>
)

@Composable
fun BalanceGameFlowScreen(
    tripId: String,
    destination: String = "부산",
    dateRangeText: String = "2026년 8월 1일 ~ 8월 3일",
    initialMemberCount: Int? = null,
    initialMemberNames: List<String> = listOf("홍길동(나)", "김명명", "이민수", "박지훈"),
    onMemberCountChanged: (Int) -> Unit = {},
    startAtChemiResult: Boolean = false,
    onBack: () -> Unit,
    onExitToTripHome: (() -> Unit)? = null,
    onPlacesRecommended: (List<RecommendedPlace>) -> Unit = {},
    onCreateScheduleWithPlaces: (List<RecommendedPlace>) -> Unit = {},
    // 여행방에서 쓸 때: 위쪽 헤더와 아래 탭을 여행방 것으로 바꿔 끼움
    header: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null
) {
    val mainBlue = Color(0xFF0055D4)
    val headerBlue = Color(0xFF4380DF)
    val softBg = Color(0xFFF3F5F8)
    val coralOrange = Color(0xFFFF6E40)
    val coroutineScope = rememberCoroutineScope()
    val handleCloseResult: () -> Unit = {
        if (onExitToTripHome != null) {
            onExitToTripHome()
        } else {
            onBack()
        }
    }

    val resolvedInitialCount = (initialMemberCount ?: initialMemberNames.size).coerceIn(2, 4)

    var selectedMemberCount by remember(tripId) { BalanceGameMemory.state(tripId, "memberCount") {
        resolvedInitialCount }
    }

    LaunchedEffect(initialMemberCount) {
        if (initialMemberCount != null) {
            val coerced = initialMemberCount.coerceIn(2, 4)
            if (selectedMemberCount != coerced) {
                selectedMemberCount = coerced
            }
        }
    }

    var activeMemberIndex by remember(tripId) { BalanceGameMemory.state(tripId, "activeMember") { 0 } }
    var currentQuestionIndex by remember(tripId) { BalanceGameMemory.state(tripId, "questionIndex") { 0 } }
    var flowStep by remember(tripId) { BalanceGameMemory.state(tripId, "flowStep") { if (startAtChemiResult) GameFlowStep.CHEMI_RESULT else GameFlowStep.QUESTIONS } }
    // 분석 도중 다른 탭으로 갔다 오면 분석이 멈추므로 완료 화면으로 되돌림
    LaunchedEffect(tripId) { if (flowStep == GameFlowStep.ANALYZING) flowStep = GameFlowStep.COMPLETED }

    val members = remember(selectedMemberCount, initialMemberNames) {
        val defaultLabels = listOf("홍길동(나)", "김명명", "이민수", "박지훈")
        (0 until selectedMemberCount).map { idx ->
            initialMemberNames.getOrNull(idx)?.takeIf { it.isNotBlank() } ?: defaultLabels[idx]
        }
    }

    val answersByMember = remember(tripId) { BalanceGameMemory.obj(tripId, "answers") {
        mutableStateMapOf<Int, MutableMap<Int, String>>().apply {
            for (m in 0 until 4) {
                put(m, mutableStateMapOf())
            }
        }
    } }

    val fallbackQuestions = remember {
        BalanceGameTemplates.list.take(5).mapIndexed { idx, item -> item.copy(id = idx + 1) }
    }
    var questions by remember(tripId) { BalanceGameMemory.state(tripId, "questions") { fallbackQuestions } }
    var isLoadingQuestions by remember { mutableStateOf(false) }
    var reloadNonce by remember(tripId) { BalanceGameMemory.state(tripId, "reloadNonce") { 0 } }
    var lastLoadedDestination by remember(tripId) { BalanceGameMemory.state<String?>(tripId, "lastDestination") { null } }
    var lastHandledReloadNonce by remember(tripId) { BalanceGameMemory.state(tripId, "handledNonce") { 0 } }
    var computedReport by remember(tripId) { BalanceGameMemory.state<ChemiComputedReport?>(tripId, "report") { null } }

    val geminiHelper = remember { GeminiHelper() }
    val manager = remember { BalanceGameManager() }

    LaunchedEffect(startAtChemiResult, tripId, selectedMemberCount) {
        if (startAtChemiResult) {
            val applyReportWithCache: () -> Unit = {
                val base = buildChemiReport(
                    destination = destination,
                    questions = questions,
                    members = members,
                    answersByMember = answersByMember
                )
                computedReport = base
                manager.getCachedAnalysisResult(
                    tripId = tripId,
                    onResult = { cached ->
                        if (cached != null) {
                            computedReport = base.copy(
                                headline = cached.summary.ifBlank { base.headline },
                                tips = cached.compromiseTips.takeIf { it.isNotEmpty() } ?: base.tips,
                                places = cached.places.takeIf { it.isNotEmpty() } ?: base.places
                            )
                        }
                    },
                    onFailure = {}
                )
            }

            manager.loadParticipantAnswers(
                tripId = tripId,
                memberCount = selectedMemberCount,
                questionIds = (1..questions.size).toList(),
                onLoaded = { restoredMap ->
                    restoredMap.forEach { (mIdx, qMap) ->
                        val target = answersByMember.getOrPut(mIdx) { mutableStateMapOf() }
                        if (qMap.isNotEmpty()) {
                            target.clear()
                            target.putAll(qMap)
                        }
                    }
                    applyReportWithCache()
                }
            )
            flowStep = GameFlowStep.CHEMI_RESULT
        }
    }

    LaunchedEffect(destination, reloadNonce, startAtChemiResult) {
        val isDestinationChanged = lastLoadedDestination != null && lastLoadedDestination != destination
        val isManualReload = reloadNonce > lastHandledReloadNonce

        if (lastLoadedDestination != null && !isDestinationChanged && !isManualReload) {
            return@LaunchedEffect
        }

        if (startAtChemiResult && !isManualReload && !isDestinationChanged) {
            lastLoadedDestination = destination
            flowStep = GameFlowStep.CHEMI_RESULT
            return@LaunchedEffect
        }

        isLoadingQuestions = true
        try {
            val generated = geminiHelper.getRandom5BalanceGames(destination)
                .take(5)
                .mapIndexed { idx, item -> item.copy(id = idx + 1) }
            questions = (if (generated.isNotEmpty()) generated else fallbackQuestions)
                .mapIndexed { idx, item -> item.copy(id = idx + 1) }

            if (isDestinationChanged || isManualReload) {
                currentQuestionIndex = 0
                activeMemberIndex = 0
                answersByMember.values.forEach { it.clear() }
                computedReport = null
                flowStep = GameFlowStep.QUESTIONS
            }

            lastLoadedDestination = destination
            lastHandledReloadNonce = reloadNonce
        } catch (_: Exception) {
            questions = fallbackQuestions
            if (isDestinationChanged || isManualReload) {
                currentQuestionIndex = 0
                activeMemberIndex = 0
                answersByMember.values.forEach { it.clear() }
                computedReport = null
                flowStep = GameFlowStep.QUESTIONS
            }
            lastLoadedDestination = destination
            lastHandledReloadNonce = reloadNonce
        } finally {
            isLoadingQuestions = false
        }
    }

    val currentAnswers = answersByMember.getOrPut(activeMemberIndex) { mutableStateMapOf() }

    val allAnswered = questions.isNotEmpty() && (0 until selectedMemberCount).all { mIdx ->
        val map = answersByMember[mIdx].orEmpty()
        questions.all { q -> map[q.id] == "A" || map[q.id] == "B" }
    }

    fun runAiAnalysisAndOpenResult() {
        if (flowStep == GameFlowStep.ANALYZING) return
        flowStep = GameFlowStep.ANALYZING
        coroutineScope.launch {
            try {
                for (mIdx in 0 until selectedMemberCount) {
                    manager.submitParticipantAnswers(
                        tripId = tripId,
                        memberIndex = mIdx,
                        answersByTemplateId = answersByMember[mIdx].orEmpty()
                    )
                }

                val baseReport = buildChemiReport(
                    destination = destination,
                    questions = questions,
                    members = members,
                    answersByMember = answersByMember
                )

                val gamesForAi = questions.mapIndexed { idx, q ->
                    val countA = (0 until selectedMemberCount).count { mIdx ->
                        answersByMember[mIdx]?.get(q.id) == "A"
                    }
                    val countB = (0 until selectedMemberCount).count { mIdx ->
                        answersByMember[mIdx]?.get(q.id) == "B"
                    }
                    BalanceGame(
                        gameId = "q_${q.id}",
                        title = q.title,
                        optionA = q.optionA,
                        optionB = q.optionB,
                        countA = countA,
                        countB = countB,
                        order = (idx + 1).toLong()
                    )
                }

                val aiResult: BalanceGameAnalysisResult? = try {
                    geminiHelper.getRecommendedPlaces(
                        tripTitle = "$destination 여행 (${members.joinToString(", ")})",
                        games = gamesForAi
                    )
                } catch (_: Exception) {
                    null
                }

                val finalReport = if (aiResult != null) {
                    baseReport.copy(
                        headline = aiResult.summary.ifBlank { baseReport.headline },
                        tips = aiResult.compromiseTips.takeIf { it.isNotEmpty() } ?: baseReport.tips,
                        places = aiResult.places.takeIf { it.isNotEmpty() } ?: baseReport.places
                    )
                } else {
                    baseReport
                }

                computedReport = finalReport
                onMemberCountChanged(selectedMemberCount)
                onPlacesRecommended(finalReport.places)
                manager.saveAnalysisResult(
                    tripId = tripId,
                    result = BalanceGameAnalysisResult(
                        summary = finalReport.headline,
                        compromiseTips = finalReport.tips,
                        places = finalReport.places
                    ),
                    memberCount = selectedMemberCount
                )
                flowStep = GameFlowStep.CHEMI_RESULT
            } catch (_: Exception) {
                computedReport = buildChemiReport(
                    destination = destination,
                    questions = questions,
                    members = members,
                    answersByMember = answersByMember
                )
                flowStep = GameFlowStep.CHEMI_RESULT
            }
        }
    }

    Scaffold(
        containerColor = softBg,
        bottomBar = {
            if (bottomBar != null) bottomBar() else FlowBottomBar(
                selectedIndex = 0,
                mainBlue = mainBlue,
                onSelect = { if (it == 0) flowStep = GameFlowStep.QUESTIONS }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .background(softBg)
                .then(if (header == null || (flowStep != GameFlowStep.QUESTIONS && flowStep != GameFlowStep.COMPLETED)) Modifier.statusBarsPadding() else Modifier)
        ) {
            when (flowStep) {
                GameFlowStep.QUESTIONS -> {
                    FigmaQuestionStage(
                        header = header,
                        dateRangeText = dateRangeText,
                        destination = destination,
                        headerBlue = headerBlue,
                        mainBlue = mainBlue,
                        coralOrange = coralOrange,
                        questions = questions,
                        currentQuestionIndex = currentQuestionIndex.coerceIn(0, (questions.size - 1).coerceAtLeast(0)),
                        onSelectQuestionIndex = { currentQuestionIndex = it },
                        isLoadingQuestions = isLoadingQuestions,
                        selectedMemberCount = selectedMemberCount,
                        onCycleMemberCount = {
                            val next = if (selectedMemberCount >= 4) 2 else selectedMemberCount + 1
                            selectedMemberCount = next
                            onMemberCountChanged(next)
                            if (activeMemberIndex >= selectedMemberCount) {
                                activeMemberIndex = 0
                            }
                        },
                        members = members,
                        activeMemberIndex = activeMemberIndex,
                        onSelectMemberTab = { activeMemberIndex = it },
                        currentAnswers = currentAnswers,
                        answersByMember = answersByMember,
                        allAnswered = allAnswered,
                        onRefreshQuestions = { if (!isLoadingQuestions) reloadNonce++ },
                        onAllCompleted = { flowStep = GameFlowStep.COMPLETED }
                    )
                }

                GameFlowStep.COMPLETED -> {
                    FigmaCompletedStage(
                        header = header,
                        headerBlue = headerBlue,
                        coralOrange = coralOrange,
                        totalQuestions = questions.size,
                        selectedMemberCount = selectedMemberCount,
                        members = members,
                        onBackToEdit = {
                            currentQuestionIndex = 0
                            flowStep = GameFlowStep.QUESTIONS
                        },
                        onViewResultClick = { runAiAnalysisAndOpenResult() }
                    )
                }

                GameFlowStep.ANALYZING -> {
                    AiAnalyzingStage(
                        destination = destination,
                        mainBlue = mainBlue
                    )
                }

                GameFlowStep.CHEMI_RESULT -> {
                    val report = computedReport ?: buildChemiReport(
                        destination = destination,
                        questions = questions,
                        members = members,
                        answersByMember = answersByMember
                    )
                    ChemiAnalysisResultCard(
                        report = report,
                        onBackToCompleted = { flowStep = GameFlowStep.COMPLETED },
                        onClose = handleCloseResult,
                        onOpenRecommendedPlaces = { flowStep = GameFlowStep.RECOMMENDED_PLACES }
                    )
                }

                GameFlowStep.RECOMMENDED_PLACES -> {
                    val report = computedReport ?: buildChemiReport(
                        destination = destination,
                        questions = questions,
                        members = members,
                        answersByMember = answersByMember
                    )
                    RecommendedPlacesCard(
                        destination = destination,
                        places = report.places,
                        onBackToResult = { flowStep = GameFlowStep.CHEMI_RESULT },
                        onClose = handleCloseResult,
                        onCreateSchedule = { chosen -> onCreateScheduleWithPlaces(chosen) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AiAnalyzingStage(
    destination: String,
    mainBlue: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 44.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = mainBlue,
                    strokeWidth = 3.5.dp,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "AI가 케미와 맞춤 장소를\n분석하고 있어요...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.Center,
                    lineHeight = 25.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$destination 취향 결과와 타협 팁을 정리 중이에요",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FigmaTopMemberHeader(
    subtitleText: String,
    headerBlue: Color,
    selectedMemberCount: Int,
    members: List<String>,
    activeMemberIndex: Int,
    onSelectMemberTab: (Int) -> Unit,
    onCycleMemberCount: (() -> Unit)? = null,
    onRefreshQuestions: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(headerBlue)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = subtitleText,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.92f),
                fontWeight = FontWeight.SemiBold
            )
            if (onRefreshQuestions != null) {
                IconButton(
                    onClick = onRefreshQuestions,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "질문 재생성",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            members.forEachIndexed { idx, name ->
                val isActive = idx == activeMemberIndex
                val chipColor = when (idx) {
                    0 -> Color(0xFFE57373)
                    1 -> Color(0xFFF6C453)
                    2 -> Color(0xFF64B5F6)
                    else -> Color(0xFF81C784)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onSelectMemberTab(idx) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(chipColor)
                            .border(
                                width = if (isActive) 2.dp else 0.dp,
                                color = if (isActive) Color.White else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.take(1),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = name,
                        fontSize = 10.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onCycleMemberCount != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .clickable { onCycleMemberCount() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "2/3/4명 인원 변경",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${selectedMemberCount}명",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FigmaQuestionStage(
    header: (@Composable () -> Unit)? = null,
    dateRangeText: String,
    destination: String,
    headerBlue: Color,
    mainBlue: Color,
    coralOrange: Color,
    questions: List<GameTemplate>,
    currentQuestionIndex: Int,
    onSelectQuestionIndex: (Int) -> Unit,
    isLoadingQuestions: Boolean,
    selectedMemberCount: Int,
    onCycleMemberCount: () -> Unit,
    members: List<String>,
    activeMemberIndex: Int,
    onSelectMemberTab: (Int) -> Unit,
    currentAnswers: MutableMap<Int, String>,
    answersByMember: Map<Int, Map<Int, String>>,
    allAnswered: Boolean,
    onRefreshQuestions: () -> Unit,
    onAllCompleted: () -> Unit
) {
    val currentQuestion = questions.getOrNull(currentQuestionIndex)
    val selectedOption = currentQuestion?.let { currentAnswers[it.id] }
    val progressFraction = if (questions.isEmpty()) 0f
    else ((currentQuestionIndex + 1).toFloat() / questions.size.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (header != null) header() else FigmaTopMemberHeader(
            subtitleText = dateRangeText,
            headerBlue = headerBlue,
            selectedMemberCount = selectedMemberCount,
            members = members,
            activeMemberIndex = activeMemberIndex,
            onSelectMemberTab = onSelectMemberTab,
            onCycleMemberCount = onCycleMemberCount,
            onRefreshQuestions = onRefreshQuestions
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "여행 밸런스 게임",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
                Text(
                    text = "${(currentQuestionIndex + 1).coerceAtMost(questions.size)} / ${questions.size}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = mainBlue
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = mainBlue,
                trackColor = Color(0xFFE5E7EB)
            )

            Spacer(modifier = Modifier.height(22.dp))

            if (isLoadingQuestions) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = mainBlue)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "$destination 맞춤 5문항 생성 중...",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            } else if (currentQuestion != null) {
                Text(
                    text = currentQuestion.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FigmaVerticalChoiceCard(
                            text = currentQuestion.optionA,
                            isSelected = selectedOption == "A",
                            bgColor = if (selectedOption == "A") Color(0xFFFFDDD2) else Color(0xFFFFEFEA),
                            borderColor = if (selectedOption == "A") coralOrange else Color(0xFFFAD2C5),
                            onClick = { currentAnswers[currentQuestion.id] = "A" }
                        )
                        FigmaVerticalChoiceCard(
                            text = currentQuestion.optionB,
                            isSelected = selectedOption == "B",
                            bgColor = if (selectedOption == "B") Color(0xFFD7E9FF) else Color(0xFFEDF5FF),
                            borderColor = if (selectedOption == "B") mainBlue else Color(0xFFC2DAFA),
                            onClick = { currentAnswers[currentQuestion.id] = "B" }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1F2937)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "VS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val isLastQuestion = currentQuestionIndex >= questions.lastIndex

                Button(
                    onClick = {
                        if (selectedOption != null) {
                            if (!isLastQuestion) {
                                onSelectQuestionIndex(currentQuestionIndex + 1)
                            } else if (allAnswered) {
                                onAllCompleted()
                            } else {
                                val nextMember = (0 until selectedMemberCount).firstOrNull { mIdx ->
                                    val map = answersByMember[mIdx].orEmpty()
                                    questions.any { q -> map[q.id] != "A" && map[q.id] != "B" }
                                }
                                if (nextMember != null) {
                                    onSelectMemberTab(nextMember)
                                    val nextQ = questions.indexOfFirst { q ->
                                        val ans = answersByMember[nextMember]?.get(q.id)
                                        ans != "A" && ans != "B"
                                    }
                                    if (nextQ >= 0) onSelectQuestionIndex(nextQ)
                                }
                            }
                        }
                    },
                    enabled = selectedOption != null || allAnswered,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = coralOrange,
                        disabledContainerColor = Color(0xFFD1D5DB)
                    )
                ) {
                    Text(
                        text = when {
                            isLastQuestion && allAnswered -> "선택 완료"
                            !isLastQuestion -> "선택 완료"
                            else -> "다음 참여자 선택하기"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    members.forEachIndexed { idx, name ->
                        val isDone = questions.all { q ->
                            val ans = answersByMember[idx]?.get(q.id)
                            ans == "A" || ans == "B"
                        }
                        Row(
                            modifier = Modifier
                                .clickable { onSelectMemberTab(idx) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isDone) Color(0xFF16A34A) else Color(0xFF9CA3AF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$name · ${if (isDone) "참여 완료" else "대기"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDone) Color(0xFF16A34A) else Color(0xFF6B7280)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FigmaCompletedStage(
    header: (@Composable () -> Unit)? = null,
    headerBlue: Color,
    coralOrange: Color,
    totalQuestions: Int,
    selectedMemberCount: Int,
    members: List<String>,
    onBackToEdit: () -> Unit,
    onViewResultClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (header != null) header() else FigmaTopMemberHeader(
            subtitleText = "참여자 ${selectedMemberCount}명",
            headerBlue = headerBlue,
            selectedMemberCount = selectedMemberCount,
            members = members,
            activeMemberIndex = 0,
            onSelectMemberTab = {}
        )

        Spacer(modifier = Modifier.height(14.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "여행 밸런스 게임",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )
                Text(
                    text = "$totalQuestions / $totalQuestions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = coralOrange
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { 1f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = coralOrange,
                trackColor = Color(0xFFE5E7EB)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "밸런스 게임 완료! 🎉",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF111827)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "결과를 확인해 보세요.",
                fontSize = 13.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onViewResultClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = coralOrange)
            ) {
                Text(
                    text = "결과보기",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                members.forEach { name ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0FDF4))
                            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
                            .clickable { onBackToEdit() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$name · 참여 완료",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FigmaVerticalChoiceCard(
    text: String,
    isSelected: Boolean,
    bgColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 20.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChemiAnalysisResultCard(
    report: ChemiComputedReport,
    onBackToCompleted: () -> Unit,
    onClose: () -> Unit,
    onOpenRecommendedPlaces: () -> Unit
) {
    val mainBlue = Color(0xFF0055D4)
    val isGroupMode = report.memberCount >= 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToCompleted, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "완료 화면으로",
                            tint = Color(0xFF374151)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "메이트와의 케미 분석 결과",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "밸런스 게임 ${report.axes.size}문항 기준",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color(0xFF374151)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF5D50A6), Color(0xFF7B6BB8))
                        )
                    )
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE86A33))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = report.subBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = report.headline,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 23.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val statusList = if (isGroupMode) {
                        listOf("만장일치", "다수결", "대립")
                    } else {
                        listOf("일치", "대립")
                    }
                    statusList.forEach { st ->
                        val count = report.axes.count { it.statusLabel == st }
                        val (pillBg, pillFg) = when (st) {
                            "만장일치", "일치" -> Color.White.copy(alpha = 0.25f) to Color.White
                            "다수결" -> Color.White.copy(alpha = 0.18f) to Color.White
                            else -> Color(0xFFE86A33) to Color.White
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(pillBg)
                                .padding(horizontal = 11.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$st $count",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = pillFg
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
                color = Color(0xFF111827)
            )

            Spacer(modifier = Modifier.height(12.dp))

            report.axes.forEach { item ->
                if (!isGroupMode) {
                    TwoPersonAxisRow(item = item)
                } else {
                    GroupAxisBarRow(item = item)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF3F6FF))
                    .padding(14.dp)
            ) {
                Text(
                    text = "이렇게 타협해봐요",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = mainBlue
                )
                Spacer(modifier = Modifier.height(6.dp))
                report.tips.forEachIndexed { idx, tip ->
                    Text(
                        text = "${idx + 1}. $tip",
                        fontSize = 12.sp,
                        color = Color(0xFF374151),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onOpenRecommendedPlaces,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = mainBlue)
            ) {
                Text(
                    text = "우리에게 맞는 추천 장소 보기 →",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TwoPersonAxisRow(item: AxisComparisonItem) {
    val isMatch = item.statusLabel == "일치"
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.leftLabel} VS ${item.rightLabel}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isMatch) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.statusLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMatch) Color(0xFF15803D) else Color(0xFFB91C1C)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                item.leftVotes.forEach { MemberMiniChip(it) }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .height(2.dp)
                    .background(Color(0xFFD1D5DB))
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                item.rightVotes.forEach { MemberMiniChip(it) }
            }
        }
    }
}

@Composable
private fun GroupAxisBarRow(item: AxisComparisonItem) {
    val badgeBg = when (item.statusLabel) {
        "만장일치" -> Color(0xFFDCFCE7)
        "다수결" -> Color(0xFFDBEAFE)
        else -> Color(0xFFFEE2E2)
    }
    val badgeFg = when (item.statusLabel) {
        "만장일치" -> Color(0xFF15803D)
        "다수결" -> Color(0xFF1D4ED8)
        else -> Color(0xFFB91C1C)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.leftLabel} VS ${item.rightLabel}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeBg)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.statusLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeFg
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF3F4F6)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.leftVotes.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .weight(item.leftVotes.size.toFloat())
                        .height(30.dp)
                        .background(Color(0xFF3B82F6))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.leftLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    item.leftVotes.forEach { MemberMiniChip(it) }
                }
            }
            if (item.rightVotes.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .weight(item.rightVotes.size.toFloat())
                        .height(30.dp)
                        .background(Color(0xFFF97316))
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
                ) {
                    item.rightVotes.forEach { MemberMiniChip(it) }
                    Text(
                        text = item.rightLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberMiniChip(name: String) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF111827)
        )
    }
}

@Composable
private fun RecommendedPlacesCard(
    destination: String,
    places: List<RecommendedPlace>,
    onBackToResult: () -> Unit,
    onClose: () -> Unit,
    onCreateSchedule: (List<RecommendedPlace>) -> Unit
) {
    val context = LocalContext.current
    val mainBlue = Color(0xFF0055D4)
    val categories = listOf("전체", "감성", "핫플", "자연", "도시", "로컬", "유명")
    var selectedCategory by rememberSaveable { mutableStateOf("전체") }
    val checkedNames = remember {
        mutableStateListOf<String>().apply {
            places.take(2).forEach { add(it.name) }
        }
    }

    val filteredPlaces = remember(selectedCategory, places) {
        if (selectedCategory == "전체") places
        else places.filter {
            normalizeFixedCategory(it.category, it.tag, it.name) == selectedCategory
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackToResult, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "케미 결과로",
                            tint = Color(0xFF374151)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "맞춤형 추천 장소",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = "마음에 드는 장소를 체크해서 일정에 담아보세요",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color(0xFF374151)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val active = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (active) mainBlue else Color(0xFFF3F4F6))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else Color(0xFF4B5563)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            filteredPlaces.forEachIndexed { idx, place ->
                val isChecked = checkedNames.contains(place.name)
                val fixedCat = normalizeFixedCategory(place.category, place.tag, place.name)
                val (badgeBg, badgeFg) = categoryBadgeColors(fixedCat)
                val thumbUrl = remember(fixedCat, place.name, idx) {
                    getReliablePlaceImageUrl(fixedCat, place.name, idx)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable {
                            if (isChecked) checkedNames.remove(place.name)
                            else checkedNames.add(place.name)
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(thumbUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = place.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE5E7EB))
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(badgeBg)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = fixedCat,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeFg
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = place.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF111827),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = place.reason,
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isChecked) mainBlue else Color.White)
                            .border(
                                width = 1.5.dp,
                                color = if (isChecked) mainBlue else Color(0xFFD1D5DB),
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "선택됨",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val selectedPlaces = places.filter { checkedNames.contains(it.name) }

            Button(
                onClick = { onCreateSchedule(selectedPlaces) },
                enabled = selectedPlaces.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = mainBlue,
                    disabledContainerColor = Color(0xFFD1D5DB)
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "선택한 장소로 일정 만들기",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun normalizeFixedCategory(category: String, tag: String, placeName: String): String {
    val raw = "$category $tag $placeName"
    return when {
        raw.contains("감성") || raw.contains("카페") || raw.contains("야경") -> "감성"
        raw.contains("핫플") || raw.contains("트렌드") || raw.contains("포토") -> "핫플"
        raw.contains("자연") || raw.contains("힐링") || raw.contains("바다") || raw.contains("해수욕장") || raw.contains("산") -> "자연"
        raw.contains("도시") || raw.contains("시내") || raw.contains("타워") || raw.contains("쇼핑") -> "도시"
        raw.contains("로컬") || raw.contains("골목") || raw.contains("시장") || raw.contains("맛집") -> "로컬"
        else -> "유명"
    }
}

private fun getReliablePlaceImageUrl(fixedCategory: String, placeName: String, index: Int): String {
    val natureImages = listOf(
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&q=80",
        "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=400&q=80",
        "https://images.unsplash.com/photo-1501785888041-af3ef285b470?w=400&q=80"
    )
    val emotionImages = listOf(
        "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=400&q=80",
        "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=400&q=80",
        "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=400&q=80"
    )
    val hotplaceImages = listOf(
        "https://images.unsplash.com/photo-1514933651103-005eec06c04b?w=400&q=80",
        "https://images.unsplash.com/photo-1519501025264-65ba15a82390?w=400&q=80",
        "https://images.unsplash.com/photo-1534430480872-3498386e7856?w=400&q=80"
    )

    val targetList = when (fixedCategory) {
        "자연" -> natureImages
        "감성", "로컬" -> emotionImages
        else -> hotplaceImages
    }
    return targetList[index % targetList.size]
}

private fun categoryBadgeColors(category: String): Pair<Color, Color> {
    return when (category) {
        "감성" -> Color(0xFFF3E8FF) to Color(0xFF7E22CE)
        "핫플" -> Color(0xFFFEE2E2) to Color(0xFFB91C1C)
        "자연" -> Color(0xFFDCFCE7) to Color(0xFF15803D)
        "도시" -> Color(0xFFDBEAFE) to Color(0xFF1D4ED8)
        "로컬" -> Color(0xFFFEF3C7) to Color(0xFFB45309)
        else -> Color(0xFFE0E7FF) to Color(0xFF4338CA)
    }
}

private fun fixedAxisPair(index: Int): Pair<String, String> {
    val fixedAxes = listOf(
        "감성" to "핫플",
        "자연" to "도시",
        "로컬" to "유명"
    )
    return fixedAxes[index % fixedAxes.size]
}

private fun buildChemiReport(
    destination: String,
    questions: List<GameTemplate>,
    members: List<String>,
    answersByMember: Map<Int, Map<Int, String>>
): ChemiComputedReport {
    val memberCount = members.size
    val targetQuestions = questions.take(3)

    val axes = targetQuestions.mapIndexed { idx, q ->
        val leftVotes = mutableListOf<String>()
        val rightVotes = mutableListOf<String>()
        members.forEachIndexed { mIdx, name ->
            when (answersByMember[mIdx]?.get(q.id)) {
                "B" -> rightVotes.add(name)
                else -> leftVotes.add(name)
            }
        }

        val (leftShort, rightShort) = fixedAxisPair(idx)

        val statusLabel = if (memberCount == 2) {
            if (leftVotes.size == 2 || rightVotes.size == 2) "일치" else "대립"
        } else {
            when {
                leftVotes.size == memberCount || rightVotes.size == memberCount -> "만장일치"
                leftVotes.size == rightVotes.size -> "대립"
                else -> "다수결"
            }
        }

        val dominant = if (leftVotes.size >= rightVotes.size) leftShort else rightShort
        val hashtag = when (statusLabel) {
            "만장일치", "일치" -> "#${dominant}만장일치"
            "다수결" -> "#${dominant}다수결"
            else -> "#${leftShort}vs${rightShort}"
        }

        AxisComparisonItem(
            leftLabel = leftShort,
            rightLabel = rightShort,
            leftVotes = leftVotes,
            rightVotes = rightVotes,
            statusLabel = statusLabel,
            hashtag = hashtag
        )
    }

    val summaryHashtags = axes.map { it.hashtag }.distinct()
    val subBadge = if (memberCount >= 3) "단체 케미 타입" else "우리의 케미 타입"
    val matchAxis = axes.firstOrNull { it.statusLabel == "만장일치" || it.statusLabel == "일치" }
    val conflictAxis = axes.firstOrNull { it.statusLabel == "대립" }

    val headline = when {
        matchAxis != null && conflictAxis != null -> {
            val matchWord = if (matchAxis.leftVotes.size >= matchAxis.rightVotes.size) matchAxis.leftLabel else matchAxis.rightLabel
            "${matchWord}은 만장일치,\n${conflictAxis.leftLabel} vs ${conflictAxis.rightLabel}은 ${conflictAxis.leftVotes.size}:${conflictAxis.rightVotes.size}!"
        }
        conflictAxis != null -> {
            "${conflictAxis.leftLabel} vs ${conflictAxis.rightLabel}은 ${conflictAxis.leftVotes.size}:${conflictAxis.rightVotes.size}!\n취향 타협이 필요해요"
        }
        else -> {
            "${summaryHashtags.joinToString(" ")}\n취향 완벽 일치!"
        }
    }

    val tips = listOf(
        "의견이 모인 #${axes.firstOrNull()?.leftLabel ?: "감성"} 스팟부터 함께 둘러보세요.",
        "취향이 갈리는 코스는 오전·오후로 나눠 번갈아 방문해 보세요."
    )

    val places = listOf(
        RecommendedPlace(
            name = "$destination 해수욕장",
            category = "자연",
            reason = "바다와 산책로로 여유롭게 즐기기 좋은 곳",
            tag = "#자연"
        ),
        RecommendedPlace(
            name = "$destination 감성골목",
            category = "로컬",
            reason = "아기자기한 골목과 감성 사진 명소",
            tag = "#로컬"
        ),
        RecommendedPlace(
            name = "$destination 전망타워",
            category = "유명",
            reason = "$destination 시내를 한눈에 볼 수 있는 대표 명소",
            tag = "#유명"
        )
    )

    return ChemiComputedReport(
        memberCount = memberCount,
        memberNames = members,
        headline = headline,
        subBadge = subBadge,
        summaryHashtags = summaryHashtags,
        axes = axes,
        tips = tips,
        places = places
    )
}

@Composable
private fun FlowBottomBar(
    selectedIndex: Int,
    mainBlue: Color,
    onSelect: (Int) -> Unit
) {
    val items = listOf<Triple<String, ImageVector, Boolean>>(
        Triple("여행룸", Icons.Default.Home, false),
        Triple("일정", Icons.Default.DateRange, false),
        Triple("가계부", Icons.Default.Place, true)
    )
    NavigationBar(containerColor = Color.White, tonalElevation = 6.dp) {
        items.forEachIndexed { index, (label, icon, isLocationPin) ->
            val selected = selectedIndex == index
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(index) },
                icon = {
                    Icon(
                        imageVector = if (isLocationPin) Icons.Default.LocationOn else icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = mainBlue,
                    selectedTextColor = mainBlue,
                    unselectedIconColor = Color(0xFF9CA3AF),
                    unselectedTextColor = Color(0xFF9CA3AF),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}