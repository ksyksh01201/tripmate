package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import com.example.ui.theme.TripMateBlue

@Composable
fun SuitcaseIllustration(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(170.dp)
            .clip(CircleShape)
            .background(Color(0xFFE3EEFC)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(75.dp)) {
            val strokeWidth = 3.8.dp.toPx()
            val blueColor = Color(0xFF4A90E2)

            // Handle
            val handleWidth = size.width * 0.36f
            val handleHeight = size.height * 0.16f
            val handleLeft = (size.width - handleWidth) / 2f
            val handleTop = size.height * 0.08f

            val handlePath = Path().apply {
                moveTo(handleLeft, handleTop + handleHeight)
                lineTo(handleLeft, handleTop + 5.dp.toPx())
                quadraticTo(handleLeft, handleTop, handleLeft + 5.dp.toPx(), handleTop)
                lineTo(handleLeft + handleWidth - 5.dp.toPx(), handleTop)
                quadraticTo(handleLeft + handleWidth, handleTop, handleLeft + handleWidth, handleTop + 5.dp.toPx())
                lineTo(handleLeft + handleWidth, handleTop + handleHeight)
            }
            drawPath(
                path = handlePath,
                color = blueColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Main Suitcase Body
            val bodyLeft = size.width * 0.09f
            val bodyTop = size.height * 0.23f
            val bodyWidth = size.width * 0.82f
            val bodyHeight = size.height * 0.69f
            val cornerRadius = 14.dp.toPx()

            drawRoundRect(
                color = blueColor,
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyWidth, bodyHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Vertical Straps / dividers
            val strap1X = bodyLeft + bodyWidth * 0.32f
            val strap2X = bodyLeft + bodyWidth * 0.68f

            drawLine(
                color = blueColor,
                start = Offset(strap1X, bodyTop),
                end = Offset(strap1X, bodyTop + bodyHeight),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            drawLine(
                color = blueColor,
                start = Offset(strap2X, bodyTop),
                end = Offset(strap2X, bodyTop + bodyHeight),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun BusanThumbnail(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF0F1E36))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Night Sky Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0B1426),
                        Color(0xFF162544),
                        Color(0xFF0D1B2A)
                    ),
                    startY = 0f,
                    endY = h * 0.65f
                ),
                topLeft = Offset.Zero,
                size = Size(w, h * 0.65f)
            )

            // Sea / Water
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A1526),
                        Color(0xFF050B14)
                    ),
                    startY = h * 0.65f,
                    endY = h
                ),
                topLeft = Offset(0f, h * 0.65f),
                size = Size(w, h * 0.35f)
            )

            // Distant Mountains / Horizon
            val mountainPath = Path().apply {
                moveTo(0f, h * 0.62f)
                lineTo(w * 0.25f, h * 0.58f)
                lineTo(w * 0.5f, h * 0.63f)
                lineTo(w * 0.8f, h * 0.56f)
                lineTo(w, h * 0.61f)
                lineTo(w, h * 0.66f)
                lineTo(0f, h * 0.66f)
                close()
            }
            drawPath(mountainPath, color = Color(0xFF0E1A30))

            // Bridge Towers (Gwangandaegyo)
            val tower1X = w * 0.35f
            val tower2X = w * 0.65f
            val deckY = h * 0.62f
            val towerTopY = h * 0.35f

            // Deck
            drawLine(
                color = Color(0xFFE2E8F0),
                start = Offset(0f, deckY),
                end = Offset(w, deckY),
                strokeWidth = 2.5f
            )

            // Tower 1
            drawLine(
                color = Color(0xFFF8FAFC),
                start = Offset(tower1X, towerTopY),
                end = Offset(tower1X, deckY + 4f),
                strokeWidth = 2.5f
            )

            // Tower 2
            drawLine(
                color = Color(0xFFF8FAFC),
                start = Offset(tower2X, towerTopY),
                end = Offset(tower2X, deckY + 4f),
                strokeWidth = 2.5f
            )

            // Suspension Main Cables
            val cablePath = Path().apply {
                moveTo(0f, deckY)
                quadraticTo(tower1X * 0.5f, deckY - 6f, tower1X, towerTopY)
                quadraticTo(w * 0.5f, deckY - 4f, tower2X, towerTopY)
                quadraticTo(w * 0.85f, deckY - 6f, w, deckY)
            }
            drawPath(
                cablePath,
                color = Color(0xFF7DD3FC),
                style = Stroke(width = 1.8f)
            )

            // Bridge Lights / Illuminations
            val lightColors = listOf(Color(0xFFFDE047), Color(0xFF67E8F9), Color(0xFFA78BFA), Color(0xFFF472B6))
            for (i in 0..12) {
                val lx = w * (0.08f + i * 0.07f)
                val color = lightColors[i % lightColors.size]
                drawCircle(
                    color = color,
                    radius = 1.6f,
                    center = Offset(lx, deckY)
                )
                // Water reflection
                drawCircle(
                    color = color.copy(alpha = 0.4f),
                    radius = 1.2f,
                    center = Offset(lx, deckY + 8f + (i % 3) * 4f)
                )
            }
        }
    }
}

@Composable
fun JejuThumbnail(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF60A5FA))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Sky
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF38BDF8),
                        Color(0xFF93C5FD)
                    ),
                    startY = 0f,
                    endY = h * 0.55f
                ),
                topLeft = Offset.Zero,
                size = Size(w, h * 0.55f)
            )

            // Ocean Horizon
            drawRect(
                color = Color(0xFF0284C7),
                topLeft = Offset(0f, h * 0.45f),
                size = Size(w, h * 0.15f)
            )

            // White Puffy Cloud
            drawCircle(color = Color.White.copy(alpha = 0.9f), radius = w * 0.14f, center = Offset(w * 0.72f, h * 0.22f))
            drawCircle(color = Color.White.copy(alpha = 0.9f), radius = w * 0.1f, center = Offset(w * 0.58f, h * 0.24f))
            drawCircle(color = Color.White.copy(alpha = 0.9f), radius = w * 0.11f, center = Offset(w * 0.84f, h * 0.25f))

            // Green Hill / Field
            val hillPath = Path().apply {
                moveTo(0f, h * 0.58f)
                quadraticTo(w * 0.35f, h * 0.48f, w * 0.7f, h * 0.55f)
                quadraticTo(w * 0.9f, h * 0.58f, w, h * 0.52f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                hillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF65A30D),
                        Color(0xFF4D7C0F),
                        Color(0xFF3F6212)
                    ),
                    startY = h * 0.5f,
                    endY = h
                )
            )

            // Subtle Flowers / Rapeseed yellow dots on green field
            val flowerPoints = listOf(
                Offset(w * 0.2f, h * 0.75f),
                Offset(w * 0.35f, h * 0.82f),
                Offset(w * 0.5f, h * 0.7f),
                Offset(w * 0.65f, h * 0.88f),
                Offset(w * 0.8f, h * 0.78f),
                Offset(w * 0.15f, h * 0.88f),
                Offset(w * 0.42f, h * 0.62f),
                Offset(w * 0.88f, h * 0.68f)
            )
            for (p in flowerPoints) {
                drawCircle(color = Color(0xFFFACC15), radius = 1.8f, center = p)
            }
        }
    }
}

@Composable
fun GenericTravelThumbnail(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color(0xFF3B82F6))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Sunset Sky Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFB923C),
                        Color(0xFFF472B6),
                        Color(0xFF818CF8)
                    ),
                    startY = 0f,
                    endY = h * 0.65f
                ),
                topLeft = Offset.Zero,
                size = Size(w, h * 0.65f)
            )

            // Sun
            drawCircle(
                color = Color(0xFFFEF08A),
                radius = w * 0.16f,
                center = Offset(w * 0.35f, h * 0.42f)
            )

            // Mountains silhouette
            val mountainPath = Path().apply {
                moveTo(0f, h * 0.65f)
                lineTo(w * 0.3f, h * 0.48f)
                lineTo(w * 0.65f, h * 0.62f)
                lineTo(w * 0.85f, h * 0.52f)
                lineTo(w, h * 0.6f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(mountainPath, color = Color(0xFF1E293B))
        }
    }
}
