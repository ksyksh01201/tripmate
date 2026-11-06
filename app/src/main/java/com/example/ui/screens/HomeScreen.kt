package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Trip
import com.example.data.model.TripHelper
import com.example.ui.components.SuitcaseIllustration
import com.example.ui.components.TripMateLogo
import com.example.ui.viewmodel.HomeViewModel

private val HomeBlue = Color(0xFF4A90E2)

private enum class HomeDialog { None, Create, Join }

/**
 * 로그인 이후 표시되는 TripMate 홈 화면.
 * - 여행이 없으면: 여행가방 아이콘 + "첫 여행을 시작해 볼까요?" 빈 화면
 * - 여행이 있으면: "내 여행" 목록 + 하단 [여행 생성] [초대코드 참여] 버튼
 * 여행을 만들거나 참여하면 onOpenTrip(tripId)로 여행룸으로 이동합니다.
 */
@Composable
fun HomeScreen(
    onOpenTrip: (tripId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val trips by viewModel.myTrips.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf(HomeDialog.None) }

    // 생성/참여가 끝나면 팝업을 닫고 여행룸으로 이동
    LaunchedEffect(uiState.openTripId) {
        val tripId = uiState.openTripId ?: return@LaunchedEffect
        dialog = HomeDialog.None
        viewModel.onTripOpened()
        onOpenTrip(tripId)
    }

    val openCreate = {
        viewModel.clearError()
        dialog = HomeDialog.Create
    }
    val openJoin = {
        viewModel.clearError()
        dialog = HomeDialog.Join
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeTopBar()

            val currentTrips = trips
            when {
                currentTrips == null -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = HomeBlue) }

                currentTrips.isEmpty() -> EmptyHomeContent(
                    modifier = Modifier.weight(1f),
                    onCreateClick = openCreate,
                    onJoinClick = openJoin
                )

                else -> TripListContent(
                    trips = currentTrips,
                    modifier = Modifier.weight(1f),
                    onTripClick = { onOpenTrip(it.tripId) },
                    onCreateClick = openCreate,
                    onJoinClick = openJoin
                )
            }
        }
    }

    when (dialog) {
        HomeDialog.Create -> CreateTripDialog(
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            onDismiss = { dialog = HomeDialog.None },
            onCreate = { destination, start, end, companion ->
                viewModel.createTrip(destination, start, end, companion)
            }
        )

        HomeDialog.Join -> JoinTripDialog(
            isSubmitting = uiState.isSubmitting,
            errorMessage = uiState.errorMessage,
            onDismiss = { dialog = HomeDialog.None },
            onCodeChange = { if (uiState.errorMessage != null) viewModel.clearError() },
            onJoin = { code -> viewModel.joinTrip(code) }
        )

        HomeDialog.None -> Unit
    }
}

/** 여행이 하나도 없을 때의 빈 화면 (기존 디자인 그대로) */
@Composable
private fun EmptyHomeContent(
    modifier: Modifier,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(0.9f))

        SuitcaseIllustration()

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "첫 여행을 시작해 볼까요?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "새 여행을 만들거나\n친구의 초대 코드로 참여해 보세요",
            fontSize = 16.sp,
            color = Color(0xFF808080),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = onCreateClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("empty_create_trip_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HomeBlue,
                contentColor = Color.White
            )
        ) {
            Text(text = "새 여행 만들기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedBlueButton(
            text = "초대 코드로 참여하기",
            onClick = onJoinClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("empty_join_trip_button")
        )

        Spacer(modifier = Modifier.weight(1.3f))
    }
}

/** 여행이 있을 때: 내 여행 목록 + 하단 버튼 2개 */
@Composable
private fun TripListContent(
    trips: List<Trip>,
    modifier: Modifier,
    onTripClick: (Trip) -> Unit,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "내 여행",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(trips, key = { it.tripId }) { trip ->
                TripCard(trip = trip, onClick = { onTripClick(trip) })
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCreateClick,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("home_create_trip_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HomeBlue, contentColor = Color.White)
            ) {
                Text("여행 생성", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedBlueButton(
                text = "초대코드 참여",
                onClick = onJoinClick,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .testTag("home_join_trip_button")
            )
        }
    }
}

@Composable
private fun TripCard(trip: Trip, onClick: () -> Unit) {
    val dDay = TripHelper.calculateDDay(trip.startDate)
    val dDayText = when {
        dDay == 0 -> "D-Day"
        dDay < 0 -> "D$dDay"
        else -> "D+$dDay"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE0E3E8), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
            .testTag("trip_card_${trip.tripId}")
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFE3EEFC))
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Text(dDayText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HomeBlue)
        }
        Spacer(Modifier.height(8.dp))
        Text(trip.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(trip.dateRangeText, fontSize = 13.sp, color = Color(0xFF808080), modifier = Modifier.weight(1f))
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = Color(0xFF808080),
                modifier = Modifier.size(16.dp)
            )
            Text("${trip.memberCount}", fontSize = 13.sp, color = Color(0xFF808080))
        }
    }
}

@Composable
private fun OutlinedBlueButton(text: String, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, HomeBlue, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HomeBlue)
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TripMateLogo(
            color = HomeBlue,
            fontSize = 32.sp
        )
        IconButton(
            onClick = {
                // 설정 화면은 다음 단계에서 연결
            },
            modifier = Modifier
                .size(40.dp)
                .testTag("home_settings_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "설정",
                tint = Color(0xFFB0B0B0),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
