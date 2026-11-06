package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * "초대 코드로 참여하기" 팝업
 * 코드를 1자리 이상 입력하면 [참여하기] 버튼이 활성화됩니다.
 */
@Composable
fun JoinTripDialog(
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCodeChange: () -> Unit,
    onJoin: (inviteCode: String) -> Unit
) {
    var code by rememberSaveable { mutableStateOf("") }
    val canSubmit = code.isNotBlank() && !isSubmitting

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
            Column(modifier = Modifier.padding(24.dp)) {
                DialogHeader(title = "초대 코드로 참여하기", onClose = onDismiss, closeEnabled = !isSubmitting)

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "친구에게 받은 초대 코드를 입력해 주세요",
                    fontSize = 14.sp,
                    color = Color(0xFF808080),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { input ->
                        // 영문/숫자만, 대문자로, 최대 12자
                        code = input.filter { it.isLetterOrDigit() }.uppercase().take(12)
                        onCodeChange()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_trip_code_field"),
                    placeholder = {
                        Text(
                            text = "예) TRIP0101",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF9E9E9E)
                        )
                    },
                    textStyle = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    isError = errorMessage != null,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A90E2),
                        unfocusedBorderColor = Color(0xFFE0E3E8),
                        cursorColor = Color(0xFF4A90E2)
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (canSubmit) onJoin(code) }
                    )
                )

                if (errorMessage != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFE5484D),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(24.dp))

                PrimaryDialogButton(
                    text = "참여하기",
                    enabled = canSubmit,
                    isLoading = isSubmitting,
                    testTag = "join_trip_submit_button",
                    onClick = { onJoin(code) }
                )
            }
        }
    }
}
