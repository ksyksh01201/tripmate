package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DaySchedule
import com.example.model.PlaceDataProvider
import com.example.model.Trip
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScheduleScreen(
    trip: Trip,
    daySchedules: List<DaySchedule>,
    onBackClick: () -> Unit,
    onViewMapRoute: (Int) -> Unit = {},
    onViewExpense: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // [6. selectedDay 상태 사용 & 9. 진입 시 DAY 1 기본 선택]
    var selectedDay by remember(trip.id) { mutableIntStateOf(0) }

    val currentTripId = trip.id
    // [7. 여행 날짜에서 DAY 계산 & 8. 여행별 DAY 완전 분리]
    val effectiveDays = remember(trip.id, trip.startDate, trip.endDate, daySchedules) {
        val baseDays = PlaceDataProvider.generateDays(trip.startDate, trip.endDate)
        val allTripItems = daySchedules.flatMap { it.items }
            .filter { it.tripId.isEmpty() || it.tripId == currentTripId }

        baseDays.map { baseDay ->
            val dayExisting = daySchedules.firstOrNull { it.dayNumber == baseDay.dayNumber || it.dateStr == baseDay.dateStr }
            val directItems = dayExisting?.items?.filter { it.tripId.isEmpty() || it.tripId == currentTripId }

            val dateMatchingItems = allTripItems.filter { it.date == baseDay.dateStr }

            val items = when {
                !directItems.isNullOrEmpty() -> directItems
                dateMatchingItems.isNotEmpty() -> dateMatchingItems
                else -> emptyList()
            }
            baseDay.copy(
                items = items.distinctBy { it.id }.sortedBy { PlaceDataProvider.timeToMinutes(it.time) }
            )
        }
    }

    val currentDay = effectiveDays.getOrElse(selectedDay.coerceIn(0, (effectiveDays.size - 1).coerceAtLeast(0))) {
        DaySchedule(1, trip.startDate, emptyList())
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (trip.title.isNotBlank()) "전체 일정 · ${trip.title}" else "전체 일정",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val dateText = if (trip.startDate.isNotBlank() && trip.endDate.isNotBlank()) {
                            "${trip.startDate} ~ ${trip.endDate}"
                        } else {
                            trip.startDate
                        }
                        if (dateText.isNotBlank()) {
                            Text(
                                text = dateText,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("full_schedule_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { onViewMapRoute(selectedDay) },
                        modifier = Modifier.testTag("top_map_button")
                    ) {
                        Text(
                            text = "지도 보기",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // [1. DAY 탭을 날짜 필터로 변경 & 5. 선택된 DAY 표시]
            if (effectiveDays.size > 1) {
                TabRow(
                    selectedTabIndex = selectedDay.coerceIn(0, effectiveDays.size - 1),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    effectiveDays.forEachIndexed { index, day ->
                        Tab(
                            selected = selectedDay == index,
                            onClick = {
                                selectedDay = index
                            },
                            text = {
                                Text(
                                    text = "DAY ${day.dayNumber}",
                                    fontWeight = if (selectedDay == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }
            }

            // [12. 전체 일정을 세로로 나열하지 않고 선택한 DAY 일정만 표시]
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // DAY 헤더: DAY 1 · 10.11
                item {
                    val headerText = if (currentDay.dateStr.isNotBlank()) {
                        "DAY ${currentDay.dayNumber} · ${currentDay.dateStr}"
                    } else {
                        "DAY ${currentDay.dayNumber}"
                    }
                    Text(
                        text = headerText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // [4. 등록된 일정이 없는 경우]
                if (currentDay.items.isEmpty()) {
                    item {
                        Text(
                            text = "등록된 일정이 없습니다.",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 6.dp)
                        )
                    }
                } else {
                    // [2. & 3. 선택한 DAY의 일정 목록 표시]
                    itemsIndexed(currentDay.items) { itemIdx, item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirmed_item_${item.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.time,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    text = item.placeName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }

                        if (itemIdx < currentDay.items.size - 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
