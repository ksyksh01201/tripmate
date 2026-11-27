package com.example.ui.screens

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.BalanceGameManager
import com.example.data.RecommendedPlace
import com.example.data.TripMemberSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

private fun buildPresetMembers(count: Int, baseNames: List<String>): List<String> {
    val targetCount = count.coerceIn(2, 4)
    val defaults = listOf("홍길동(나)", "김명명", "이민수", "박지훈")
    return (0 until targetCount).map { idx ->
        baseNames.getOrNull(idx)?.takeIf { it.isNotBlank() } ?: defaults[idx]
    }
}

@Composable
fun TripHomeScreen(
    tripId: String,
    destination: String = "부산",
    tripTitle: String = "$destination 바캉스🏖️",
    dateRange: String = "2026년 8월 1일 - 8월 3일",
    selectedSchedulePlaces: List<RecommendedPlace> = emptyList(),
    initialMemberCount: Int = 2,
    onMemberCountChanged: (Int) -> Unit = {},
    onBack: () -> Unit,
    onOpenChemiResult: () -> Unit,
    onSelectPlace: (RecommendedPlace) -> Unit = {},
    // 여행방 연결: 아래 탭(0 여행홈, 1 일정, 2 가계부)을 누르면 여행방 탭으로 이동
    onRoomTabSelected: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val manager = remember { BalanceGameManager() }
    val mainBlue = Color(0xFF3B82F6)
    val darkBlueCard = Color(0xFF2B6CE6)
    val accentOrange = Color(0xFFF97316)

    var activeMemberCount by rememberSaveable(tripId) {
        mutableIntStateOf(initialMemberCount.coerceIn(2, 4))
    }
    var fetchedMemberNames by remember {
        mutableStateOf(listOf("홍길동(나)", "김명명", "이민수", "박지훈"))
    }
    var hasUserOverriddenCount by rememberSaveable(tripId) {
        mutableStateOf(false)
    }
    var gameMemberCountFromDb by remember {
        mutableStateOf<Int?>(null)
    }

    LaunchedEffect(initialMemberCount) {
        val normalized = initialMemberCount.coerceIn(2, 4)
        if (!hasUserOverriddenCount && gameMemberCountFromDb == null && activeMemberCount != normalized) {
            activeMemberCount = normalized
        }
    }

    val displayedNames = remember(activeMemberCount, fetchedMemberNames) {
        buildPresetMembers(activeMemberCount, fetchedMemberNames)
    }

    val memberSummary = remember(activeMemberCount, displayedNames) {
        TripMemberSummary(
            memberCount = activeMemberCount,
            memberNames = displayedNames
        )
    }

    var recommendedPlaces by remember(selectedSchedulePlaces) {
        mutableStateOf(selectedSchedulePlaces)
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedPlaceDialog by remember { mutableStateOf<RecommendedPlace?>(null) }
    var isHotspotExpanded by remember { mutableStateOf(false) }

    val fallbackHotspotUrl = remember(destination) {
        reliableFallbackPhotoUrl("$destination 핫플레이스", "명소", 0)
    }
    var hotspotPhotoUrl by remember(destination) {
        mutableStateOf(fallbackHotspotUrl)
    }

    LaunchedEffect(destination, recommendedPlaces) {
        val targetPlace = recommendedPlaces.firstOrNull()
        val fetched = fetchBestMatchingPhotoUrl(
            placeName = targetPlace?.name ?: "$destination 핫플레이스",
            destination = destination,
            category = targetPlace?.category ?: "명소",
            pickIndex = 0
        )
        if (!fetched.isNullOrBlank()) {
            hotspotPhotoUrl = fetched
        }
    }

    DisposableEffect(tripId, selectedSchedulePlaces) {
        val gameCountListener = manager.listenToLatestGameMemberCount(
            tripId = tripId,
            onUpdate = { savedGameCount ->
                gameMemberCountFromDb = savedGameCount
                if (savedGameCount != null && !hasUserOverriddenCount) {
                    activeMemberCount = savedGameCount
                    onMemberCountChanged(savedGameCount)
                }
            }
        )

        val tripListener = manager.listenToTripMembers(
            tripId = tripId,
            fallbackCount = activeMemberCount,
            onUpdate = { summary ->
                if (summary.memberNames.isNotEmpty()) {
                    fetchedMemberNames = summary.memberNames
                }
                if (!hasUserOverriddenCount && gameMemberCountFromDb == null) {
                    val synced = summary.memberCount.coerceIn(2, 4)
                    activeMemberCount = synced
                    onMemberCountChanged(synced)
                }
            }
        )

        manager.getCachedAnalysisResult(
            tripId = tripId,
            onResult = { cached ->
                val cachedPlaces = cached?.places.orEmpty()
                recommendedPlaces = (selectedSchedulePlaces + cachedPlaces).distinctBy { it.name }
            },
            onFailure = { e ->
                Log.e("TripHomeScreen", "getCachedAnalysisResult failed", e)
            }
        )

        onDispose {
            gameCountListener.remove()
            tripListener.remove()
        }
    }

    val cycleMemberCount: () -> Unit = {
        val nextCount = if (activeMemberCount >= 4) 2 else activeMemberCount + 1
        hasUserOverriddenCount = true
        activeMemberCount = nextCount
        onMemberCountChanged(nextCount)
    }

    Scaffold(
        containerColor = Color(0xFFF7F9FC),
        bottomBar = {
            TripHomeBottomBar(
                selectedTab = selectedTab,
                mainBlue = mainBlue,
                onTabSelected = { selectedTab = it; onRoomTabSelected(it) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = mainBlue,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "뒤로가기",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "tripmate",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "편집",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = tripTitle,
                        color = Color.White,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateRange,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = darkBlueCard,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { cycleMemberCount() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val avatarColors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFFFF8A5B),
                            Color(0xFF34D399),
                            Color(0xFFA78BFA)
                        )
                        memberSummary.memberNames.take(4).forEachIndexed { idx, name ->
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(avatarColors[idx % avatarColors.size], CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name.take(1),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                .clickable { cycleMemberCount() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "2/3/4명 인원 전환",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "현재 ${memberSummary.memberCount}명",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "친구와 나의 케미는? 🙌",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFFB26B), Color(0xFFFF9843))
                            )
                        )
                        .clickable { onOpenChemiResult() }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFE599), RoundedCornerShape(20.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "밸런스 게임",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C4A03)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "우리 여행 스타일\n얼마나 잘 맞을까?",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1A1A1A),
                            lineHeight = 20.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = mainBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = accentOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "결과보기 →",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5C3A14)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .animateContentSize()
            ) {
                Text(
                    text = "꼭 가봐야 하는 $destination 핫플 🔥",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (!isHotspotExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable { isHotspotExpanded = true }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(hotspotPhotoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "$destination 핫플레이스",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE2E8F0)),
                            onError = {
                                if (hotspotPhotoUrl != fallbackHotspotUrl) {
                                    hotspotPhotoUrl = fallbackHotspotUrl
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "INSTARGRAM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF888888)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "나만 알고싶은\n숨은 $destination 핫플레이스",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A),
                                lineHeight = 17.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "펼치기",
                            tint = Color(0xFF999999)
                        )
                    }
                } else {
                    val featuredSpot = recommendedPlaces.firstOrNull()
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF1E293B))
                            .clickable { isHotspotExpanded = false }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(hotspotPhotoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "$destination TRAVEL SPOT",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onError = {
                                if (hotspotPhotoUrl != fallbackHotspotUrl) {
                                    hotspotPhotoUrl = fallbackHotspotUrl
                                }
                            }
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.25f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.72f)
                                        )
                                    )
                                )
                        )

                        Text(
                            text = "2026",
                            color = Color.White.copy(alpha = 0.92f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 14.dp)
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(28.dp)
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "접기",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "나만 알고 싶어서 숨겨둔",
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "TRAVEL SPOT",
                                color = Color.White,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.45f),
                                        blurRadius = 8f
                                    )
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (featuredSpot != null) {
                                    "$destination 대표 추천 스팟 · ${featuredSpot.name}"
                                } else {
                                    "사진 찍기 좋고 분위기 좋은 숨은 $destination 핫플레이스"
                                },
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "$destination, 이런 곳 어떠세요?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (recommendedPlaces.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                            .clickable { onOpenChemiResult() }
                            .padding(18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "밸런스 게임 결과를 확인하면 추천 장소가 표시돼요",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                } else {
                    val cardThemes = listOf(
                        Pair(Color(0xFF68A5F8), Color(0xFF2563EB)),
                        Pair(Color(0xFF7FD19C), Color(0xFF16A34A))
                    )
                    val rows = recommendedPlaces.chunked(2)
                    rows.forEachIndexed { rowIndex, rowPlaces ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowPlaces.forEachIndexed { colIndex, place ->
                                val idx = rowIndex * 2 + colIndex
                                val (cardBg, badgeText) = cardThemes[idx % cardThemes.size]
                                TripHomePlaceCard(
                                    place = place,
                                    destination = destination,
                                    cardBgColor = cardBg,
                                    badgeTextColor = badgeText,
                                    cardIndex = idx,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedPlaceDialog = place
                                        onSelectPlace(place)
                                    }
                                )
                            }
                            if (rowPlaces.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        if (rowIndex < rows.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    selectedPlaceDialog?.let { place ->
        AlertDialog(
            onDismissRequest = { selectedPlaceDialog = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            title = {
                Column {
                    Text(
                        text = place.category,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = mainBlue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = place.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A1A1A)
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = place.reason,
                        fontSize = 14.sp,
                        color = Color(0xFF444444),
                        lineHeight = 20.sp
                    )
                    if (place.tag.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = place.tag,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = mainBlue
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPlaceDialog = null }) {
                    Text("확인", fontWeight = FontWeight.Bold, color = mainBlue)
                }
            }
        )
    }
}

@Composable
private fun TripHomePlaceCard(
    place: RecommendedPlace,
    destination: String,
    cardBgColor: Color,
    badgeTextColor: Color,
    cardIndex: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val fallbackUrl = remember(place.name, place.category, cardIndex) {
        reliableFallbackPhotoUrl(place.name, place.category, cardIndex)
    }
    var photoUrl by remember(place.name, destination, cardIndex) {
        mutableStateOf(fallbackUrl)
    }

    LaunchedEffect(place.name, destination, place.category, cardIndex) {
        val fetched = fetchBestMatchingPhotoUrl(
            placeName = place.name,
            destination = destination,
            category = place.category,
            pickIndex = cardIndex
        )
        if (!fetched.isNullOrBlank()) {
            photoUrl = fetched
        }
    }

    Column(
        modifier = modifier
            .height(166.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(cardBgColor)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = place.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    if (photoUrl != fallbackUrl) {
                        photoUrl = fallbackUrl
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = place.category.ifBlank { "추천" },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeTextColor,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = place.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun reliableFallbackPhotoUrl(placeName: String, category: String, index: Int): String {
    val combined = "$placeName $category"
    val seaPhotos = listOf(
        "https://images.pexels.com/photos/457882/pexels-photo-457882.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1032650/pexels-photo-1032650.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1450353/pexels-photo-1450353.jpeg?auto=compress&cs=tinysrgb&w=600"
    )
    val foodPhotos = listOf(
        "https://images.pexels.com/photos/1640777/pexels-photo-1640777.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/70497/pexels-photo-70497.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/958545/pexels-photo-958545.jpeg?auto=compress&cs=tinysrgb&w=600"
    )
    val cafePhotos = listOf(
        "https://images.pexels.com/photos/302899/pexels-photo-302899.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/1855214/pexels-photo-1855214.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/683039/pexels-photo-683039.jpeg?auto=compress&cs=tinysrgb&w=600"
    )
    val travelPhotos = listOf(
        "https://images.pexels.com/photos/21014/pexels-photo-21014.jpg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/3225531/pexels-photo-3225531.jpeg?auto=compress&cs=tinysrgb&w=600",
        "https://images.pexels.com/photos/417074/pexels-photo-417074.jpeg?auto=compress&cs=tinysrgb&w=600"
    )

    val pool = when {
        combined.contains("해수욕장") || combined.contains("해변") || combined.contains("바다") ||
                combined.contains("오션") -> seaPhotos
        combined.contains("카페") || combined.contains("커피") || combined.contains("디저트") -> cafePhotos
        combined.contains("맛집") || combined.contains("식당") || combined.contains("음식") ||
                combined.contains("국밥") || combined.contains("갈비") -> foodPhotos
        else -> travelPhotos
    }
    val offset = abs(placeName.hashCode()) % pool.size
    return pool[(index + offset) % pool.size]
}

private suspend fun fetchBestMatchingPhotoUrl(
    placeName: String,
    destination: String,
    category: String,
    pickIndex: Int
): String? = withContext(Dispatchers.IO) {
    val exactQueries = listOf(placeName, "$destination $placeName").distinct()
    for (q in exactQueries) {
        searchWikipediaThumb("ko", q)?.let { return@withContext it }
        searchWikipediaThumb("en", q)?.let { return@withContext it }
    }

    val localQueries = listOf(
        "$destination $placeName",
        "$destination ${category.ifBlank { "명소" }}",
        "$destination 여행",
        destination
    ).distinct()

    for (q in localQueries) {
        val candidates = searchCommonsPhotos(q)
        if (candidates.isNotEmpty()) {
            return@withContext candidates[pickIndex % candidates.size]
        }
    }

    null
}

private fun searchWikipediaThumb(lang: String, query: String): String? {
    return try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val wikiUrl =
            "https://$lang.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch=$encoded&gsrlimit=2&prop=pageimages&pithumbsize=600&format=json"
        val conn = (URL(wikiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "TripMateApp/1.0 (Android)")
            connectTimeout = 3500
            readTimeout = 3500
        }
        if (conn.responseCode == 200) {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val pages = JSONObject(body).optJSONObject("query")?.optJSONObject("pages")
            if (pages != null) {
                val keys = pages.keys()
                while (keys.hasNext()) {
                    val pageObj = pages.getJSONObject(keys.next())
                    val thumb = pageObj.optJSONObject("thumbnail")?.optString("source")
                    if (!thumb.isNullOrBlank()) {
                        return thumb
                    }
                }
            }
        }
        null
    } catch (_: Exception) {
        null
    }
}

private fun searchCommonsPhotos(query: String): List<String> {
    return try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val commonsUrl =
            "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=$encoded&gsrnamespace=6&gsrlimit=6&prop=imageinfo&iiprop=url&iiurlwidth=600&format=json"
        val conn = (URL(commonsUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("User-Agent", "TripMateApp/1.0 (Android)")
            connectTimeout = 3500
            readTimeout = 3500
        }
        val results = mutableListOf<String>()
        if (conn.responseCode == 200) {
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val pages = JSONObject(body).optJSONObject("query")?.optJSONObject("pages")
            if (pages != null) {
                val keys = pages.keys()
                while (keys.hasNext()) {
                    val pageObj = pages.getJSONObject(keys.next())
                    val title = pageObj.optString("title", "").lowercase()
                    val isPhoto = title.endsWith(".jpg") || title.endsWith(".jpeg")
                    val isNoise = title.contains("map") || title.contains("logo") ||
                            title.contains("icon") || title.contains("flag")
                    if (isPhoto && !isNoise) {
                        val thumbUrl = pageObj.optJSONArray("imageinfo")
                            ?.optJSONObject(0)
                            ?.optString("thumburl")
                        if (!thumbUrl.isNullOrBlank()) {
                            results.add(thumbUrl)
                        }
                    }
                }
            }
        }
        results
    } catch (_: Exception) {
        emptyList()
    }
}

@Composable
private fun TripHomeBottomBar(
    selectedTab: Int,
    mainBlue: Color,
    onTabSelected: (Int) -> Unit
) {
    val items: List<Pair<String, ImageVector>> = listOf(
        "여행홈" to Icons.Default.Home,
        "일정" to Icons.Default.DateRange,
        "가계부" to Icons.Default.Place
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 6.dp
    ) {
        items.forEachIndexed { index, (label, icon) ->
            val selected = selectedTab == index
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = mainBlue,
                    selectedTextColor = mainBlue,
                    unselectedIconColor = Color(0xFF9CA3AF),
                    unselectedTextColor = Color(0xFF9CA3AF),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}