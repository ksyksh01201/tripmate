package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val CalBlue = Color(0xFF4A90E2)
private val CalRange = Color(0xFFE3EEFC)
private val CalDisabled = Color(0xFFBDBDBD)
private val CalText = Color(0xFF1E2022)
private val CalSub = Color(0xFF606060)
private val CalSunday = Color(0xFFE57373)

private const val DAY_MILLIS = 24L * 60 * 60 * 1000
private const val MONTHS_TO_SHOW = 24
private const val MONTHS_BEFORE = 12
private val WeekdayLabels = listOf("일", "월", "화", "수", "목", "금", "토")

/**
 * 한국어 여행 날짜 선택 달력 (시작일 ~ 종료일).
 * - 기기 언어와 상관없이 요일/월/문구를 한국어로 표시
 * - 한 달씩 보여주고, 위아래로 밀거나 ^ 는 이전 달, ∨ 는 다음 달로 이동
 * - 요일 줄과 월 제목은 눌러지지 않고, 오늘 이전 날짜도 고를 수 없음
 * - 날짜를 하나도 고르지 않으면 [확인]은 비활성, [취소]만 누를 수 있음
 *
 * 날짜는 DatePicker와 같은 "UTC 자정 기준 밀리초"로 주고받습니다.
 */
@Composable
internal fun KoreanDateRangePickerDialog(
    initialStart: Long?,
    initialEnd: Long?,
    todayMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (start: Long, end: Long) -> Unit
) {
    var start by remember { mutableStateOf(initialStart) }
    var end by remember { mutableStateOf(initialEnd) }

    val months = remember(todayMillis) {
        val today = utcCalendar().apply { timeInMillis = todayMillis }
        // 지난 달들도 넘겨 볼 수 있게 앞쪽에 몇 달을 둡니다. (지난 날짜는 선택 불가)
        (-MONTHS_BEFORE until MONTHS_TO_SHOW).map {
            monthInfo(today.get(Calendar.YEAR), today.get(Calendar.MONTH), it)
        }
    }
    // 이미 고른 날짜가 있으면 그 달을, 없으면 이번 달을 먼저 보여줍니다.
    val firstPage = remember {
        initialStart?.let { s ->
            val c = utcCalendar().apply { timeInMillis = s }
            months.indexOfFirst { it.year == c.get(Calendar.YEAR) && it.month == c.get(Calendar.MONTH) }
        }?.takeIf { it >= 0 } ?: MONTHS_BEFORE
    }
    val pagerState = rememberPagerState(initialPage = firstPage) { months.size }
    val scope = rememberCoroutineScope()

    val s = start
    val e = end
    val headline = "${s?.let(::koreanDate) ?: "시작일"} - ${e?.let(::koreanDate) ?: "종료일"}"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.White
        ) {
            Column {
                Text(
                    text = "여행 날짜를 선택하세요",
                    modifier = Modifier.padding(start = 24.dp, top = 20.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CalSub
                )
                Text(
                    text = headline,
                    modifier = Modifier.padding(start = 24.dp, top = 6.dp, bottom = 14.dp),
                    fontSize = 24.sp,
                    color = CalText
                )
                HorizontalDivider(color = Color(0xFFE0E3E8))

                // 요일 줄 (글자만, 클릭 안 됨)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    WeekdayLabels.forEachIndexed { i, label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = when (i) {
                                0 -> CalSunday
                                6 -> CalBlue
                                else -> CalSub
                            }
                        )
                    }
                }

                // 한 화면에 한 달씩. 위아래로 밀거나 ^ / ∨ 를 누르면 이전 달 / 다음 달로 넘어갑니다.
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                        .padding(horizontal = 12.dp)
                ) { page ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        MonthArrow(
                            icon = Icons.Filled.KeyboardArrowUp,
                            description = "이전 달",
                            enabled = page > 0,
                            tag = "calendar_prev_month",
                            onClick = { scope.launch { pagerState.animateScrollToPage(page - 1) } }
                        )
                        MonthBlock(
                            month = months[page],
                            todayMillis = todayMillis,
                            start = s,
                            end = e,
                            onDayClick = { day ->
                                when {
                                    // 처음 고르거나, 이미 둘 다 골랐으면 새로 시작
                                    s == null || e != null -> { start = day; end = null }
                                    // 시작일보다 앞을 누르면 시작일을 바꿈
                                    day < s -> start = day
                                    else -> end = day
                                }
                            }
                        )
                        MonthArrow(
                            icon = Icons.Filled.KeyboardArrowDown,
                            description = "다음 달",
                            enabled = page < months.lastIndex,
                            tag = "calendar_next_month",
                            onClick = { scope.launch { pagerState.animateScrollToPage(page + 1) } }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 12.dp, top = 4.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("취소", color = CalBlue, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        enabled = s != null,
                        // 종료일을 안 고르면 당일치기로 봅니다.
                        onClick = { s?.let { onConfirm(it, e ?: it) } }
                    ) {
                        Text("확인", color = if (s != null) CalBlue else CalDisabled, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthArrow(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.testTag(tag)) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = if (enabled) CalSub else Color.Transparent,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun MonthBlock(
    month: MonthInfo,
    todayMillis: Long,
    start: Long?,
    end: Long?,
    onDayClick: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 월 제목 (글자만, 클릭 안 됨)
        Text(
            text = "${month.year}년 ${month.month + 1}월",
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = CalText
        )
        // 달마다 높이가 같도록 항상 6줄로 그립니다.
        for (r in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val dayNum = r * 7 + c - month.leadingBlanks + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..month.dayCount) {
                            DayCell(
                                day = dayNum,
                                millis = month.firstDayMillis + (dayNum - 1) * DAY_MILLIS,
                                todayMillis = todayMillis,
                                start = start,
                                end = end,
                                onClick = onDayClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    millis: Long,
    todayMillis: Long,
    start: Long?,
    end: Long?,
    onClick: (Long) -> Unit
) {
    val enabled = millis >= todayMillis
    val isEdge = millis == start || millis == end
    val isToday = millis == todayMillis
    val hasRange = start != null && end != null && end > start
    // 시작일~종료일 사이를 연한 파란 띠로 이어 줍니다.
    val leftOn = hasRange && millis > start!! && millis <= end!!
    val rightOn = hasRange && millis >= start!! && millis < end!!

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (leftOn) CalRange else Color.Transparent)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (rightOn) CalRange else Color.Transparent)
        )
    }

    var circle = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(if (isEdge) CalBlue else Color.Transparent)
    if (isToday && !isEdge) circle = circle.border(1.dp, CalBlue, CircleShape)

    Box(
        modifier = circle.clickable(enabled = enabled) { onClick(millis) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$day",
            fontSize = 15.sp,
            color = when {
                isEdge -> Color.White
                !enabled -> CalDisabled
                isToday -> CalBlue
                else -> CalText
            }
        )
    }
}

private class MonthInfo(
    val year: Int,
    val month: Int, // 0 = 1월
    val firstDayMillis: Long,
    val leadingBlanks: Int, // 1일 앞의 빈 칸 수 (일요일 시작)
    val dayCount: Int
)

private fun utcCalendar(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

private fun monthInfo(baseYear: Int, baseMonth: Int, offset: Int): MonthInfo {
    val cal = utcCalendar().apply {
        clear()
        set(baseYear, baseMonth, 1)
        add(Calendar.MONTH, offset)
    }
    return MonthInfo(
        year = cal.get(Calendar.YEAR),
        month = cal.get(Calendar.MONTH),
        firstDayMillis = cal.timeInMillis,
        leadingBlanks = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY,
        dayCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    )
}

private fun koreanDate(millis: Long): String {
    val c = utcCalendar().apply { timeInMillis = millis }
    return String.format(
        Locale.KOREA, "%d.%02d.%02d",
        c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH)
    )
}
