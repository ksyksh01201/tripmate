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
import com.example.ui.components.TripMateLogo
import com.example.ui.components.TripMateTextField
import com.example.ui.theme.TripMateBlue
import com.example.ui.theme.TripMateOrange
import com.example.ui.theme.TripMateTextSecondary

/**
 * [개발용 임시 회원가입 검증 로직]
 * Firebase 연동 전 UI 및 화면 전환 테스트를 위한 임시 회원가입 검증입니다.
 * 실제 계정 데이터가 서버나 DB에 생성되지 않으며, 테스트 완료 후 Firebase Auth로 대체될 예정입니다.
 */
private fun executeMockSignUp(
    name: String,
    email: String,
    password: String,
    context: Context,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val trimmedName = name.trim()
    val trimmedEmail = email.trim()
    val trimmedPassword = password.trim()

    // 1. 이름 입력값 검사
    if (trimmedName.isEmpty()) {
        onError("이름을 입력해 주세요.")
        return
    }

    // 2. 이메일 입력값 검사
    if (trimmedEmail.isEmpty()) {
        onError("이메일을 입력해 주세요.")
        return
    }

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

    if (trimmedPassword.length < 8) {
        onError("비밀번호를 8자 이상 입력해 주세요.")
        return
    }

    // 검증 성공: 개발용 임시 회원가입 완료 안내 후 로그인 화면으로 복귀
    Toast.makeText(context, "[개발용] 임시 회원가입이 완료되었습니다. 로그인해 주세요.", Toast.LENGTH_SHORT).show()
    onSuccess()
}

@Composable
fun SignUpScreen(
    onNavigateToLogin: () -> Unit,
    onSignUpSuccess: () -> Unit = onNavigateToLogin,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                text = "계정을 만들고 여행을 계획하세요.",
                fontSize = 13.5.sp,
                color = TripMateTextSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Name Field
            TripMateTextField(
                label = "이름",
                value = name,
                onValueChange = {
                    name = it
                    if (errorMessage != null) errorMessage = null
                },
                placeholder = "이름 입력",
                keyboardType = KeyboardType.Text,
                testTag = "signup_name_input"
            )

            Spacer(modifier = Modifier.height(20.dp))

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
                testTag = "signup_email_input"
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
                placeholder = "8자 이상 입력",
                isPassword = true,
                testTag = "signup_password_input"
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

            // Sign Up Button (Coral Orange)
            Button(
                onClick = {
                    executeMockSignUp(
                        name = name,
                        email = email,
                        password = password,
                        context = context,
                        onSuccess = onSignUpSuccess,
                        onError = { message ->
                            errorMessage = message
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("signup_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TripMateOrange,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "회원가입",
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
                    text = "이미 계정이 있나요? ",
                    fontSize = 13.5.sp,
                    color = Color(0xFF4B5563)
                )
                Text(
                    text = "로그인",
                    fontSize = 13.5.sp,
                    color = TripMateBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                        .testTag("go_to_login_button")
                )
            }
        }
    }
}
