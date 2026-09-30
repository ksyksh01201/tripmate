package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EXPENSE_CATEGORIES
import com.example.model.Expense
import com.example.model.TripMember
import com.example.ui.theme.*
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    tripId: String,
    members: List<TripMember>,
    onBackClick: () -> Unit,
    onSaveExpense: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }

    // 카테고리: 식비, 숙박, 교통, 관광, 쇼핑, 기타 6개 고정
    val categories = remember { EXPENSE_CATEGORIES }
    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    // 결제한 사람
    var selectedPayerId by remember {
        mutableStateOf(members.find { it.isCurrentUser }?.id ?: members.firstOrNull()?.id ?: "user_me")
    }
    var isPayerDropdownExpanded by remember { mutableStateOf(false) }

    // 함께 부담할 사람 (기본: 전체 선택)
    val selectedParticipantIds = remember {
        mutableStateListOf<String>().apply {
            addAll(members.map { it.id })
        }
    }

    val selectedPayer = members.find { it.id == selectedPayerId } ?: members.firstOrNull()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "지출 추가",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("add_expense_back_button")
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
                    val amountLong = amountText.toLongOrNull() ?: 0L
                    val isFormValid = title.isNotBlank() && amountLong > 0 && selectedParticipantIds.isNotEmpty()

                    Button(
                        onClick = {
                            if (isFormValid) {
                                val payer = selectedPayer ?: members.first()
                                val selectedMembers = members.filter { selectedParticipantIds.contains(it.id) }
                                val newExpense = Expense(
                                    expenseId = "exp_${UUID.randomUUID().toString().take(6)}",
                                    tripId = tripId,
                                    title = title.trim(),
                                    amount = amountLong,
                                    category = selectedCategory,
                                    payerId = payer.id,
                                    payerName = payer.name,
                                    participantIds = selectedParticipantIds.toList(),
                                    participantNames = selectedMembers.map { it.name },
                                    date = ""
                                )
                                onSaveExpense(newExpense)
                            }
                        },
                        enabled = isFormValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("save_expense_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "저장",
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
            // 지출 내용 & 금액
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
                    Text(
                        text = "지출 내용",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("예: 저녁 식사") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "금액",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            amountText = input.filter { it.isDigit() }
                        },
                        placeholder = { Text("예: 80,000") },
                        suffix = { Text("원") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 카테고리 [ 식비 ▼ ]
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "카테고리",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    ExposedDropdownMenuBox(
                        expanded = isCategoryDropdownExpanded,
                        onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("expense_category_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isCategoryDropdownExpanded,
                            onDismissRequest = { isCategoryDropdownExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontSize = 15.sp) },
                                    onClick = {
                                        selectedCategory = cat
                                        isCategoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 결제한 사람 [ 김철수 ▼ ]
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "결제한 사람",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    ExposedDropdownMenuBox(
                        expanded = isPayerDropdownExpanded,
                        onExpandedChange = { isPayerDropdownExpanded = !isPayerDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedPayer?.name ?: "선택",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPayerDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("expense_payer_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = isPayerDropdownExpanded,
                            onDismissRequest = { isPayerDropdownExpanded = false }
                        ) {
                            members.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.name, fontSize = 15.sp) },
                                    onClick = {
                                        selectedPayerId = m.id
                                        isPayerDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 함께 부담할 사람
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "함께 부담할 사람 (${selectedParticipantIds.size}명)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    members.forEach { m ->
                        val isChecked = selectedParticipantIds.contains(m.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) {
                                        if (selectedParticipantIds.size > 1) {
                                            selectedParticipantIds.remove(m.id)
                                        }
                                    } else {
                                        selectedParticipantIds.add(m.id)
                                    }
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = m.name,
                                fontSize = 15.sp,
                                color = TextPrimary,
                                fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal
                            )
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        selectedParticipantIds.add(m.id)
                                    } else if (selectedParticipantIds.size > 1) {
                                        selectedParticipantIds.remove(m.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
