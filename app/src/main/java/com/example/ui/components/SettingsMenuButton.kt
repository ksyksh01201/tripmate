package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 홈 화면 오른쪽 위 톱니바퀴. 누르면 작게 [로그아웃] [탈퇴] 메뉴가 뜹니다. */
@Composable
fun SettingsMenuButton(
    onLogoutClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = { open = true },
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
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color.White
        ) {
            DropdownMenuItem(
                text = { Text("로그아웃", fontSize = 15.sp, color = Color(0xFF202020)) },
                onClick = { open = false; onLogoutClick() },
                modifier = Modifier
                    .height(44.dp)
                    .testTag("settings_logout")
            )
            DropdownMenuItem(
                text = { Text("탈퇴", fontSize = 15.sp, color = Color(0xFFE53935), fontWeight = FontWeight.Medium) },
                onClick = { open = false; onWithdrawClick() },
                modifier = Modifier
                    .height(44.dp)
                    .testTag("settings_withdraw")
            )
        }
    }
}
