package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.Schedule
import com.example.data.Trip
import com.example.data.TripDay
import com.example.data.TripDays
import com.example.ui.viewmodel.ScheduleViewModel

// 피그마 "일정 화면" 색상
private val ScheduleBlue = Color(0xFF4A90E2)
private val ScheduleText = Color(0xFF202020)
private val ScheduleGray = Color(0xFF727272)
private val ScheduleLine = Color(0xFFDCE6F2)
private val ScheduleCardBorder = Color(0xFFEEEEEE)

/**
 * 여행룸 하단 [일정] 탭 내용.
 * [1일차 | 2일차 | 3일차] → "8월 1일 토요일 / 지도 보기" → "N개의 일정" → 번호 타임라인 → [+ 일정 추가]
 */
@Composable
fun ScheduleContent(trip: Trip) {
    val viewModel: ScheduleViewModel = viewModel(
        key = "schedule_${trip.tripId}",
        factory = viewModelFactory { initializer { ScheduleViewModel(trip.tripId) } }
    )
    val schedules by viewModel.schedules.collectAsStateWithLifecycle()
    val loadError by viewModel.loadError.collectAsStateWithLifecycle()
    val editState by viewModel.editState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val days = remember(trip.startDate, trip.endDate) { TripDays.of(trip) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedDay = days.getOrNull(selectedIndex) ?: days.firstOrNull()
    var showAdd by rememberSaveable { mutableStateOf(false) }

    // 저장이 끝나면 팝업 닫기
    LaunchedEffect(editState.savedCount) { if (editState.savedCount > 0) showAdd = false }

    if (selectedDay == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("여행 날짜가 없어요.", color = ScheduleGray, fontSize = 14.sp)
        }
        return
    }

    val daySchedules = schedules.orEmpty().filter { it.day == selectedDay.index }

    Column(modifier = Modifier.fillMaxSize()) {
        DayTabs(
            days = days,
            selectedIndex = selectedDay.index,
            onSelect = { selectedIndex = it },
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)
        )
        Spacer(Modifier.height(22.dp))

        // 하루 요약: "8월 1일 토요일"  ……  [지도] 지도 보기
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(selectedDay.longDate, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ScheduleText)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        // TODO 카카오맵 키를 받으면 지도 화면으로 연결
                        Toast.makeText(context, "지도 보기는 카카오맵 연결 후 사용할 수 있어요", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("schedule_open_map"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Outlined.Map, contentDescription = null, tint = ScheduleBlue, modifier = Modifier.size(16.dp))
                Text("지도 보기", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = ScheduleBlue)
            }
        }
        Text(
            text = "${daySchedules.size}개의 일정",
            fontSize = 12.sp,
            color = ScheduleGray,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        loadError?.let {
            Text(it, color = Color(0xFFCF592D), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when {
                schedules == null -> CircularProgressIndicator(color = ScheduleBlue, modifier = Modifier.align(Alignment.Center))
                daySchedules.isEmpty() -> Text(
                    text = "아직 일정이 없어요.\n아래 [일정 추가]로 가고 싶은 곳을 넣어 보세요.",
                    color = ScheduleGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(daySchedules, key = { _, s -> s.scheduleId }) { i, schedule ->
                        ScheduleRow(
                            number = i + 1,
                            schedule = schedule,
                            isLast = i == daySchedules.lastIndex,
                            canMoveUp = i > 0,
                            canMoveDown = i < daySchedules.lastIndex,
                            onMoveUp = { viewModel.swap(schedule, daySchedules[i - 1]) },
                            onMoveDown = { viewModel.swap(schedule, daySchedules[i + 1]) },
                            onDelete = { viewModel.delete(schedule) }
                        )
                    }
                }
            }
        }

        // + 일정 추가
        Row(
            modifier = Modifier
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp)
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ScheduleBlue)
                .clickable { viewModel.clearError(); showAdd = true }
                .testTag("schedule_add"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
            Text("일정 추가", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showAdd) {
        AddScheduleDialog(
            day = selectedDay,
            isSaving = editState.isSaving,
            errorMessage = editState.errorMessage,
            onDismiss = { showAdd = false },
            onSave = { title, memo ->
                viewModel.addSchedule(Schedule(day = selectedDay.index, title = title, memo = memo))
            }
        )
    }
}

/** 상단 일차 탭. 3일 이하면 화면을 나눠 꽉 채우고, 더 길면 옆으로 스크롤 */
@Composable
private fun DayTabs(days: List<TripDay>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) { if (days.size > 3) listState.animateScrollToItem(selectedIndex) }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val gap = 10.dp
        val itemWidth = if (days.size <= 3) (maxWidth - gap * (days.size - 1).coerceAtLeast(0)) / days.size.coerceAtLeast(1) else 104.dp
        LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(gap)) {
            itemsIndexed(days, key = { _, d -> d.index }) { _, day ->
                val selected = day.index == selectedIndex
                Column(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) ScheduleBlue else Color.White)
                        .border(1.dp, if (selected) ScheduleBlue else ScheduleLine, RoundedCornerShape(10.dp))
                        .clickable { onSelect(day.index) }
                        .testTag("schedule_day_${day.index}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
                ) {
                    Text(day.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else ScheduleText)
                    Text(day.shortDate, fontSize = 12.sp, color = if (selected) Color.White else ScheduleGray)
                }
            }
        }
    }
}

/** 번호 + 타임라인 점/선 + 일정 카드(⋮ 메뉴) */
@Composable
private fun ScheduleRow(
    number: Int,
    schedule: Schedule,
    isLast: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 순번 + 타임라인
        Column(
            modifier = Modifier
                .width(46.dp)
                .fillMaxHeight()
                .padding(top = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("$number", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ScheduleGray)
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(ScheduleBlue)
            )
            if (!isLast) {
                Box(
                    Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(ScheduleLine)
                )
            }
        }

        // 일정 카드
        var menuOpen by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 72.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color.White)
                .border(1.dp, ScheduleCardBorder, RoundedCornerShape(15.dp))
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(end = 40.dp)) {
                Text(
                    schedule.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScheduleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = schedule.address.ifBlank { schedule.memo }
                if (sub.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(sub, fontSize = 12.sp, color = ScheduleGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 4.dp)
                        .testTag("schedule_menu_${schedule.scheduleId}")
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "일정 메뉴", tint = ScheduleGray, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (canMoveUp) DropdownMenuItem(text = { Text("위로 이동") }, onClick = { menuOpen = false; onMoveUp() })
                    if (canMoveDown) DropdownMenuItem(text = { Text("아래로 이동") }, onClick = { menuOpen = false; onMoveDown() })
                    DropdownMenuItem(
                        text = { Text("삭제", color = Color(0xFFCF592D)) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

/** [+ 일정 추가] 팝업: 장소/할 일 + 메모 */
@Composable
private fun AddScheduleDialog(
    day: TripDay,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (title: String, memo: String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var memo by rememberSaveable { mutableStateOf("") }
    val canSave = title.isNotBlank() && !isSaving

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                DialogHeader(title = "${day.label} 일정 추가", onClose = onDismiss, closeEnabled = !isSaving)
                Spacer(Modifier.height(4.dp))
                Text(
                    day.longDate,
                    fontSize = 13.sp,
                    color = ScheduleGray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))

                FieldLabel("장소 또는 할 일")
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 40) title = it },
                    placeholder = { Text("예: 부산역, 해운대 해수욕장") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = scheduleFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_title")
                )
                Spacer(Modifier.height(16.dp))

                FieldLabel("메모 (선택)")
                OutlinedTextField(
                    value = memo,
                    onValueChange = { if (it.length <= 100) memo = it },
                    placeholder = { Text("예: 10시 KTX 도착") },
                    shape = RoundedCornerShape(10.dp),
                    colors = scheduleFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_memo")
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(errorMessage, color = Color(0xFFCF592D), fontSize = 13.sp)
                }
                Spacer(Modifier.height(24.dp))
                PrimaryDialogButton(
                    text = "추가하기",
                    enabled = canSave,
                    isLoading = isSaving,
                    testTag = "schedule_save",
                    onClick = { onSave(title, memo) }
                )
            }
        }
    }
}

@Composable
private fun scheduleFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = ScheduleBlue,
    unfocusedBorderColor = ScheduleLine,
    cursorColor = ScheduleBlue
)
