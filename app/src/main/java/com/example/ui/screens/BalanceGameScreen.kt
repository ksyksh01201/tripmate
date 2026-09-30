package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PreferenceQuestions
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceGameScreen(
    onBackClick: () -> Unit,
    onGameComplete: (Map<Int, Boolean>) -> Unit,
    modifier: Modifier = Modifier
) {
    val questions = PreferenceQuestions.questions
    var currentIndex by remember { mutableIntStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, Boolean>() }
    val coroutineScope = rememberCoroutineScope()
    var selectedOption by remember { mutableStateOf<Boolean?>(null) }
    var isTransitioning by remember { mutableStateOf(false) }

    val currentQuestion = questions[currentIndex]
    val progress = (currentIndex + 1) / questions.size.toFloat()

    fun selectOption(isA: Boolean) {
        if (isTransitioning) return
        isTransitioning = true
        selectedOption = isA
        answers[currentQuestion.id] = isA

        coroutineScope.launch {
            delay(200) // 짧은 선택 효과 후 다음 문제로 이동
            selectedOption = null
            if (currentIndex < questions.size - 1) {
                currentIndex++
                isTransitioning = false
            } else {
                onGameComplete(answers)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = "여행 취향",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (isTransitioning) return@IconButton
                                if (currentIndex > 0) {
                                    currentIndex--
                                    selectedOption = null
                                } else {
                                    onBackClick()
                                }
                            },
                            modifier = Modifier.testTag("game_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "이전 질문",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        Text(
                            text = "${currentIndex + 1} / ${questions.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )

                // 얇은 진행률 바 (1번=10%, 5번=50%, 10번=100%)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = TravelBluePrimary,
                    trackColor = Color(0xFFF1F5F9)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.45f))

            // 질문
            Text(
                text = currentQuestion.question,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                lineHeight = 29.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            )

            // 선택지 1 (Option A)
            SimpleOptionCard(
                text = currentQuestion.optionA.text,
                isSelected = selectedOption == true,
                onClick = { selectOption(true) },
                testTag = "option_a_card"
            )

            // 두 선택지를 자연스럽게 구분하는 심플한 VS
            Text(
                text = "VS",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(vertical = 10.dp)
            )

            // 선택지 2 (Option B)
            SimpleOptionCard(
                text = currentQuestion.optionB.text,
                isSelected = selectedOption == false,
                onClick = { selectOption(false) },
                testTag = "option_b_card"
            )

            Spacer(modifier = Modifier.weight(1.0f))
        }
    }
}

@Composable
fun SimpleOptionCard(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFFEFF6FF) else Color.White
    val borderColor = if (isSelected) TravelBluePrimary else Color(0xFFCBD5E1)
    val textColor = if (isSelected) TravelBluePrimary else TextPrimary
    val borderWidth = if (isSelected) 1.5.dp else 1.2.dp

    OutlinedCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.84f)
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = backgroundColor
        ),
        border = BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.outlinedCardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

