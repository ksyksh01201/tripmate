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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GroupAdd
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
import com.example.ui.viewmodel.TripRoomUiState
import com.example.ui.viewmodel.TripRoomViewModel

private val RoomBlue = Color(0xFF4A90E2)
private val RoomBlueLight = Color(0xFFE3EEFC)
private val RoomGray = Color(0xFF808080)
private val AvatarColors = listOf(
    Color(0xFF1E2B3E), Color(0xFFFA6B43), Color(0xFF4A90E2),
    Color(0xFF34C38F), Color(0xFF9B6BDF), Color(0xFFF5A623)
)

/**
 * 여행룸 화면.
 * Firestore의 members 수를 실시간으로 보고 화면을 바꿉니다.
 * - members.size == 1 : "트립메이트를 초대하세요!" + 초대 코드 + [복사]
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val trip = (uiState as? TripRoomUiState.Loaded)?.trip
        TripRoomTopBar(trip = trip, myUid = (uiState as? TripRoomUiState.Loaded)?.myUid, onBack = onBack)

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
                        label = "trip_room_branch"
                    ) { waiting ->
                        if (waiting) {
                            InviteMatesContent(inviteCode = state.trip.inviteCode)
                        } else {
                            BalanceGameContent(onSelected = onTravelStyleSelected)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripRoomTopBar(trip: Trip?, myUid: String?, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.testTag("trip_room_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color.Black)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = trip?.title.orEmpty(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1
            )
            if (trip != null) {
                Text(text = trip.dateRangeText, fontSize = 13.sp, color = RoomGray)
            }
        }
        if (trip != null) {
            MemberAvatars(members = trip.members, myUid = myUid)
        }
    }
}

/** 상단 프로필 원형 아이콘. 인원수만큼 겹쳐서 보여줍니다. (최대 4개 + "+N") */
@Composable
private fun MemberAvatars(members: List<String>, myUid: String?) {
    val maxShown = 4
    val shown = members.take(maxShown)
    Row(
        modifier = Modifier.testTag("trip_room_member_avatars"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        shown.forEachIndexed { index, uid ->
            Box(
                modifier = Modifier
                    .offset(x = (-8 * index).dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(AvatarColors[index % AvatarColors.size])
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uid == myUid) "나" else "${index + 1}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (members.size > maxShown) {
            Text(
                text = "+${members.size - maxShown}",
                modifier = Modifier.offset(x = (-8 * maxShown + 4).dp),
                fontSize = 13.sp,
                color = RoomGray
            )
        }
    }
}

/** Case A: 나 혼자일 때 - 초대 코드 화면 */
@Composable
private fun InviteMatesContent(inviteCode: String) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(RoomBlueLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.GroupAdd, contentDescription = null, tint = RoomBlue, modifier = Modifier.size(64.dp))
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "트립메이트를 초대하세요!",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "아래 초대 코드를 친구에게 보내면\n함께 여행을 계획할 수 있어요",
            fontSize = 15.sp,
            color = RoomGray,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(Modifier.height(32.dp))

        // 초대 코드 박스
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(RoomBlueLight)
                .padding(vertical = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = inviteCode,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                color = RoomBlue,
                modifier = Modifier.testTag("trip_room_invite_code")
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                clipboard.setText(AnnotatedString(inviteCode))
                Toast.makeText(context, "초대 코드가 복사되었어요", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("trip_room_copy_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RoomBlue, contentColor = Color.White)
        ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("복사", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "친구가 들어오면 화면이 자동으로 바뀌어요",
            fontSize = 13.sp,
            color = Color(0xFFB0B0B0)
        )
    }
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
