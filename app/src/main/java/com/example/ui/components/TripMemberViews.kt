package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.AccountStore
import com.example.data.Trip
import com.example.data.TripMemberNames
import kotlinx.coroutines.flow.flowOf

private val PanelBlue = Color(0xFF3A78D6)
private val AvatarIconBlue = Color(0xFF3A78D6)

/** 멤버 표시 이름: 나는 "홍길동(나)", 다른 사람은 아직 이름 정보가 없어서 "멤버2" … */
@Composable
private fun memberLabel(uid: String, index: Int, myUid: String?, names: Map<String, String>): String {
    val context = LocalContext.current
    val myName = remember { AccountStore.name(context) }
    return if (uid == myUid) "${myName.ifBlank { "나" }}(나)"
    else names[uid]?.takeIf { it.isNotBlank() } ?: "멤버${index + 1}"
}

/**
 * 여행방 상단 참여자 바 (피그마 새 UI).
 * [👥 참여자 N명 | (사람) 홍길동(나)  (사람) 멤버2 …]  — 인원이 많으면 옆으로 넘겨 볼 수 있어요.
 */
@Composable
fun TripMembersPanel(
    members: List<String>,
    myUid: String?,
    modifier: Modifier = Modifier,
    trip: Trip? = null
) {
    val context = LocalContext.current
    // 여행 문서에 저장된 이름들 (팀원이 자기 폰에서 들어오면 그 사람 이름이 보여요)
    val names by remember(trip?.tripId) {
        trip?.tripId?.takeIf { it.isNotBlank() }?.let { TripMemberNames.observe(it) } ?: flowOf(emptyMap())
    }.collectAsState(initial = emptyMap())
    // 내 이름을 여행 문서에 저장해서 다른 멤버 화면에도 보이게 함
    LaunchedEffect(trip?.tripId, myUid) {
        val id = trip?.tripId
        if (!id.isNullOrBlank() && myUid != null && myUid in members) {
            runCatching { TripMemberNames.saveMyName(id, myUid, AccountStore.name(context)) }
        }
    }
    // 테스트 때 넣었던 가짜 멤버가 남아 있으면 자동으로 지움
    LaunchedEffect(trip?.tripId, members) {
        val id = trip?.tripId
        if (!id.isNullOrBlank() && myUid != null && myUid in members) {
            runCatching { TripMemberNames.removeTestMembers(id, members) }
        }
    }
    // 나를 맨 앞에 보여 줌
    val ordered = remember(members, myUid) {
        members.withIndex().sortedBy { if (it.value == myUid) 0 else 1 }
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PanelBlue)
            .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
            .testTag("trip_room_members_panel"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text("참여자 ${members.size}명", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .width(1.5.dp)
                .height(34.dp)
                .background(Color.White.copy(alpha = 0.85f))
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(start = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ordered.forEach { (index, uid) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.widthIn(min = 52.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = AvatarIconBlue, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        memberLabel(uid, index, myUid, names),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 72.dp)
                    )
                }
            }
        }
    }
}

/** 홈 화면 파란 카드의 인원 표시: [👥 2명] 흰 테두리 알약 */
@Composable
fun TripMemberCountPill(count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(1.5.dp, Color.White, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .testTag("home_member_count"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Group, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("${count}명", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
