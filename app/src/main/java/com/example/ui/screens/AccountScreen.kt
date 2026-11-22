package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AccountStore
import com.example.ui.components.TripMateLogo
import com.google.firebase.auth.FirebaseAuth

// 피그마 "로그아웃 화면 / 로그아웃 확인 여부" 색상
private val AccBlue = Color(0xFF4A90E2)
private val AccButtonBlue = Color(0xFF3A7BE0)
private val AccText = Color(0xFF000000)
private val AccGray = Color(0xFF6B6B6B)
private val AccBorder = Color(0xFFE0E0E0)
private val LogoutRed = Color(0xFFD83A2E)
private val LogoutBackground = Color(0xFFFCEAE8)
private val PopupCancelGray = Color(0xFFEEEEEE)

/**
 * 계정 정보 화면 (홈 톱니바퀴 → 로그아웃).
 * 이름 / 이메일 → [로그아웃] → "정말 로그아웃 하시겠어요?" → [로그아웃] 이면 로그인 화면으로
 */
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current

    val auth = FirebaseAuth.getInstance()
    val currentUid = auth.currentUser?.uid

    var name by remember(currentUid) {
        mutableStateOf(
            auth.currentUser?.displayName
                ?.takeIf { it.isNotBlank() }
                ?: AccountStore.name(context)
        )
    }

    val email = remember(currentUid) {
        auth.currentUser?.email
            ?: AccountStore.email(context)
    }

    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var editName by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 파란 상단: ← / tripmate / 계정 정보
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(AccBlue)
                .statusBarsPadding()
                .height(92.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .testTag("account_back")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color.White)
            }
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                TripMateLogo(color = Color.White, fontSize = 18.sp)
                Text("계정 정보", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 40.dp)) {
            // 이름 / 이메일 카드
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .border(1.dp, AccBorder, RoundedCornerShape(24.dp))
            ) {
                AccountRow(
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = AccBlue, modifier = Modifier.size(30.dp)) },
                    label = "이름",
                    value = name.ifBlank { "이름 설정" },
                    showChevron = true,
                    modifier = Modifier
                        .clickable { editName = true }
                        .testTag("account_name")
                )
                HorizontalDivider(color = Color(0xFFEAEAEA), modifier = Modifier.padding(horizontal = 20.dp))
                AccountRow(
                    icon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = AccBlue, modifier = Modifier.size(28.dp)) },
                    label = "이메일",
                    value = email.ifBlank { "-" },
                    showChevron = false,
                    modifier = Modifier.testTag("account_email")
                )
            }
            Spacer(Modifier.height(72.dp))

            // 로그아웃 버튼
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .height(88.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(LogoutBackground)
                    .clickable { confirmLogout = true }
                    .testTag("account_logout"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = LogoutRed, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(14.dp))
                Text("로그아웃", color = LogoutRed, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (confirmLogout) {
        LogoutConfirmDialog(
            onCancel = { confirmLogout = false },

            onLogout = {
                confirmLogout = false

                // Firebase 로그인 계정 로그아웃
                FirebaseAuth.getInstance().signOut()

                // 기존 앱에 저장된 계정 정보 초기화
                AccountStore.logout(context)

                // 로그인 화면으로 이동
                onLoggedOut()
            }

        )
    }

    if (editName) {
        NameEditDialog(
            initial = name,
            onDismiss = { editName = false },

            onSave = { newName ->
                val trimmedName = newName.trim()
                val user = FirebaseAuth.getInstance().currentUser

                if (user != null && !user.isAnonymous) {
                    val profileUpdates =
                        com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(trimmedName)
                            .build()

                    user.updateProfile(profileUpdates)
                        .addOnSuccessListener {
                            AccountStore.saveName(context, trimmedName)
                            name = trimmedName
                            editName = false

                            android.widget.Toast.makeText(
                                context,
                                "이름이 변경되었습니다.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            android.widget.Toast.makeText(
                                context,
                                "이름 변경에 실패했습니다.",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    android.widget.Toast.makeText(
                        context,
                        "로그인 상태를 확인해 주세요.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }

        )
    }
}

@Composable
private fun AccountRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    showChevron: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .padding(start = 20.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 21.sp, color = AccText)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            fontSize = 16.sp,
            color = AccGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        if (showChevron) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = AccGray, modifier = Modifier.size(28.dp))
        }
    }
}

/** "로그아웃 / 정말 로그아웃 하시겠어요?" [취소] [로그아웃] */
@Composable
private fun LogoutConfirmDialog(onCancel: () -> Unit, onLogout: () -> Unit) {
    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("로그아웃", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = AccText)
                Spacer(Modifier.height(10.dp))
                Text("정말 로그아웃 하시겠어요?", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = AccGray)
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    DialogChoice("취소", PopupCancelGray, AccText, Modifier.weight(1f).testTag("logout_cancel"), onCancel)
                    DialogChoice("로그아웃", AccButtonBlue, Color.White, Modifier.weight(1f).testTag("logout_confirm"), onLogout)
                }
            }
        }
    }
}

@Composable
private fun DialogChoice(text: String, background: Color, textColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}

/** 이름 오른쪽 > 를 누르면 이름 바꾸기 */
@Composable
private fun NameEditDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf(initial) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("이름 변경", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AccText)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.length <= 20) value = it },
                    singleLine = true,
                    placeholder = { Text("이름 입력") },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccBlue, unfocusedBorderColor = AccBorder, cursorColor = AccBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_name_input")
                )
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DialogChoice("취소", PopupCancelGray, AccText, Modifier.weight(1f), onDismiss)
                    DialogChoice(
                        "저장",
                        if (value.isNotBlank()) AccButtonBlue else AccButtonBlue.copy(alpha = 0.4f),
                        Color.White,
                        Modifier.weight(1f).testTag("account_name_save")
                    ) { if (value.isNotBlank()) onSave(value) }
                }
            }
        }
    }
}
