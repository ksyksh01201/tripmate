package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Trip
import com.example.ui.components.TripMateLogo
import com.example.ui.viewmodel.TripEditDone
import com.example.ui.viewmodel.TripEditViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// 피그마 "여행방 편집 화면" 색상
private val EditBlue = Color(0xFF4A90E2)
private val EditButtonBlue = Color(0xFF3A7BE0)
private val EditText = Color(0xFF000000)
private val EditGray = Color(0xFF727272)
private val EditBorder = Color(0xFFE5E5E5)
private val EditBackground = Color(0xFFFFFFFF)
private val DeleteRed = Color(0xFFE53935)
private val DeleteBackground = Color(0xFFFFF5F5)
private val DeleteBorder = Color(0xFFEAE0E0)
private val SuccessGreen = Color(0xFF4CAF50)
private val SuccessCircle = Color(0xFFDDF1DD)
private val CancelGray = Color(0xFFEEEEEE)

private enum class EditPopup { Saved, Leave, Delete }

/**
 * 여행방 편집 화면 (여행룸 오른쪽 위 [편집]).
 * 여행명·여행 날짜 수정 → [변경사항 저장] → "변경사항이 저장되었어요!" 팝업
 * [여행방 나가기] → 확인 팝업 → 나가면 홈으로
 * [여행 삭제] → 확인 팝업 → 삭제하면 홈으로 (방장만 가능)
 */
@Composable
fun TripEditScreen(
    trip: Trip,
    myUid: String?,
    onBack: () -> Unit,
    onExitTrip: () -> Unit
) {
    val viewModel: TripEditViewModel = viewModel(key = "trip_edit_${trip.tripId}")
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var title by rememberSaveable { mutableStateOf(trip.title) }
    var startMillis by rememberSaveable { mutableStateOf(trip.startDate.toUtcMillis()) }
    var endMillis by rememberSaveable { mutableStateOf(trip.endDate.toUtcMillis()) }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var pickerAtEnd by rememberSaveable { mutableStateOf(false) }
    var popup by rememberSaveable { mutableStateOf<EditPopup?>(null) }

    val newStart = startMillis?.toDateKey() ?: trip.startDate
    val newEnd = endMillis?.toDateKey() ?: trip.endDate
    val changed = title.trim() != trip.title || newStart != trip.startDate || newEnd != trip.endDate
    val canSave = changed && title.isNotBlank() && !state.isWorking

    BackHandler { if (!state.isWorking) onBack() }

    // 작업이 끝나면: 저장 → 완료 팝업 / 나가기·삭제 → 홈으로
    LaunchedEffect(state.done) {
        when (state.done) {
            TripEditDone.Saved -> popup = EditPopup.Saved
            TripEditDone.Left -> {
                Toast.makeText(context, "여행방에서 나왔어요", Toast.LENGTH_SHORT).show()
                onExitTrip()
            }
            TripEditDone.Deleted -> {
                Toast.makeText(context, "여행을 삭제했어요", Toast.LENGTH_SHORT).show()
                onExitTrip()
            }
            null -> Unit
        }
        if (state.done != null) viewModel.consumeDone()
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(EditBackground)
    ) {
        EditHeader(onBack = { if (!state.isWorking) onBack() })

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            // 여행명
            EditCard {
                Text("여행명", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EditText)
                Spacer(Modifier.height(12.dp))
                TitleField(value = title, onValueChange = { if (it.length <= 30) title = it })
            }
            Spacer(Modifier.height(16.dp))

            // 여행 날짜
            EditCard {
                Text("여행 날짜", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EditText)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DateBox(
                        text = startMillis?.toKoreanDate() ?: "시작일",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_start_date"),
                        onClick = { pickerAtEnd = false; pickerOpen = true }
                    )
                    Text(
                        "~",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EditText,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    DateBox(
                        text = endMillis?.toKoreanDate() ?: "종료일",
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_end_date"),
                        onClick = { pickerAtEnd = true; pickerOpen = true }
                    )
                }
            }
            Spacer(Modifier.height(28.dp))

            // ✓ 변경사항 저장
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canSave) EditBlue else EditBlue.copy(alpha = 0.4f))
                    .clickable(enabled = canSave) {
                        viewModel.save(trip.tripId, title, newStart, newEnd)
                    }
                    .testTag("edit_save"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isWorking) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("변경사항 저장", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(40.dp))

            // 여행방 나가기
            ActionRow(
                text = "여행방 나가기",
                icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = EditText, modifier = Modifier.size(22.dp)) },
                textColor = EditText,
                background = Color.White,
                border = EditBorder,
                chevronColor = EditGray,
                tag = "edit_leave",
                onClick = { if (!state.isWorking) popup = EditPopup.Leave }
            )
            Spacer(Modifier.height(32.dp))

            // 여행 삭제
            ActionRow(
                text = "여행 삭제",
                icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = DeleteRed, modifier = Modifier.size(24.dp)) },
                textColor = DeleteRed,
                background = DeleteBackground,
                border = DeleteBorder,
                chevronColor = DeleteRed,
                tag = "edit_delete",
                onClick = {
                    when {
                        state.isWorking -> Unit
                        trip.createdBy != myUid ->
                            Toast.makeText(context, "여행 삭제는 방장만 할 수 있어요", Toast.LENGTH_SHORT).show()
                        else -> popup = EditPopup.Delete
                    }
                }
            )
        }
    }

    if (pickerOpen) {
        KoreanDateRangePickerDialog(
            initialStart = startMillis,
            initialEnd = endMillis,
            startWithEnd = pickerAtEnd,
            todayMillis = remember { todayUtcMillis() },
            onDismiss = { pickerOpen = false },
            onConfirm = { s, e ->
                startMillis = s
                endMillis = e
                pickerOpen = false
            }
        )
    }

    when (popup) {
        EditPopup.Saved -> SavedPopup(onConfirm = { popup = null; onBack() })
        EditPopup.Leave -> ConfirmPopup(
            title = "여행방에서 나가시겠어요?",
            message = null,
            confirmText = "나가기",
            confirmColor = EditButtonBlue,
            isWorking = state.isWorking,
            onCancel = { popup = null },
            onConfirm = { viewModel.leave(trip) }
        )
        EditPopup.Delete -> ConfirmPopup(
            title = "정말 이 여행을 삭제하시겠어요?",
            message = "삭제하면 복구할 수 없어요.",
            confirmText = "삭제",
            confirmColor = DeleteRed,
            isWorking = state.isWorking,
            onCancel = { popup = null },
            onConfirm = { viewModel.delete(trip) }
        )
        null -> Unit
    }
}

/** 파란 상단: ← / tripmate / 여행방 편집 */
@Composable
private fun EditHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(EditBlue)
            .statusBarsPadding()
            .height(92.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .testTag("edit_back")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color.White)
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TripMateLogo(color = Color.White, fontSize = 18.sp)
            Text("여행방 편집", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EditCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, EditBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) { content() }
}

/** 여행명 입력칸 (오른쪽 ⓧ 로 지우기) */
@Composable
private fun TitleField(value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, EditBorder, RoundedCornerShape(10.dp))
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("여행 이름을 입력해 주세요", color = Color(0xFFB0B0B0), fontSize = 17.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = EditText, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(EditBlue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_title")
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onValueChange("") }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Cancel, contentDescription = "지우기", tint = Color(0xFF9E9E9E), modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** 📅 2026년 8월 1일 */
@Composable
private fun DateBox(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, EditBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = EditGray, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = EditText, maxLines = 1)
    }
}

/** [여행방 나가기 >] / [여행 삭제 >] */
@Composable
private fun ActionRow(
    text: String,
    icon: @Composable () -> Unit,
    textColor: Color,
    background: Color,
    border: Color,
    chevronColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(16.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = textColor, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = chevronColor)
    }
}

/** "변경사항이 저장되었어요!" 팝업 */
@Composable
private fun SavedPopup(onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onConfirm, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(SuccessCircle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text("변경사항이 저장되었어요!", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = EditText)
                Spacer(Modifier.height(18.dp))
                PopupButton(
                    text = "확인",
                    background = EditButtonBlue,
                    textColor = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("edit_saved_ok"),
                    onClick = onConfirm
                )
            }
        }
    }
}

/** 나가기 / 삭제 확인 팝업: [취소] [나가기 or 삭제] */
@Composable
private fun ConfirmPopup(
    title: String,
    message: String?,
    confirmText: String,
    confirmColor: Color,
    isWorking: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = { if (!isWorking) onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 36.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 30.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = EditText, textAlign = TextAlign.Center)
                if (message != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(message, fontSize = 13.sp, color = EditGray, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(if (message != null) 28.dp else 22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PopupButton(
                        text = "취소",
                        background = CancelGray,
                        textColor = EditGray,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("popup_cancel"),
                        enabled = !isWorking,
                        onClick = onCancel
                    )
                    PopupButton(
                        text = confirmText,
                        background = confirmColor,
                        textColor = Color.White,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("popup_confirm"),
                        enabled = !isWorking,
                        isLoading = isWorking,
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

@Composable
private fun PopupButton(
    text: String,
    background: Color,
    textColor: Color,
    modifier: Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = textColor, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else {
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

// ---------- 날짜 변환 (Firestore 는 "yyyy-MM-dd", 달력은 UTC 자정 밀리초) ----------

private fun utc(pattern: String) =
    SimpleDateFormat(pattern, Locale.KOREA).apply { timeZone = TimeZone.getTimeZone("UTC") }

private fun String.toUtcMillis(): Long? =
    runCatching { utc("yyyy-MM-dd").parse(this)?.time }.getOrNull()

private fun Long.toDateKey(): String = utc("yyyy-MM-dd").format(Date(this))

private fun Long.toKoreanDate(): String = utc("yyyy년 M월 d일").format(Date(this))

private fun todayUtcMillis(): Long {
    val local = Calendar.getInstance()
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
