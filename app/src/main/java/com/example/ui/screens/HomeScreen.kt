package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Trip
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    trips: List<Trip>,
    onTripClick: (Trip) -> Unit,
    onCreateTripClick: () -> Unit,
    onJoinByCode: (String) -> Unit,
    onLogoutClick: () -> Unit,
    onViewScheduleClick: (Trip?) -> Unit = {},
    onExpenseTabClick: () -> Unit = {},
    onMyPageClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showJoinDialog by remember { mutableStateOf(false) }
    var inputInviteCode by remember { mutableStateOf("") }
    var selectedBottomNavIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        selectedBottomNavIndex = 0
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "트립메이트",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TravelBluePrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                val navItems = listOf(
                    Triple("홈", Icons.Default.Home, 0),
                    Triple("일정", Icons.Default.CalendarMonth, 1),
                    Triple("경비", Icons.Default.AccountBalanceWallet, 2),
                    Triple("마이", Icons.Default.Person, 3)
                )
                navItems.forEach { (title, icon, index) ->
                    val isSelected = selectedBottomNavIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            selectedBottomNavIndex = index
                            when (index) {
                                0 -> { /* Home */ }
                                1 -> onViewScheduleClick(null)
                                2 -> onExpenseTabClick()
                                3 -> onMyPageClick()
                            }
                        },
                        icon = {
                            Icon(
                                icon,
                                contentDescription = title,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary
                            )
                        },
                        label = {
                            Text(
                                title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else TextSecondary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // 섹션 타이틀: 내 여행
            item {
                Text(
                    text = "내 여행",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // 여행 목록
            if (trips.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "아직 여행이 없어요.",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "새로운 여행을 만들어보세요.",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(trips, key = { it.id }) { trip ->
                    OutlinedCard(
                        onClick = { onTripClick(trip) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trip_card_${trip.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = Color.White
                        ),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        elevation = CardDefaults.outlinedCardElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            focusedElevation = 0.dp,
                            hoveredElevation = 0.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = trip.title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${trip.startDate} - ${trip.endDate} · ${trip.expectedCount}명",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "여행방 이동",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // [+ 여행 만들기] & 초대코드로 참여하기
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCreateTripClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("create_trip_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TravelBluePrimary
                        ),
                        border = BorderStroke(1.dp, TravelBluePrimary.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "+ 여행 만들기",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    TextButton(
                        onClick = { showJoinDialog = true },
                        modifier = Modifier.testTag("join_by_code_button")
                    ) {
                        Text(
                            text = "초대코드로 참여하기",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // 초대코드 참여 다이얼로그
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = {
                Text(
                    text = "초대코드로 참여하기",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = inputInviteCode,
                    onValueChange = { inputInviteCode = it.uppercase() },
                    placeholder = { Text("초대코드를 입력하세요") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("invite_code_input"),
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputInviteCode.isNotBlank()) {
                            onJoinByCode(inputInviteCode.trim())
                            showJoinDialog = false
                            inputInviteCode = ""
                        }
                    },
                    modifier = Modifier.testTag("submit_join_code_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("참여하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}
