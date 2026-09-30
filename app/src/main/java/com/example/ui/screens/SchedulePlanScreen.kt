package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DaySchedule
import com.example.model.Place
import com.example.model.PlaceDataProvider
import com.example.model.ScheduleItem
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulePlanScreen(
    tripId: String = "",
    initialSchedules: List<DaySchedule> = emptyList(),
    selectedPlaces: List<Place> = emptyList(),
    startDate: String = "10.10",
    endDate: String = "10.12",
    onBackClick: () -> Unit,
    onScheduleConfirmed: (List<DaySchedule>) -> Unit,
    onViewFullSchedule: () -> Unit,
    onViewMapRoute: () -> Unit,
    modifier: Modifier = Modifier
) {
    var daySchedules by remember(initialSchedules, startDate, endDate) {
        mutableStateOf(
            if (initialSchedules.isNotEmpty()) {
                initialSchedules
            } else {
                PlaceDataProvider.generateDays(startDate, endDate)
            }
        )
    }

    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showUnplacedWarningDialog by remember { mutableStateOf(false) }

    var selectedItemForAction by remember { mutableStateOf<ScheduleItem?>(null) }

    val currentDay = daySchedules.getOrElse(selectedDayIndex) {
        daySchedules.firstOrNull() ?: DaySchedule(1, startDate, emptyList())
    }

    // 전체 일정에 이미 배치된 장소명 목록 (중복 장소 방지)
    val allScheduledPlaceNames = remember(daySchedules) {
        daySchedules.flatMap { it.items }.map { it.placeName }.toSet()
    }

    // 아직 일정에 추가되지 않은 선택 장소 목록
    val availablePlaces = remember(selectedPlaces, allScheduledPlaceNames) {
        selectedPlaces.filter { it.name !in allScheduledPlaceNames }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "여행 일정",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("schedule_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = onViewMapRoute,
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
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            val unplaced = selectedPlaces.filter { it.name !in allScheduledPlaceNames }
                            if (unplaced.isNotEmpty()) {
                                showUnplacedWarningDialog = true
                            } else {
                                onScheduleConfirmed(daySchedules)
                                onViewFullSchedule()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_schedule_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    ) {
                        Text(
                            text = "일정 확정",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // DAY 1 | DAY 2 | DAY 3 탭 (실제 여행 날짜 기준 동적 생성)
            if (daySchedules.isNotEmpty()) {
                TabRow(
                    selectedTabIndex = selectedDayIndex.coerceIn(0, daySchedules.size - 1),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    daySchedules.forEachIndexed { index, day ->
                        Tab(
                            selected = selectedDayIndex == index,
                            onClick = { selectedDayIndex = index },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "DAY ${day.dayNumber}",
                                        fontWeight = if (selectedDayIndex == index) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    if (day.dateStr.isNotBlank()) {
                                        Text(
                                            text = day.dateStr,
                                            fontSize = 11.sp,
                                            color = if (selectedDayIndex == index) MaterialTheme.colorScheme.primary else TextSecondary
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // [18. 일정이 없는 경우] 및 [3. 일정 화면] 목록
            if (selectedPlaces.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "선택한 장소가 없어요.",
                        fontSize = 16.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (currentDay.items.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "아직 일정이 없어요.",
                                    fontSize = 16.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        itemsIndexed(currentDay.items) { index, item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedItemForAction = item
                                    }
                                    .testTag("schedule_item_${item.id}"),
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

                            if (index < currentDay.items.size - 1) {
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

                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("add_place_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "+ 장소 추가",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // [5. 장소 추가 다이얼로그]
    if (showAddDialog) {
        var chosenPlace by remember { mutableStateOf(availablePlaces.firstOrNull()) }
        var inputTime by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "장소 추가",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (availablePlaces.isEmpty()) {
                        Text(
                            text = "선택한 모든 장소가 이미 일정에 추가되었습니다.",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            text = "장소 선택",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        availablePlaces.forEach { place ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { chosenPlace = place }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = chosenPlace?.id == place.id,
                                    onClick = { chosenPlace = place }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = place.name,
                                    fontSize = 15.sp,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "방문 시간",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        OutlinedTextField(
                            value = inputTime,
                            onValueChange = { inputTime = it },
                            placeholder = { Text("10:00") },
                            label = { Text("시간") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Done
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (availablePlaces.isNotEmpty()) {
                    Button(
                        onClick = {
                            val place = chosenPlace
                            if (place != null && inputTime.isNotBlank()) {
                                val newItem = ScheduleItem(
                                    id = "sched_${System.currentTimeMillis()}",
                                    tripId = tripId,
                                    placeId = place.id,
                                    placeName = place.name,
                                    category = place.categories.firstOrNull() ?: "관광",
                                    time = inputTime.trim(),
                                    memo = place.description,
                                    latitude = place.latitude,
                                    longitude = place.longitude,
                                    date = currentDay.dateStr
                                )
                                // [7. 시간순 자동 정렬]
                                val updatedItems = (currentDay.items + newItem)
                                    .sortedBy { PlaceDataProvider.timeToMinutes(it.time) }

                                daySchedules = daySchedules.map {
                                    if (it.dayNumber == currentDay.dayNumber) it.copy(items = updatedItems) else it
                                }
                                showAddDialog = false
                            }
                        },
                        enabled = chosenPlace != null && inputTime.isNotBlank()
                    ) {
                        Text("추가")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // [8. 일정 수정 & 9. 다른 날짜로 이동 다이얼로그]
    selectedItemForAction?.let { item ->
        var editTime by remember(item) { mutableStateOf(item.time) }
        var targetDayNumber by remember(item) { mutableIntStateOf(currentDay.dayNumber) }

        AlertDialog(
            onDismissRequest = { selectedItemForAction = null },
            title = {
                Text(
                    text = "일정 수정",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = item.placeName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = editTime,
                        onValueChange = { editTime = it },
                        label = { Text("방문 시간 (예: 14:00)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (daySchedules.size > 1) {
                        Text(
                            text = "날짜 변경 (이동)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            daySchedules.forEach { day ->
                                FilterChip(
                                    selected = targetDayNumber == day.dayNumber,
                                    onClick = { targetDayNumber = day.dayNumber },
                                    label = { Text("DAY ${day.dayNumber}") }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val updatedItems = currentDay.items.filter { it.id != item.id }
                            daySchedules = daySchedules.map {
                                if (it.dayNumber == currentDay.dayNumber) it.copy(items = updatedItems) else it
                            }
                            selectedItemForAction = null
                        }
                    ) {
                        Text("삭제", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            if (editTime.isNotBlank()) {
                                if (targetDayNumber != currentDay.dayNumber) {
                                    // 다른 날짜로 이동
                                    val sourceItems = currentDay.items.filter { it.id != item.id }
                                    val targetDay = daySchedules.firstOrNull { it.dayNumber == targetDayNumber }
                                    val movedItem = item.copy(time = editTime.trim(), date = targetDay?.dateStr ?: "")
                                    val targetItems = ((targetDay?.items ?: emptyList()) + movedItem)
                                        .sortedBy { PlaceDataProvider.timeToMinutes(it.time) }

                                    daySchedules = daySchedules.map {
                                        when (it.dayNumber) {
                                            currentDay.dayNumber -> it.copy(items = sourceItems)
                                            targetDayNumber -> it.copy(items = targetItems)
                                            else -> it
                                        }
                                    }
                                    selectedDayIndex = daySchedules.indexOfFirst { it.dayNumber == targetDayNumber }
                                        .coerceAtLeast(0)
                                } else {
                                    // 현재 날짜 내에서 시간 수정
                                    val updatedItems = currentDay.items.map {
                                        if (it.id == item.id) it.copy(time = editTime.trim()) else it
                                    }.sortedBy { PlaceDataProvider.timeToMinutes(it.time) }

                                    daySchedules = daySchedules.map {
                                        if (it.dayNumber == currentDay.dayNumber) it.copy(items = updatedItems) else it
                                    }
                                }
                                selectedItemForAction = null
                            }
                        }
                    ) {
                        Text("수정")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedItemForAction = null }) {
                    Text("취소")
                }
            }
        )
    }

    // [14. 일정 확정 전 미배치 장소 확인 다이얼로그]
    if (showUnplacedWarningDialog) {
        AlertDialog(
            onDismissRequest = { showUnplacedWarningDialog = false },
            title = {
                Text(
                    text = "일정 확정",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Text(
                    text = "아직 일정에 추가하지 않은 장소가 있어요.\n그래도 일정을 확정할까요?",
                    fontSize = 15.sp,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnplacedWarningDialog = false
                        onScheduleConfirmed(daySchedules)
                        onViewFullSchedule()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                ) {
                    Text("확정")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnplacedWarningDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}
