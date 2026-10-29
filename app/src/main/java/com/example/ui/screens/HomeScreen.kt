package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SuitcaseIllustration
import com.example.ui.components.TripMateLogo
import com.example.ui.theme.TripMateBlue
import com.example.ui.theme.TripMateTextPrimary
import com.example.ui.theme.TripMateTextSecondary

/**
 * 로그인 이후 표시되는 TripMate 홈 화면
 * 첨부된 이미지(IMG_4546_5a206f00.jpeg)의 UI를 그대로 구현하며,
 * 여행가방 아이콘과 “첫 여행을 시작해 볼까요?” 문구가 있는 빈 상태 화면입니다.
 * 현재 단계에서는 UI 디자인 전용 단계이므로 홈 화면의 모든 버튼과 아이콘은 클릭 시 동작하지 않습니다.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 상단 앱 바: 왼쪽 파란색 tripmate 로고, 오른쪽 회색 설정 아이콘
            HomeTopBar()

            // 중앙 및 하단 영역
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.9f))

                // 2. 여행가방 아이콘: 170px 원형 배경(#E3EEFC) + 75px 여행가방 아이콘(#4A90E2)
                SuitcaseIllustration()

                Spacer(modifier = Modifier.height(32.dp))

                // 3. 메인 제목: "첫 여행을 시작해 볼까요?", 28px, 굵기 700~800, 검은색, 중앙 정렬
                Text(
                    text = "첫 여행을 시작해 볼까요?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. 안내 문구: 두 줄 표시, 16px, #808080, 중앙 정렬, 적정 줄 간격
                Text(
                    text = "새 여행을 만들거나\n친구의 초대 코드로 참여해 보세요",
                    fontSize = 16.sp,
                    color = Color(0xFF808080),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                // 5. 첫 번째 버튼: "새 여행 만들기", 배경 #4A90E2, 흰색 글씨, 굵기 700, 높이 60px, 모서리 10px (비활성 유지)
                Button(
                    onClick = {
                        // 현재 UI 단계: 클릭 동작 없음
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("empty_create_trip_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A90E2),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "새 여행 만들기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. 두 번째 버튼: "초대 코드로 참여하기", 배경 흰색, 테두리 파란색 1px, 글씨 #4A90E2, 높이 60px, 모서리 10px (비활성 유지)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color.White, RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF4A90E2), RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // 현재 UI 단계: 클릭 동작 없음
                        }
                        .testTag("empty_join_trip_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "초대 코드로 참여하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4A90E2)
                    )
                }

                Spacer(modifier = Modifier.weight(1.3f))
            }
        }
    }
}

@Composable
private fun HomeTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TripMateLogo(
            color = Color(0xFF4A90E2),
            fontSize = 32.sp
        )
        IconButton(
            onClick = {
                // 현재 UI 단계: 클릭 동작 없음
            },
            modifier = Modifier
                .size(40.dp)
                .testTag("home_settings_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "설정",
                tint = Color(0xFFB0B0B0),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
