package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LilitaOneFontFamily
import com.example.ui.theme.TripMateBlue

@Composable
fun TripMateLogo(
    modifier: Modifier = Modifier,
    color: Color = TripMateBlue,
    fontSize: TextUnit = 32.sp
) {
    Text(
        text = "tripmate",
        color = color,
        fontFamily = LilitaOneFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        letterSpacing = (-0.5).sp,
        modifier = modifier
    )
}
