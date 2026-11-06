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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Trip
import com.example.ui.components.BusanThumbnail
import com.example.ui.components.GenericTravelThumbnail
import com.example.ui.components.JejuThumbnail
import com.example.ui.components.TripMateLogo
import com.example.ui.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import java.util.TimeZone

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

// ---------- 여행이 있을 때: 내 여행 / 지난 여행 목록 ----------

private val CardBlue = Color(0xFF3F7FE8)
private val BadgeOrange = Color(0xFFF2915A)
private val BadgeBlueBg = Color(0xFFE3EEFC)
private val SubGray = Color(0xFF808080)
private val PastPill = Color(0xFFE9EAEC)
private val CreateButtonBlue = Color(0xFF5B8DE3)
private val MemberColors = listOf(
    Color(0xFF2B2550), Color(0xFFF2915A), Color(0xFF4A90E2),
    Color(0xFF34C38F), Color(0xFF9B6BDF), Color(0xFFF5A623)
)
private const val PAST_PREVIEW_COUNT = 2

/**
 * 여행이 있을 때 홈 화면.
 * - 내 여행: 아직 끝나지 않은 여행 (가장 가까운 여행은 파란 카드로 강조)
 * - 지난 여행: 종료일이 지난 여행 (기본 2개, "여행 전체보기"로 모두 보기)
 * - 하단: [여행 생성] [초대코드로 참여]
 */
@Composable
private fun TripListScreen(
    trips: List<Trip>,
    onTripClick: (Trip) -> Unit,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit
) {
    val today = remember { todayDayNumber() }
    val myUid = remember { runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull() }
    val upcoming = trips
        .filter { (dayNumber(it.endDate) ?: Long.MAX_VALUE) >= today }
        .sortedBy { it.startDate }
    val past = trips
        .filter { (dayNumber(it.endDate) ?: Long.MAX_VALUE) < today }
        .sortedByDescending { it.endDate }
    var showAllPast by rememberSaveable { mutableStateOf(false) }
    val shownPast = if (showAllPast) past else past.take(PAST_PREVIEW_COUNT)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFCFD))
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
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SectionTitle("내 여행") }
            if (upcoming.isEmpty()) {
                item {
                    Text(
                        text = "예정된 여행이 없어요. 새 여행을 만들어 보세요!",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        fontSize = 14.sp,
                        color = SubGray
                    )
                }
            }
            itemsIndexed(upcoming, key = { _, trip -> trip.tripId }) { index, trip ->
                UpcomingTripCard(
                    trip = trip,
                    dDay = (dayNumber(trip.startDate) ?: today) - today,
                    highlighted = index == 0,
                    myUid = myUid,
                    onClick = { onTripClick(trip) }
                )
            }

            if (past.isNotEmpty()) {
                item { SectionTitle("지난 여행", topPadding = 12.dp) }
                items(shownPast, key = { it.tripId }) { trip ->
                    PastTripCard(trip = trip, onClick = { onTripClick(trip) })
                }
                if (past.size > PAST_PREVIEW_COUNT) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (showAllPast) "접기  ∧" else "여행 전체보기  >",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAllPast = !showAllPast }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("home_show_all_past"),
                                fontSize = 14.sp,
                                color = Color(0xFF5F6368)
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = onCreateClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("home_create_trip_button"),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CreateButtonBlue, contentColor = Color.White)
            ) {
                Text("여행 생성", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, CreateButtonBlue, RoundedCornerShape(8.dp))
                    .clickable(onClick = onJoinClick)
                    .testTag("home_join_trip_button"),
                contentAlignment = Alignment.Center
            ) {
                Text("초대코드로 참여", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CreateButtonBlue)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 8.dp, top = topPadding),
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black
    )
}

/** 다가오는 여행 카드. 가장 가까운 여행(highlighted)은 파란 배경 + 주황 D-day + 멤버 표시 */
@Composable
private fun UpcomingTripCard(
    trip: Trip,
    dDay: Long,
    highlighted: Boolean,
    myUid: String?,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val titleColor = if (highlighted) Color.White else Color.Black
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (highlighted) 6.dp else 3.dp, shape)
            .clip(shape)
            .background(if (highlighted) CardBlue else Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("trip_card_${trip.tripId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (highlighted) BadgeOrange else BadgeBlueBg)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = dDayText(dDay),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (highlighted) Color.White else Color(0xFF3A6FC4)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = trip.title,
                fontSize = if (highlighted) 21.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = koreanDateRange(trip.startDate, trip.endDate),
                fontSize = 14.sp,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal,
                color = if (highlighted) Color.White else SubGray
            )
            if (highlighted) {
                Spacer(Modifier.height(10.dp))
                MemberCircles(members = trip.members, myUid = myUid)
            }
        }
        Spacer(Modifier.width(12.dp))
        DestinationCircle(destination = trip.destination, size = if (highlighted) 96.dp else 84.dp)
    }
}

/** 멤버 동그라미 (겹쳐서 최대 4개) */
@Composable
private fun MemberCircles(members: List<String>, myUid: String?) {
    Row {
        members.take(4).forEachIndexed { index, uid ->
            Box(
                modifier = Modifier
                    .offset(x = (-6 * index).dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MemberColors[index % MemberColors.size]),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uid == myUid) "나" else "${index + 1}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/** 여행지 동그라미 그림: 프로젝트에 이미 있는 TripVisuals.kt의 썸네일을 그대로 사용 */
@Composable
private fun DestinationCircle(destination: String, size: androidx.compose.ui.unit.Dp) {
    val modifier = Modifier.size(size)
    when (destination) {
        "부산" -> BusanThumbnail(modifier = modifier)
        "제주" -> JejuThumbnail(modifier = modifier)
        else -> GenericTravelThumbnail(modifier = modifier)
    }
}

/** 지난 여행 카드: 제목 / 날짜 / [완료] */
@Composable
private fun PastTripCard(trip: Trip, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape)
            .clip(shape)
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .testTag("past_trip_card_${trip.tripId}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(trip.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.Black, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(koreanDateRange(trip.startDate, trip.endDate), fontSize = 14.sp, color = SubGray)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(PastPill)
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text("완료", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3C3C3C))
        }
    }
}

private fun dDayText(dDay: Long): String = when {
    dDay > 0 -> "D-$dDay"
    dDay == 0L -> "D-Day"
    else -> "여행 중"
}

/** "2026-08-01" → 날짜 번호(일 단위). 날짜 비교/디데이 계산용 */
private fun dayNumber(date: String): Long? {
    val p = date.split("-").mapNotNull { it.toIntOrNull() }
    if (p.size != 3) return null
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(p[0], p[1] - 1, p[2])
    }
    return cal.timeInMillis / (24L * 60 * 60 * 1000)
}

private fun todayDayNumber(): Long {
    val local = Calendar.getInstance()
    return dayNumber(
        "%d-%d-%d".format(local.get(Calendar.YEAR), local.get(Calendar.MONTH) + 1, local.get(Calendar.DAY_OF_MONTH))
    )!!
}
