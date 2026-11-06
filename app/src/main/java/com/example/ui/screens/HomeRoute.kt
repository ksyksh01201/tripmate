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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Trip
import com.example.data.model.TripHelper
import com.example.ui.components.TripMateLogo
import com.example.ui.viewmodel.HomeViewModel

private val HomeBlue = Color(0xFF4A90E2)

private enum class HomeDialog { None, Create, Join }

/**
 * 홈 화면의 "동작" 담당 (기존 HomeScreen 디자인은 그대로 사용).
 * - 여행이 없으면: 기존 HomeScreen (빈 화면)
 * - 여행이 있으면: 내 여행 목록
 * - [새 여행 만들기] / [초대 코드로 참여하기] 팝업을 띄우고, 완료되면 onOpenTrip(tripId)로 여행룸 이동
 */
@Composable
fun HomeRoute(
    onOpenTrip: (tripId: String) -> Unit,
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

    val currentTrips = trips
    when {
        currentTrips == null -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = HomeBlue) }

        currentTrips.isEmpty() -> HomeScreen(
            onCreateTripClick = openCreate,
            onJoinTripClick = openJoin
        )

        else -> TripListScreen(
            trips = currentTrips,
            onTripClick = { onOpenTrip(it.tripId) },
            onCreateClick = openCreate,
            onJoinClick = openJoin
        )
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

/** 여행이 있을 때: 내 여행 목록 + 하단 버튼 2개 */
@Composable
private fun TripListScreen(
    trips: List<Trip>,
    onTripClick: (Trip) -> Unit,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TripMateLogo(color = HomeBlue, fontSize = 32.sp)
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "설정",
                tint = Color(0xFFB0B0B0),
                modifier = Modifier.size(26.dp)
            )
        }

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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .border(1.dp, HomeBlue, RoundedCornerShape(10.dp))
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onJoinClick
                    )
                    .testTag("home_join_trip_button"),
                contentAlignment = Alignment.Center
            ) {
                Text("초대코드 참여", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HomeBlue)
            }
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
