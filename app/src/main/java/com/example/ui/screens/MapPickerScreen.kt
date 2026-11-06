package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.BuildConfig
import com.example.data.KakaoPlace
import com.example.data.KakaoPlaceSearch
import com.example.data.Schedule
import com.example.data.TripDay
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder
import kotlinx.coroutines.launch

private val MapBlue = Color(0xFF4A90E2)
private val MapOrange = Color(0xFFFF914D)
private val MapText = Color(0xFF202020)
private val MapGray = Color(0xFF727272)
private val MapLine = Color(0xFFDCE6F2)

/** 여행 도시 → 지도 처음 위치 (일정에 좌표가 하나도 없을 때 사용) */
private val CityCenters = mapOf(
    "서울" to (37.5665 to 126.9780), "부산" to (35.1796 to 129.0756), "제주" to (33.4996 to 126.5312),
    "강릉" to (37.7519 to 128.8761), "여수" to (34.7604 to 127.6622), "경주" to (35.8562 to 129.2247),
    "전주" to (35.8242 to 127.1480), "속초" to (38.2070 to 128.5918), "인천" to (37.4563 to 126.7052),
    "대구" to (35.8714 to 128.6014), "광주" to (35.1595 to 126.8526), "대전" to (36.3504 to 127.3845),
    "통영" to (34.8544 to 128.4331), "포항" to (36.0190 to 129.3435), "춘천" to (37.8813 to 127.7298)
)

/**
 * [지도 보기] 전체 화면.
 * 위: 장소 검색 / 가운데: 카카오맵 (그날 일정 = 파란 핀, 검색 결과 = 주황 핀) / 아래: 검색 결과 목록
 * 결과를 누르면 지도가 그 장소로 이동하고, [+ 추가]를 누르면 그날 일정에 추가됩니다.
 */
@Composable
internal fun MapPickerDialog(
    day: TripDay,
    destination: String,
    daySchedules: List<Schedule>,
    isSaving: Boolean,
    onAddPlace: (KakaoPlace) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<KakaoPlace>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var focused by remember { mutableStateOf<KakaoPlace?>(null) }
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapError by remember { mutableStateOf<String?>(null) }

    val addedPlaceIds = daySchedules.mapNotNull { it.placeId.takeIf(String::isNotBlank) }.toSet()

    fun doSearch() {
        val q = query.trim()
        if (q.isEmpty() || searching) return
        focus.clearFocus()
        if (!KakaoPlaceSearch.hasKey) {
            message = "카카오 REST API 키가 없어요. .env 파일을 확인해 주세요."
            return
        }
        searching = true
        message = null
        scope.launch {
            try {
                results = KakaoPlaceSearch.search(q)
                if (results.isEmpty()) message = "'$q' 검색 결과가 없어요."
                focused = results.firstOrNull()
            } catch (e: Exception) {
                message = e.message ?: "장소 검색에 실패했어요."
            } finally {
                searching = false
            }
        }
    }

    // 지도 위 핀 다시 그리기
    LaunchedEffect(kakaoMap, results, daySchedules) {
        val map = kakaoMap ?: return@LaunchedEffect
        val layer = map.labelManager?.layer ?: return@LaunchedEffect
        layer.removeAll()
        val scheduleStyle = map.labelManager?.addLabelStyles(
            LabelStyles.from(LabelStyle.from(pinBitmap(MapBlue.toArgb())).setTextStyles(30, MapText.toArgb(), 2, Color.White.toArgb()))
        )
        val resultStyle = map.labelManager?.addLabelStyles(
            LabelStyles.from(LabelStyle.from(pinBitmap(MapOrange.toArgb())).setTextStyles(30, MapText.toArgb(), 2, Color.White.toArgb()))
        )
        daySchedules.filter { it.lat != null && it.lng != null }.forEachIndexed { i, s ->
            layer.addLabel(
                LabelOptions.from(LatLng.from(s.lat!!, s.lng!!))
                    .setStyles(scheduleStyle)
                    .setTexts(LabelTextBuilder().setTexts("${daySchedules.indexOf(s) + 1}. ${s.title}"))
            )
        }
        results.filter { it.id !in addedPlaceIds }.forEach { p ->
            layer.addLabel(
                LabelOptions.from(LatLng.from(p.lat, p.lng))
                    .setStyles(resultStyle)
                    .setTexts(LabelTextBuilder().setTexts(p.name))
            )
        }
    }

    // 결과를 고르면 카메라 이동
    LaunchedEffect(kakaoMap, focused) {
        val map = kakaoMap ?: return@LaunchedEffect
        val p = focused ?: return@LaunchedEffect
        map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(p.lat, p.lng), 16))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // 상단: ← 1일차 · 8월 1일 토요일
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("map_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = MapText)
                    }
                    Column {
                        Text("${day.label} 장소 추가", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MapText)
                        Text(day.longDate, fontSize = 12.sp, color = MapGray)
                    }
                }

                // 검색창
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("장소 검색 (예: ${destination.ifBlank { "부산" }} 해운대)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MapBlue,
                            unfocusedBorderColor = MapLine,
                            cursorColor = MapBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("map_search_field")
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MapBlue)
                            .clickable { doSearch() }
                            .testTag("map_search_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (searching) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Filled.Search, contentDescription = "검색", tint = Color.White)
                        }
                    }
                }

                message?.let {
                    Text(it, color = Color(0xFFCF592D), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }

                // 지도
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp)
                ) {
                    val start = remember {
                        daySchedules.firstOrNull { it.lat != null && it.lng != null }?.let { it.lat!! to it.lng!! }
                            ?: CityCenters[destination]
                            ?: (37.5665 to 126.9780)
                    }
                    KakaoMapView(
                        startLat = start.first,
                        startLng = start.second,
                        onReady = { kakaoMap = it },
                        onError = { mapError = it }
                    )
                    mapError?.let {
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
                                .padding(16.dp)
                        )
                    }
                }

                // 검색 결과
                if (results.isNotEmpty()) {
                    HorizontalDivider(color = MapLine)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(results, key = { it.id }) { place ->
                            PlaceResultRow(
                                place = place,
                                selected = focused?.id == place.id,
                                added = place.id in addedPlaceIds,
                                enabled = !isSaving,
                                onClick = { focused = place },
                                onAdd = {
                                    focused = place
                                    onAddPlace(place)
                                    Toast.makeText(context, "${day.label}에 '${place.name}'을(를) 추가했어요", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                } else {
                    Text(
                        text = "가고 싶은 곳을 검색하고 [+ 추가]를 누르면 ${day.label} 일정에 들어가요.",
                        color = MapGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceResultRow(
    place: KakaoPlace,
    selected: Boolean,
    added: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) Color(0x144A90E2) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    place.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MapText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (place.category.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(place.category, fontSize = 11.sp, color = MapGray, maxLines = 1)
                }
            }
            Text(place.address, fontSize = 12.sp, color = MapGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        if (added) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MapBlue, modifier = Modifier.size(16.dp))
                Text("추가됨", fontSize = 13.sp, color = MapBlue, fontWeight = FontWeight.Medium)
            }
        } else {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, MapBlue, RoundedCornerShape(8.dp))
                    .clickable(enabled = enabled, onClick = onAdd)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("map_add_${place.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MapBlue, modifier = Modifier.size(14.dp))
                Text("추가", fontSize = 13.sp, color = MapBlue, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 카카오맵 MapView 를 Compose 안에 넣고, 화면 생명주기에 맞춰 resume/pause/finish 합니다. */
@Composable
private fun KakaoMapView(
    startLat: Double,
    startLng: Double,
    onReady: (KakaoMap) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
        LaunchedEffect(Unit) { onError("카카오 네이티브 앱 키가 없어요. .env 파일을 확인해 주세요.") }
        return
    }

    val mapView = remember {
        if (!KakaoMapSdk.isInitialized()) {
            KakaoMapSdk.init(context.applicationContext, BuildConfig.KAKAO_NATIVE_APP_KEY)
        }
        MapView(context).apply {
            start(
                object : MapLifeCycleCallback() {
                    override fun onMapDestroy() {}
                    override fun onMapError(error: Exception) {
                        val text = error.message.orEmpty()
                        onError(
                            if (text.contains("401") || text.contains("auth", ignoreCase = true)) {
                                "지도 인증에 실패했어요.\n카카오 개발자 사이트에 패키지명과 키 해시를 등록했는지 확인해 주세요.\n($text)"
                            } else {
                                "지도를 불러오지 못했어요.\n($text)"
                            }
                        )
                    }
                },
                object : KakaoMapReadyCallback() {
                    override fun onMapReady(map: KakaoMap) = onReady(map)
                    override fun getPosition(): LatLng = LatLng.from(startLat, startLng)
                    override fun getZoomLevel(): Int = 14
                }
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.resume()
                Lifecycle.Event.ON_PAUSE -> mapView.pause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.finish()
        }
    }

    AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
}

/** 지도 핀 모양(동그라미 + 흰 테두리) 비트맵 */
private fun pinBitmap(color: Int): Bitmap {
    val size = 44
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = color
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 6f, paint)
    return bitmap
}
