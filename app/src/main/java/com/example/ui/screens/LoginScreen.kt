package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.shadow
import com.example.ui.components.TripMateLogo
import com.example.ui.components.TripMateTextField
import com.example.ui.theme.TripMateBlue
import com.example.ui.theme.TripMateOrange
import com.example.ui.theme.TripMateTextSecondary
import kotlinx.coroutines.delay

/**
 * [개발용 임시 인증 검증 로직]
 * Firebase 연동 전 UI 및 화면 전환 테스트를 위한 임시 로그인 검증입니다.
 * 실제 사용자 계정 인증 서버와 연결된 기능이 아니며, 테스트 완료 후 Firebase Auth로 대체될 예정입니다.
 */
private fun executeMockLogin(
    email: String,
    password: String,
    context: Context,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val trimmedEmail = email.trim()
    val trimmedPassword = password.trim()

    // 1. 이메일 입력값 검사
    if (trimmedEmail.isEmpty()) {
        onError("이메일을 입력해 주세요.")
        return
    }

    // 2. 이메일 형식 검사 (간단한 이메일 정규식)
    val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    if (!emailRegex.matches(trimmedEmail)) {
        onError("올바른 이메일 형식을 입력해 주세요. (예: example@email.com)")
        return
    }

    // 3. 비밀번호 입력값 검사
    if (trimmedPassword.isEmpty()) {
        onError("비밀번호를 입력해 주세요.")
        return
    }

    // 검증 성공: 개발용 임시 로그인 완료 후 홈 화면으로 이동 (실제 인증 서버 연동 전 개발용)
    onSuccess(trimmedEmail)
}

@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoginSuccess by remember { mutableStateOf(false) }
    var showSnackbar by remember { mutableStateOf(false) }

    // 로그인 성공 시 2.5초 동안 로그인 화면에 머물며 성공 알림을 표시한 후 홈 화면으로 자동 이동
    // 화면이 벗어나거나 사용자가 뒤로가기 하면 LaunchedEffect는 자동으로 취소됩니다.
    if (isLoginSuccess) {
        LaunchedEffect(Unit) {
            showSnackbar = true
            delay(2150) // 성공 문구를 약 2.15초 유지
            showSnackbar = false // 화면 이동 직전에 자연스럽게 페이드아웃
            delay(350) // 페이드아웃 완료 (총 2.5초 경과)
            onLoginSuccess(email.trim())
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(56.dp))

            // Logo & Subtitle
            TripMateLogo(
                color = TripMateBlue,
                fontSize = 34.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "만나서 반가워요! 이어서 여행을 계획해 볼까요?",
                fontSize = 13.5.sp,
                color = TripMateTextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Email Field
            TripMateTextField(
                label = "이메일",
                value = email,
                onValueChange = {
                    email = it
                    if (errorMessage != null) errorMessage = null
                },
                placeholder = "example@email.com",
                keyboardType = KeyboardType.Email,
                testTag = "login_email_input"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Password Field
            TripMateTextField(
                label = "비밀번호",
                value = password,
                onValueChange = {
                    password = it
                    if (errorMessage != null) errorMessage = null
                },
                placeholder = "비밀번호 입력",
                isPassword = true,
                testTag = "login_password_input"
            )

            // 안내 / 에러 메시지 표시 영역
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    color = Color(0xFFE53935),
                    fontSize = 13.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(28.dp))
            }

            // Login Button (중복 클릭 방지를 위해 로그인 성공 처리 중에는 비활성화)
            Button(
                onClick = {
                    if (isLoginSuccess) return@Button
                    executeMockLogin(
                        email = email,
                        password = password,
                        context = context,
                        onSuccess = {
                            errorMessage = null
                            isLoginSuccess = true
                        },
                        onError = { message ->
                            errorMessage = message
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                enabled = !isLoginSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TripMateBlue,
                    contentColor = Color.White,
                    disabledContainerColor = TripMateBlue.copy(alpha = 0.7f),
                    disabledContentColor = Color.White.copy(alpha = 0.9f)
                )
            ) {
                Text(
                    text = "로그인",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(48.dp))

            // Bottom Navigation Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "아직 계정이 없나요? ",
                    fontSize = 13.5.sp,
                    color = Color(0xFF4B5563)
                )
                Text(
                    text = "회원가입",
                    fontSize = 13.5.sp,
                    color = TripMateOrange,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(enabled = !isLoginSuccess) { onNavigateToSignUp() }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .testTag("go_to_signup_button")
                )
            }
        }

        // 세련된 Snackbar 스타일의 로그인 성공 알림창
        // 로그인 성공 시 화면 하단 중앙에 부드럽게 나타났다가 2.5초 후 화면 이동 직전 사라짐
        AnimatedVisibility(
            visible = showSnackbar,
            enter = slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight / 2 },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 350)),
            exit = slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight / 3 },
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 350)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 68.dp)
        ) {
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        spotColor = Color(0x24000000),
                        ambientColor = Color(0x14000000)
                    )
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFEFF2F5), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 초록색 체크 아이콘
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "성공",
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // “로그인되었습니다!” 문구 (검은색 14sp)
                Text(
                    text = "로그인되었습니다!",
                    color = Color(0xFF1E2022),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.2).sp
                )
            }
        }
    }
}
