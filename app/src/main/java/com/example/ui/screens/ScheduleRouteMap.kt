package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Schedule
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles

private val RouteBlue = Color(0xFF3B82F6)
private val StartGreen = Color(0xFF2EA36B)
private val EndRed = Color(0xFFE5484D)
private val MiddleBlue = Color(0xFF4A90E2)

/**
 * 일정 시트를 아래로 내리면 보이는 지도.
 * 그날 일정 중 장소(좌표)가 있는 것만 번호 핀으로 찍고, 일정 순서대로 이동경로 선을 그립니다.
 * (출발 = 초록, 마지막 = 빨강, 중간 = 파랑)
 */
@Composable
internal fun ScheduleRouteMap(
    places: List<Schedule>,
    allSchedules: List<Schedule>,
    destination: String,
    modifier: Modifier = Modifier
) {
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapError by remember { mutableStateOf<String?>(null) }

    val start = remember {
        places.firstOrNull()?.let { it.lat!! to it.lng!! }
            ?: CityCenters[destination]
            ?: (37.5665 to 126.9780)
    }

    // 일정이 바뀌거나 순서가 바뀌면 핀과 경로를 다시 그림
    LaunchedEffect(kakaoMap, places) {
        val map = kakaoMap ?: return@LaunchedEffect
        val labelManager = map.labelManager ?: return@LaunchedEffect
        val labels = labelManager.layer ?: return@LaunchedEffect
        val routes = map.routeLineManager?.layer
        labels.removeAll()
        routes?.removeAll()

        val points = places.map { LatLng.from(it.lat!!, it.lng!!) }
        places.forEachIndexed { i, s ->
            val number = allSchedules.indexOfFirst { it.scheduleId == s.scheduleId } + 1
            val color = when {
                places.size > 1 && i == places.lastIndex -> EndRed
                i == 0 -> StartGreen
                else -> MiddleBlue
            }
            val style = labelManager.addLabelStyles(
                LabelStyles.from(
                    LabelStyle.from(numberPin(number, color.toArgb()))
                        .setTextStyles(26, Color(0xFF202020).toArgb(), 2, Color.White.toArgb())
                )
            )
            labels.addLabel(
                LabelOptions.from(points[i])
                    .setStyles(style)
                    .setTexts(LabelTextBuilder().setTexts(s.title))
            )
        }

        if (points.size >= 2) {
            routes?.addRouteLine(
                RouteLineOptions.from(
                    RouteLineSegment.from(
                        points,
                        RouteLineStyles.from(RouteLineStyle.from(10f, RouteBlue.toArgb(), 2f, Color.White.toArgb()))
                    )
                )
            )
            map.moveCamera(CameraUpdateFactory.fitMapPoints(points.toTypedArray(), 140))
        } else if (points.size == 1) {
            map.moveCamera(CameraUpdateFactory.newCenterPosition(points[0], 15))
        }
    }

    Box(modifier = modifier) {
        KakaoMapView(
            startLat = start.first,
            startLng = start.second,
            onReady = { kakaoMap = it },
            onError = { mapError = it }
        )
        val message = mapError
            ?: if (places.isEmpty()) "지도에서 검색해 추가한 일정이\n여기에 순서대로 이동경로로 표시돼요." else null
        message?.let {
            Text(
                it,
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xCC202020))
                    .padding(14.dp)
            )
        }
    }
}

/** 번호가 적힌 동그란 핀 비트맵 */
private fun numberPin(number: Int, color: Int): Bitmap {
    val size = 60
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 5f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.textSize = 28f
    paint.typeface = Typeface.DEFAULT_BOLD
    paint.textAlign = Paint.Align.CENTER
    val y = size / 2f - (paint.descent() + paint.ascent()) / 2f
    canvas.drawText(number.toString(), size / 2f, y, paint)
    return bitmap
}
