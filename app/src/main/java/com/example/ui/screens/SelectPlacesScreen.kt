package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Place
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPlacesScreen(
    places: List<Place>,
    onBackClick: () -> Unit,
    onConfirmedSelection: (List<Place>) -> Unit,
    modifier: Modifier = Modifier
) {
    var placeList by remember(places) {
        mutableStateOf(places.sortedByDescending { it.matchRate })
    }

    val selectedCount = placeList.count { it.isSelected }
    val isAnySelected = selectedCount > 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "갈 곳 정하기",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "우리의 여행 취향을 바탕으로 추천했어요.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("select_places_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Button(
                        onClick = {
                            val selected = placeList.filter { it.isSelected }
                            if (selected.isNotEmpty()) {
                                onConfirmedSelection(selected)
                            }
                        },
                        enabled = isAnySelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("confirm_selection_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TravelBluePrimary,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFE2E8F0),
                            disabledContentColor = Color(0xFF94A3B8)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = "선택 완료",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp)
        ) {
            items(placeList, key = { it.id }) { place ->
                val backgroundColor = if (place.isSelected) Color(0xFFEFF6FF) else Color.White
                val borderColor = if (place.isSelected) TravelBluePrimary else Color(0xFFCBD5E1)
                val borderWidth = if (place.isSelected) 1.5.dp else 1.dp

                OutlinedCard(
                    onClick = {
                        placeList = placeList.map {
                            if (it.id == place.id) it.copy(isSelected = !it.isSelected) else it
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("place_select_card_${place.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = backgroundColor
                    ),
                    border = BorderStroke(borderWidth, borderColor),
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
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = place.isSelected,
                            onCheckedChange = { checked ->
                                placeList = placeList.map {
                                    if (it.id == place.id) it.copy(isSelected = checked) else it
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = TravelBluePrimary,
                                uncheckedColor = Color(0xFF94A3B8)
                            )
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = place.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            if (place.categories.isNotEmpty()) {
                                Text(
                                    text = place.categories.joinToString(" · "),
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }

                            if (place.matchRate > 0) {
                                Text(
                                    text = "${place.matchRate}% 잘 맞아요",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TravelBluePrimary
                                )
                            } else {
                                Text(
                                    text = "0% 잘 맞아요",
                                    fontSize = 13.sp,
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
