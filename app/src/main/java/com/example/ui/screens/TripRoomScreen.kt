package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.Trip
import com.example.ui.components.TripMateLogo
import com.example.ui.viewmodel.TripRoomUiState
import com.example.ui.viewmodel.TripRoomViewModel

private val RoomBlue = Color(0xFF4A90E2)
private val RoomBlueLight = Color(0xFFE3EEFC)
private val RoomGray = Color(0xFF808080)
private val HeaderBlue = Color(0xFF5B8DE3)
private val RoomBackground = Color(0xFFF7F8FA)
private val CodeBoxBlue = Color(0xFFEAF2FD)
private val CopyButtonBlue = Color(0xFF6F9DE6)
private val AvatarColors = listOf(
    Color(0xFF1E2B3E), Color(0xFFFA6B43), Color(0xFF4A90E2),
    Color(0xFF34C38F), Color(0xFF9B6BDF), Color(0xFFF5A623)
)

/**
 * 여행룸 화면.
 * Firestore의 members 수를 실시간으로 보고 화면을 바꿉니다.
 $1초대 코드 카드 (파란 상단 + 하단 탭은 공통)
 * - members.size >= 2 : "나의 여행 스타일은?" 밸런스 게임
 */
@Composable
fun TripRoomScreen(
    tripId: String,
    onBack: () -> Unit,
    onTravelStyleSelected: (TravelStyle) -> Unit = {}
) {
    val viewModel: TripRoomViewModel = viewModel(
        key = "trip_room_$tripId",
        factory = viewModelFactory { initializer { TripRoomViewModel(tripId) } }
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val loaded = uiState as? TripRoomUiState.Loaded

    val copyInviteCode: (String) -> Unit = { code ->
        clipboard.setText(AnnotatedString(code))
        Toast.makeText(context, "초대 코드가 복사되었어요", Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RoomBackground)
    ) {
        TripRoomHeader(
            trip = loaded?.trip,
            myUid = loaded?.myUid,
            onBack = onBack,
            onEdit = { Toast.makeText(context, "편집 기능은 준비 중이에요", Toast.LENGTH_SHORT).show() },
            onInvite = { loaded?.trip?.inviteCode?.let(copyInviteCode) }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                TripRoomUiState.Loading -> CircularProgressIndicator(color = RoomBlue)
                TripRoomUiState.NotFound -> CenterMessage("여행을 찾을 수 없어요.\n삭제되었거나 잘못된 링크일 수 있어요.")
                is TripRoomUiState.Error -> CenterMessage(state.message)
                is TripRoomUiState.Loaded -> {
                    // 인원수가 바뀌면 부드럽게 화면 전환
                    AnimatedContent(
                        targetState = state.trip.isWaitingForMates,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        modifier = Modifier.fillMaxSize(),
                        label = "trip_room_branch"
                    ) { waiting ->
                        if (waiting) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                InviteMatesCard(
                                    inviteCode = state.trip.inviteCode,
                                    onCopy = { copyInviteCode(state.trip.inviteCode) }
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                BalanceGameContent(onSelected = onTravelStyleSelected)
                            }
                        }
                    }
                }
            }
        }

        TripRoomBottomBar(
            onNotReady = { name ->
                Toast.makeText(context, "$name 기능은 준비 중이에요", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/** 파란 상단 영역: ← tripmate 편집 / 여행 제목 / 날짜 / 멤버 + 초대 + 현재 N명 */
@Composable
private fun TripRoomHeader(
    trip: Trip?,
    myUid: String?,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onInvite: () -> Unit
) {
    val shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape)
            .clip(shape)
            .background(HeaderBlue)
            .statusBarsPadding()
            .padding(bottom = 22.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .testTag("trip_room_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color.White)
            }
            TripMateLogo(color = Color.White, fontSize = 22.sp)
            Text(
                text = "편집",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("trip_room_edit_button"),
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(modifier = Modifier.padding(horizontal = 28.dp)) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = trip?.title.orEmpty(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = trip?.let { koreanDateRange(it.startDate, it.endDate) }.orEmpty(),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(18.dp))
            if (trip != null) {
                MemberRow(members = trip.members, myUid = myUid, onInvite = onInvite)
            }
        }
    }
}

/** 멤버 동그라미 + 점선 (+) 초대 버튼 + "현재 N명" */
@Composable
private fun MemberRow(members: List<String>, myUid: String?, onInvite: () -> Unit) {
    val maxShown = 4
    Row(
        modifier = Modifier.testTag("trip_room_member_avatars"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        members.take(maxShown).forEachIndexed { index, uid ->
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AvatarColors[index % AvatarColors.size]),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uid == myUid) "나" else "${index + 1}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        // 점선 원 (+) : 누르면 초대 코드 복사
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .clickable(onClick = onInvite)
                .drawBehind {
                    drawCircle(
                        color = Color.White,
                        radius = size.minDimension / 2 - 1.dp.toPx(),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                        )
                    )
                }
                .testTag("trip_room_invite_plus"),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = "초대하기", tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = "현재 ${members.size}명",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Case A: 나 혼자일 때 - 초대 코드 카드 */
@Composable
private fun InviteMatesCard(inviteCode: String, onCopy: () -> Unit) {
    val cardShape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .fillMaxWidth()
            .shadow(3.dp, cardShape)
            .clip(cardShape)
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 24.dp)
    ) {
        Text(
            text = "트립메이트를 초대하세요!",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "코드를 전송하여 함께 여행을 계획하세요",
            fontSize = 13.sp,
            color = RoomGray
        )
        Spacer(Modifier.height(20.dp))

        // 점선 테두리 초대 코드 박스 + [복사]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CodeBoxBlue)
                .drawBehind {
                    val stroke = 1.5.dp.toPx()
                    drawRoundRect(
                        color = RoomBlue,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(14.dp.toPx() - stroke / 2),
                        style = Stroke(
                            width = stroke,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
                        )
                    )
                }
                .padding(start = 24.dp, end = 14.dp, top = 20.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = inviteCode,
                modifier = Modifier
                    .weight(1f)
                    .testTag("trip_room_invite_code"),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 5.sp,
                color = Color.Black,
                maxLines = 1
            )
            Text(
                text = "복사",
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CopyButtonBlue)
                    .clickable(onClick = onCopy)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("trip_room_copy_button"),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

/** 하단 탭: 여행홈(현재) / 일정 / 가계부 */
@Composable
private fun TripRoomBottomBar(onNotReady: (String) -> Unit) {
    Column(modifier = Modifier.background(Color.White)) {
        HorizontalDivider(color = Color(0xFFE6E8EC))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(72.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomTab(Icons.Outlined.Home, "여행홈", selected = true, tag = "tab_home") {}
            BottomTab(Icons.Outlined.CalendarMonth, "일정", selected = false, tag = "tab_schedule") { onNotReady("일정") }
            BottomTab(Icons.Outlined.Calculate, "가계부", selected = false, tag = "tab_budget") { onNotReady("가계부") }
        }
    }
}

@Composable
private fun RowScope.BottomTab(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit
) {
    val color = if (selected) RoomBlue else Color(0xFF6B6F76)
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 13.sp, color = color)
    }
}

/** "2026-08-01", "2026-08-03" → "2026년 8월 1일 ~ 8월 3일" (해가 바뀌면 종료일에도 연도 표시) */
internal fun koreanDateRange(start: String, end: String): String {
    fun parse(s: String) = s.split("-").mapNotNull { it.toIntOrNull() }.takeIf { it.size == 3 }
    val a = parse(start) ?: return ""
    val b = parse(end) ?: return "${a[0]}년 ${a[1]}월 ${a[2]}일"
    val endText = if (a[0] == b[0]) "${b[1]}월 ${b[2]}일" else "${b[0]}년 ${b[1]}월 ${b[2]}일"
    return "${a[0]}년 ${a[1]}월 ${a[2]}일 ~ $endText"
}

/** 밸런스 게임 선택지 */
enum class TravelStyle(val title: String, val description: String) {
    PACKED("빈틈없이 꽉 찬 일정", "계획한 곳은 전부 가 봐야 해요"),
    RELAXED("쉬엄쉬엄 발길 닿는 대로", "여유롭게 즉흥으로 즐겨요")
}

/** Case B: 2명 이상일 때 - 여행 밸런스 게임 화면 */
@Composable
private fun BalanceGameContent(onSelected: (TravelStyle) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<TravelStyle?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "여행 밸런스 게임",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = RoomBlue
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "나의 여행 스타일은?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "트립메이트와 여행 취향을 맞춰 보세요",
            fontSize = 15.sp,
            color = RoomGray
        )

        Spacer(Modifier.height(32.dp))

        BalanceOptionCard(
            style = TravelStyle.PACKED,
            icon = Icons.Outlined.CalendarMonth,
            isSelected = selected == TravelStyle.PACKED,
            onClick = { selected = TravelStyle.PACKED }
        )

        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFFA6B43)),
            contentAlignment = Alignment.Center
        ) {
            Text("VS", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        }

        BalanceOptionCard(
            style = TravelStyle.RELAXED,
            icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
            isSelected = selected == TravelStyle.RELAXED,
            onClick = { selected = TravelStyle.RELAXED }
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { selected?.let(onSelected) },
            enabled = selected != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("balance_game_submit_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = RoomBlue,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFD5D9DF),
                disabledContentColor = Color.White
            )
        ) {
            Text("선택 완료", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BalanceOptionCard(
    style: TravelStyle,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) RoomBlue else Color.White)
            .border(
                width = if (isSelected) 0.dp else 1.dp,
                color = Color(0xFFE0E3E8),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
            .testTag("balance_option_${style.name}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color.White.copy(alpha = 0.2f) else RoomBlueLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else RoomBlue)
        }
        Column {
            Text(
                text = style.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color.Black
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = style.description,
                fontSize = 13.sp,
                color = if (isSelected) Color.White.copy(alpha = 0.85f) else RoomGray
            )
        }
    }
}

@Composable
private fun CenterMessage(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(32.dp),
        fontSize = 15.sp,
        color = RoomGray,
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
    )
}
