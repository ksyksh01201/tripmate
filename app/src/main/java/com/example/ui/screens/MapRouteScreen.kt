package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DaySchedule
import com.example.model.RoutePlace
import com.example.model.TmapRouteHelper
import com.example.ui.theme.*

/**
 * [Tmap API 연동 준비 화면]
 * 향후 Android Studio에서 Tmap SDK 연결 시:
 * 1) [TmapRouteMapPlaceholder] -> 실제 TMapView (AndroidView)로 교체
 * 2) routePlaces의 위경도(latitude, longitude) -> TMapMarkerItem으로 마커 표시
 * 3) routePlaces 순서 -> TMapData.findPathDataAllType()으로 경로 폴리라인 표시
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapRouteScreen(
    tripTitle: String = "여행",
    daySchedules: List<DaySchedule>,
    initialDayIndex: Int = 0,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // [3. DAY 탭] 현재 여행의 날짜만 유지 (initialDayIndex는 일정 화면에서 선택한 DAY 반영)
    var selectedDayIndex by remember(initialDayIndex, daySchedules) {
        mutableIntStateOf(initialDayIndex.coerceIn(0, (daySchedules.size - 1).coerceAtLeast(0)))
    }

    val currentDay = daySchedules.getOrNull(selectedDayIndex)
        ?: daySchedules.firstOrNull()
        ?: DaySchedule(1, "1일차", emptyList())

    // [5. 지도 데이터 처리 로직 분리]
    // 현재 DAY의 일정을 시간/order 순으로 정렬하여 Tmap 연동용 routePlaces 생성
    val routePlaces = remember(currentDay.items) {
        TmapRouteHelper.buildRoutePlaces(currentDay.items)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "지도 확인",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // [1. 현재 여행 정보 표시] 예: chingu · DAY 1 · 10.11
                        val dateInfo = if (currentDay.dateStr.isNotBlank()) " · ${currentDay.dateStr}" else ""
                        val subText = if (tripTitle.isNotBlank()) {
                            "$tripTitle · DAY ${currentDay.dayNumber}$dateInfo"
                        } else {
                            "DAY ${currentDay.dayNumber}$dateInfo"
                        }
                        Text(
                            text = subText,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("map_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // [3. DAY 탭] 현재 여행 기간에 존재하는 DAY만 표시 (예: 2일 일정이면 DAY 1 | DAY 2만 표시)
            if (daySchedules.size > 1) {
                TabRow(
                    selectedTabIndex = selectedDayIndex.coerceIn(0, daySchedules.size - 1),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    daySchedules.forEachIndexed { index, day ->
                        Tab(
                            selected = selectedDayIndex == index,
                            onClick = { selectedDayIndex = index },
                            text = {
                                Text(
                                    text = "DAY ${day.dayNumber}",
                                    fontWeight = if (selectedDayIndex == index) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }
            }

            // [6. 현재 지도 영역 (Tmap API 연동 Placeholder)]
            TmapRouteMapPlaceholder(
                routePlaces = routePlaces,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(16.dp)
            )

            // [7. 이동 순서 목록]
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "이동 순서 (${routePlaces.size}개 장소)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (routePlaces.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "등록된 일정이 없습니다.",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(routePlaces) { index, place ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("route_stop_${place.order}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 순서 배지 (1, 2, ...)
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(TravelBluePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${place.order}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = place.placeName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (place.time.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "예상 도착: ${place.time}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * [6. Tmap 지도 컴포넌트 Placeholder]
 * 향후 실제 Tmap SDK를 연결할 때 이 컴포넌트 내부를 TMapView(AndroidView)로 교체합니다.
 *
 * Tmap 연동 명세:
 * - routePlaces.forEach { place ->
 *     if (place.latitude != null && place.longitude != null) {
 *         val marker = TMapMarkerItem().apply {
 *             setTMapPoint(place.latitude, place.longitude)
 *             name = place.placeName
 *         }
 *         tMapView.addMarkerItem("marker_${place.order}", marker)
 *     }
 * }
 * - tMapData.findPathDataAllType(startPoint, endPoint) 경로 탐색
 */
@Composable
fun TmapRouteMapPlaceholder(
    routePlaces: List<RoutePlace>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        if (routePlaces.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "등록된 장소가 없습니다.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        } else {
            // [6. 간단한 1 │ 2 형태의 이동 순서 경로 표시 유지]
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val w = size.width
                val h = size.height

                // Grid background
                val gridColor = Color(0xFFE2E8F0)
                for (x in 0..4) {
                    drawLine(
                        color = gridColor,
                        start = Offset(w * (x / 4f), 0f),
                        end = Offset(w * (x / 4f), h),
                        strokeWidth = 2f
                    )
                }
                for (y in 0..4) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, h * (y / 4f)),
                        end = Offset(w, h * (y / 4f)),
                        strokeWidth = 2f
                    )
                }

                // Points along the route
                val points = routePlaces.mapIndexed { idx, _ ->
                    val ratio = if (routePlaces.size > 1) idx.toFloat() / (routePlaces.size - 1) else 0.5f
                    val x = w * (0.15f + 0.7f * (if (idx % 2 == 0) ratio else 1f - ratio))
                    val y = h * (0.2f + 0.6f * ratio)
                    Offset(x, y)
                }

                // Path lines connecting points
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = TravelBluePrimary,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 5f
                    )
                }

                val numPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 28f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // Marker circles with sequence number
                points.forEachIndexed { idx, pt ->
                    drawCircle(
                        color = TravelBluePrimary,
                        radius = 22f,
                        center = pt
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        "${idx + 1}",
                        pt.x,
                        pt.y + 10f,
                        numPaint
                    )
                }
            }
        }
    }
}
