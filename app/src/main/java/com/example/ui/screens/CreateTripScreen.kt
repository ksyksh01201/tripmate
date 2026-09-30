package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Trip
import com.example.model.TripMember
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTripScreen(
    onBackClick: () -> Unit,
    onTripCreated: (Trip) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // [1. 새 여행 생성 시 모든 입력값 초기화 - 빈 상태로 시작]
    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var expectedCountText by remember { mutableStateOf("") }

    val popularDestinations = listOf("부산", "제주", "강릉", "경주", "여수", "오사카")

    fun isEndDateBeforeStartDate(start: String, end: String): Boolean {
        val startNums = start.split(".", "/", "-").mapNotNull { it.trim().toIntOrNull() }
        val endNums = end.split(".", "/", "-").mapNotNull { it.trim().toIntOrNull() }
        if (startNums.isNotEmpty() && endNums.isNotEmpty() && startNums.size == endNums.size) {
            for (i in startNums.indices) {
                if (endNums[i] < startNums[i]) return true
                if (endNums[i] > startNums[i]) return false
            }
            return false
        }
        return false
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "새로운 여행 만들기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("create_trip_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            val trimmedTitle = title.trim()
                            val trimmedDest = destination.trim()
                            val trimmedStart = startDate.trim()
                            val trimmedEnd = endDate.trim()
                            val count = expectedCountText.filter { it.isDigit() }.toIntOrNull() ?: 0

                            // [6. 여행방 만들기 버튼 필수값 검증]
                            if (trimmedTitle.isEmpty() || trimmedDest.isEmpty() || trimmedStart.isEmpty() || trimmedEnd.isEmpty() || count <= 0) {
                                Toast.makeText(context, "여행 정보를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // [7. 날짜 검증]
                            if (isEndDateBeforeStartDate(trimmedStart, trimmedEnd)) {
                                Toast.makeText(context, "종료일은 시작일 이후로 선택해주세요.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // [8. 여행 생성 - 최초 참여자는 방장 1명만 존재]
                            val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                            val randomCode = "TRIP" + (1..4).map { chars.random() }.joinToString("")
                            val membersList = listOf(
                                TripMember("user_me", "나 (방장)", isCurrentUser = true, isOwner = true, preferenceCompleted = false)
                            )

                            val newTrip = Trip(
                                id = "trip_${System.currentTimeMillis()}",
                                title = trimmedTitle,
                                destination = trimmedDest,
                                startDate = trimmedStart,
                                endDate = trimmedEnd,
                                expectedCount = count,
                                inviteCode = randomCode,
                                status = "취향 분석 중",
                                members = membersList,
                                expenses = emptyList(),
                                daySchedules = emptyList(),
                                selectedPlaces = emptyList(),
                                personalResult = null
                            )
                            onTripCreated(newTrip)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_create_trip_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "여행방 만들기",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = "여행 기본 정보", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    // [2. 여행 이름]
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("여행 이름") },
                        placeholder = { Text("여행 이름을 입력하세요") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trip_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // [3. 여행 목적지]
                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("여행 목적지") },
                        placeholder = { Text("도시를 선택하세요") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trip_destination_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Column {
                        Text(text = "인기 추천 여행지", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(popularDestinations) { city ->
                                val isSelected = destination == city
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) TravelBlueLight else SurfaceVariantLight,
                                    modifier = Modifier.clickable { destination = city }
                                ) {
                                    Text(
                                        text = city,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) TravelBlueDark else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(text = "일정 및 인원", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

                    // [4. 시작일 / 종료일]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("시작일") },
                            placeholder = { Text("날짜 선택") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = endDate,
                            onValueChange = { endDate = it },
                            label = { Text("종료일") },
                            placeholder = { Text("날짜 선택") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                capitalization = KeyboardCapitalization.None,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // [5. 예상 인원]
                    OutlinedTextField(
                        value = expectedCountText,
                        onValueChange = { expectedCountText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("예상 인원 (명)") },
                        placeholder = { Text("인원을 입력하세요") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}
