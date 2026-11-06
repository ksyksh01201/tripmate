package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CompanionType
import com.example.data.TravelCities
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val Blue = Color(0xFF4A90E2)
private val FieldBorder = Color(0xFFE0E3E8)
private val Placeholder = Color(0xFF9E9E9E)
private val LabelColor = Color(0xFF1E2022)

/**
 * "새 여행 만들기" 팝업
 * - 여행 도시: DropdownMenu
 * - 여행 날짜: 달력 아이콘 → DateRangePicker로 시작일/종료일 선택
 * - 누구와 함께하나요?: 칩 6개 중 1개
 * 세 항목이 모두 채워져야 [여행 생성하기] 버튼이 활성화됩니다.
 *
 * @param onCreate (도시, 시작일 "yyyy-MM-dd", 종료일 "yyyy-MM-dd", 동행자 유형)
 */
@Composable
fun CreateTripDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (destination: String, startDate: String, endDate: String, companion: CompanionType) -> Unit
) {
    var destination by rememberSaveable { mutableStateOf<String?>(null) }
    var startMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var endMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var companion by rememberSaveable { mutableStateOf<CompanionType?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val canSubmit = destination != null && startMillis != null && endMillis != null &&
        companion != null && !isSubmitting

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
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
                DialogHeader(title = "새 여행 만들기", onClose = onDismiss, closeEnabled = !isSubmitting)

                Spacer(Modifier.height(24.dp))

                // 1) 여행 도시
                FieldLabel("여행 도시")
                CityDropdown(
                    selected = destination,
                    onSelect = { destination = it }
                )

                Spacer(Modifier.height(20.dp))

                // 2) 여행 날짜
                FieldLabel("여행 날짜")
                DateRangeField(
                    startMillis = startMillis,
                    endMillis = endMillis,
                    onClick = { showDatePicker = true }
                )

                Spacer(Modifier.height(20.dp))

                // 3) 누구와 함께하나요?
                FieldLabel("누구와 함께하나요?")
                CompanionChips(
                    selected = companion,
                    onSelect = { companion = it }
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFE5484D),
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(28.dp))

                PrimaryDialogButton(
                    text = "여행 생성하기",
                    enabled = canSubmit,
                    isLoading = isSubmitting,
                    testTag = "create_trip_submit_button",
                    onClick = {
                        onCreate(
                            destination!!,
                            startMillis!!.toDateString(),
                            endMillis!!.toDateString(),
                            companion!!
                        )
                    }
                )
            }
        }
    }

    if (showDatePicker) {
        TripDateRangePickerDialog(
            initialStart = startMillis,
            initialEnd = endMillis,
            onDismiss = { showDatePicker = false },
            onConfirm = { start, end ->
                startMillis = start
                endMillis = end
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun CityDropdown(selected: String?, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SelectBox(
            text = selected ?: "어디로 떠나시나요?",
            isPlaceholder = selected == null,
            trailing = {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "도시 선택", tint = Placeholder)
            },
            onClick = { expanded = true },
            modifier = Modifier.testTag("create_trip_city_field")
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .heightIn(max = 280.dp)
                .background(Color.White)
        ) {
            TravelCities.domestic.forEach { city ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = city,
                            fontWeight = if (city == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (city == selected) Blue else LabelColor
                        )
                    },
                    onClick = {
                        onSelect(city)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DateRangeField(startMillis: Long?, endMillis: Long?, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectBox(
            text = startMillis?.toDisplayDate() ?: "시작일",
            isPlaceholder = startMillis == null,
            trailing = {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "달력 열기", tint = Blue)
            },
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .testTag("create_trip_start_date_field")
        )
        Text(
            text = "~",
            modifier = Modifier.padding(horizontal = 8.dp),
            color = Placeholder
        )
        SelectBox(
            text = endMillis?.toDisplayDate() ?: "종료일",
            isPlaceholder = endMillis == null,
            trailing = {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = "달력 열기", tint = Blue)
            },
            onClick = onClick,
            modifier = Modifier
                .weight(1f)
                .testTag("create_trip_end_date_field")
        )
    }
}

@Composable
private fun CompanionChips(selected: CompanionType?, onSelect: (CompanionType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CompanionType.entries.chunked(3).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowItems.forEach { type ->
                    val isSelected = type == selected
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelect(type) },
                        label = {
                            Text(
                                text = type.label,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("companion_chip_${type.name}"),
                        shape = RoundedCornerShape(22.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.White,
                            labelColor = LabelColor,
                            selectedContainerColor = Blue,
                            selectedLabelColor = Color.White
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = FieldBorder,
                            selectedBorderColor = Blue
                        )
                    )
                }
            }
        }
    }
}

/** 달력 팝업: 시작일과 종료일을 한 번에 고릅니다. 오늘 이전 날짜는 고를 수 없습니다. (화면은 KoreanDateRangePicker.kt) */
@Composable
private fun TripDateRangePickerDialog(
    initialStart: Long?,
    initialEnd: Long?,
    onDismiss: () -> Unit,
    onConfirm: (start: Long, end: Long) -> Unit
) {
    val todayUtcMillis = remember { todayAsUtcMillis() }
    KoreanDateRangePickerDialog(
        initialStart = initialStart,
        initialEnd = initialEnd,
        todayMillis = todayUtcMillis,
        onDismiss = onDismiss,
        onConfirm = onConfirm
    )
}

// ---------- 두 팝업에서 함께 쓰는 작은 컴포넌트 ----------

@Composable
internal fun DialogHeader(title: String, onClose: () -> Unit, closeEnabled: Boolean = true) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        IconButton(
            onClick = onClose,
            enabled = closeEnabled,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(32.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Placeholder)
        }
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = LabelColor,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
internal fun PrimaryDialogButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Blue,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFD5D9DF),
            disabledContentColor = Color.White
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SelectBox(
    text: String,
    isPlaceholder: Boolean,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, FieldBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            color = if (isPlaceholder) Placeholder else LabelColor,
            maxLines = 1
        )
        Spacer(Modifier.width(4.dp))
        trailing()
    }
}

// ---------- 날짜 변환 ----------
// DatePicker는 날짜를 "UTC 자정 기준 밀리초"로 주기 때문에 UTC로 변환해야 하루 밀리지 않습니다.

private fun utcFormat(pattern: String) =
    SimpleDateFormat(pattern, Locale.KOREA).apply { timeZone = TimeZone.getTimeZone("UTC") }

private fun Long.toDateString(): String = utcFormat("yyyy-MM-dd").format(Date(this))

private fun Long.toDisplayDate(): String = utcFormat("yyyy.MM.dd").format(Date(this))

private fun todayAsUtcMillis(): Long {
    val local = Calendar.getInstance()
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
