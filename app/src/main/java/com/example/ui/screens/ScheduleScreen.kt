package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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
private val ScheduleSheetBackground = Color(0xFFFCFCFC)
private val ScheduleDeleteRed = Color(0xFFE53935)

/**
 * 여행룸 하단 [일정] 탭 내용.
 * 기본: [1일차 | 2일차 | 3일차] → "8월 1일 토요일 · N개의 일정" → 번호 타임라인 → [+ 일정 추가]
 * 일정 시트를 아래로 스와이프하면 위쪽에 지도가 펼쳐지고, 그날 일정이 순서대로 이동경로로 표시됩니다.
 * 일정 카드를 길게 누르면 파랗게 바뀌고 끌어서 순서를 바꿀 수 있습니다.
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

    val days = remember(trip.startDate, trip.endDate) { TripDays.of(trip) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedDay = days.getOrNull(selectedIndex) ?: days.firstOrNull()
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showMap by rememberSaveable { mutableStateOf(false) }

    // 저장이 끝나면 팝업 닫기
    LaunchedEffect(editState.savedCount) { if (editState.savedCount > 0) showAdd = false }

    if (selectedDay == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("여행 날짜가 없어요.", color = ScheduleGray, fontSize = 14.sp)
        }
        return
    }

    val daySchedules = schedules.orEmpty().filter { it.day == selectedDay.index }
    // 끌어서 순서를 바꾸는 동안 화면에 보여 줄 목록 (놓으면 Firestore에 저장)
    var ordered by remember(daySchedules) { mutableStateOf(daySchedules) }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val dragState = remember(listState) { ScheduleDragState(listState, scope) }
    dragState.onMove = { from, to ->
        ordered = ordered.toMutableList().apply { add(to, removeAt(from)) }
    }
    dragState.onDrop = { if (ordered.map { it.scheduleId } != daySchedules.map { it.scheduleId }) viewModel.reorder(ordered) }
    LaunchedEffect(dragState) { dragState.runAutoScroll() }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val maxOffsetPx = constraints.maxHeight * MAP_OPEN_RATIO
        var sheetOffset by remember { mutableFloatStateOf(0f) }
        var mapActivated by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { snapshotFlow { sheetOffset > 0f }.collect { if (it) mapActivated = true } }

        // 지도를 열고 닫을 때 손을 놓으면 가까운 쪽으로 붙기
        suspend fun settle(velocity: Float) {
            val target = when {
                velocity > 800f -> maxOffsetPx
                velocity < -800f -> 0f
                sheetOffset > maxOffsetPx / 2 -> maxOffsetPx
                else -> 0f
            }
            animate(sheetOffset, target, initialVelocity = velocity) { value, _ -> sheetOffset = value }
        }

        // 목록 맨 위에서 아래로 당기면 지도가 열리고, 위로 밀면 다시 닫힘
        val nestedScroll = remember(maxOffsetPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < 0 && sheetOffset > 0f) {
                        val before = sheetOffset
                        sheetOffset = (sheetOffset + available.y).coerceAtLeast(0f)
                        return Offset(0f, sheetOffset - before)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 0 && source == NestedScrollSource.UserInput && !dragState.isDragging) {
                        val before = sheetOffset
                        sheetOffset = (sheetOffset + available.y).coerceAtMost(maxOffsetPx)
                        return Offset(0f, sheetOffset - before)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (sheetOffset > 0f && sheetOffset < maxOffsetPx) {
                        settle(available.y)
                        return available
                    }
                    return Velocity.Zero
                }
            }
        }
        val headerDrag = rememberDraggableState { delta ->
            sheetOffset = (sheetOffset + delta).coerceIn(0f, maxOffsetPx)
        }

        // 뒤쪽 지도 (처음 내려 볼 때 만들어짐)
        if (mapActivated) {
            ScheduleRouteMap(
                places = ordered.filter { it.lat != null && it.lng != null },
                allSchedules = ordered,
                destination = trip.destination,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { maxOffsetPx.toDp() } + 28.dp)
            )
        }

        // 일정 시트
        Column(
            modifier = Modifier
                .fillMaxSize()
                .layout { measurable, constraints ->
                    val off = sheetOffset.roundToInt().coerceIn(0, constraints.maxHeight)
                    val h = constraints.maxHeight - off
                    val placeable = measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
                    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, off) }
                }
                .shadow(if (sheetOffset > 0f) 8.dp else 0.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(ScheduleSheetBackground)
                .nestedScroll(nestedScroll)
        ) {
            // 손잡이 + 일차 탭 + 하루 요약: 여기를 위아래로 끌어서 지도 열기/닫기
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .draggable(
                        state = headerDrag,
                        orientation = Orientation.Vertical,
                        onDragStopped = { velocity -> settle(velocity) }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scope.launch { settle(if (sheetOffset > 0f) -1000f else 1000f) }
                        }
                        .testTag("schedule_sheet_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(2) {
                            Box(
                                Modifier
                                    .width(30.dp)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(Color(0xFFB5B5B5))
                            )
                        }
                    }
                }
                DayTabs(
                    days = days,
                    selectedIndex = selectedDay.index,
                    onSelect = { selectedIndex = it },
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp)
                )
                Spacer(Modifier.height(18.dp))

                // 하루 요약: "8월 1일 토요일"  ……  "3개의 일정"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(selectedDay.longDate, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ScheduleText)
                    Text("${ordered.size}개의 일정", fontSize = 12.sp, color = ScheduleGray)
                }
            }

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
                    ordered.isEmpty() -> Text(
                        text = "아직 일정이 없어요.\n아래 [일정 추가]로 가고 싶은 곳을 넣어 보세요.",
                        color = ScheduleGray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .verticalScroll(rememberScrollState())
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(dragState) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { dragState.onDragStart(it) },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragState.onDrag(amount.y)
                                    },
                                    onDragEnd = { dragState.onDragEnd() },
                                    onDragCancel = { dragState.onDragEnd() }
                                )
                            },
                        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(ordered, key = { _, s -> s.scheduleId }) { i, schedule ->
                            val dragging = dragState.draggingIndex == i
                            val itemModifier = if (dragging) {
                                Modifier
                                    .zIndex(1f)
                                    .graphicsLayer { translationY = dragState.draggingItemOffset }
                            } else {
                                Modifier.animateItem()
                            }
                            ScheduleRow(
                                number = i + 1,
                                schedule = schedule,
                                isLast = i == ordered.lastIndex,
                                dragging = dragging,
                                onDelete = { viewModel.delete(schedule) },
                                modifier = itemModifier
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
    }

    if (showAdd) {
        AddScheduleDialog(
            day = selectedDay,
            isSaving = editState.isSaving,
            errorMessage = editState.errorMessage,
            onDismiss = { showAdd = false },
            onSave = { title, memo ->
                viewModel.addSchedule(Schedule(day = selectedDay.index, title = title, memo = memo))
            },
            onOpenMap = { showAdd = false; showMap = true }
        )
    }

    if (showMap) {
        MapPickerDialog(
            day = selectedDay,
            destination = trip.destination,
            daySchedules = daySchedules,
            isSaving = editState.isSaving,
            onAddPlace = { place ->
                viewModel.addSchedule(
                    Schedule(
                        day = selectedDay.index,
                        title = place.name,
                        address = place.address,
                        lat = place.lat,
                        lng = place.lng,
                        placeId = place.id
                    )
                )
            },
            onDismiss = { showMap = false }
        )
    }
}

/** 지도가 펼쳐졌을 때 화면 높이 중 지도가 차지하는 비율 */
private const val MAP_OPEN_RATIO = 0.5f

/**
 * 길게 눌러 끌어서 순서 바꾸기 상태.
 * (Compose 공식 예제 LazyColumnDragAndDropDemo 방식: 끄는 카드 가운데가 다른 카드 위로 가면 자리 교체,
 *  목록 끝에 닿으면 자동 스크롤)
 */
private class ScheduleDragState(
    private val listState: LazyListState,
    private val scope: CoroutineScope
) {
    var onMove: (Int, Int) -> Unit = { _, _ -> }
    var onDrop: () -> Unit = {}

    var draggingIndex by mutableStateOf<Int?>(null)
        private set
    private var draggedDelta by mutableFloatStateOf(0f)
    private var initialOffset by mutableIntStateOf(0)
    private val scrollChannel = Channel<Float>(Channel.CONFLATED)

    val isDragging: Boolean get() = draggingIndex != null

    private val draggingItemInfo: LazyListItemInfo?
        get() = draggingIndex?.let { idx -> listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == idx } }

    val draggingItemOffset: Float
        get() = draggingItemInfo?.let { initialOffset + draggedDelta - it.offset } ?: 0f

    fun onDragStart(position: Offset) {
        listState.layoutInfo.visibleItemsInfo
            .firstOrNull { position.y.toInt() in it.offset..(it.offset + it.size) }
            ?.let {
                draggingIndex = it.index
                initialOffset = it.offset
                draggedDelta = 0f
            }
    }

    fun onDrag(dy: Float) {
        draggedDelta += dy
        val item = draggingItemInfo ?: return
        val start = initialOffset + draggedDelta
        val end = start + item.size
        val middle = start + item.size / 2f
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            middle.toInt() in it.offset..(it.offset + it.size) && it.index != item.index
        }
        if (target != null) {
            if (item.index == listState.firstVisibleItemIndex || target.index == listState.firstVisibleItemIndex) {
                scope.launch { listState.scrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            }
            onMove(item.index, target.index)
            draggingIndex = target.index
        } else {
            val overscroll = when {
                draggedDelta > 0 -> (end - listState.layoutInfo.viewportEndOffset).coerceAtLeast(0f)
                draggedDelta < 0 -> (start - listState.layoutInfo.viewportStartOffset).coerceAtMost(0f)
                else -> 0f
            }
            if (overscroll != 0f) scrollChannel.trySend(overscroll)
        }
    }

    fun onDragEnd() {
        if (draggingIndex != null) onDrop()
        draggingIndex = null
        draggedDelta = 0f
        initialOffset = 0
    }

    suspend fun runAutoScroll() {
        for (diff in scrollChannel) listState.scrollBy(diff)
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

/** 번호 + 타임라인 점/선 + 일정 카드(⋮ → 삭제). 끄는 중이면 카드가 파랗게 바뀜 */
@Composable
private fun ScheduleRow(
    number: Int,
    schedule: Schedule,
    isLast: Boolean,
    dragging: Boolean,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor by animateColorAsState(if (dragging) ScheduleBlue else Color.White, label = "schedule_card")
    val titleColor = if (dragging) Color.White else ScheduleText
    val subColor = if (dragging) Color.White.copy(alpha = 0.85f) else ScheduleGray

    Row(
        modifier = modifier
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
                .shadow(if (dragging) 6.dp else 0.dp, RoundedCornerShape(15.dp))
                .clip(RoundedCornerShape(15.dp))
                .background(cardColor)
                .border(1.dp, if (dragging) ScheduleBlue else ScheduleCardBorder, RoundedCornerShape(15.dp))
                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(end = 40.dp)) {
                Text(
                    schedule.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sub = schedule.address.ifBlank { schedule.memo }
                if (sub.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(sub, fontSize = 12.sp, color = subColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
                    Icon(Icons.Filled.MoreVert, contentDescription = "일정 메뉴", tint = subColor, modifier = Modifier.size(16.dp))
                }
                // ⋮ 를 누르면 작게 [삭제] 만 표시
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    shape = RoundedCornerShape(10.dp),
                    containerColor = Color.White
                ) {
                    DropdownMenuItem(
                        text = { Text("삭제", color = ScheduleDeleteRed, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                        onClick = { menuOpen = false; onDelete() },
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("schedule_delete_${schedule.scheduleId}")
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
    onSave: (title: String, memo: String) -> Unit,
    onOpenMap: () -> Unit
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
                Spacer(Modifier.height(16.dp))

                // 카카오맵에서 장소 검색해서 추가
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(1.dp, ScheduleBlue, RoundedCornerShape(10.dp))
                        .clickable(enabled = !isSaving, onClick = onOpenMap)
                        .testTag("schedule_add_from_map"),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Map, contentDescription = null, tint = ScheduleBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("지도에서 장소 검색하기", color = ScheduleBlue, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
                Text("또는 직접 입력", fontSize = 12.sp, color = ScheduleGray)
                Spacer(Modifier.height(12.dp))

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
