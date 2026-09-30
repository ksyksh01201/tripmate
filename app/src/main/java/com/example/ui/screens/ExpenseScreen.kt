package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.*
import com.example.service.SettlementCalculator
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    trip: Trip,
    expenses: List<Expense>,
    onBackClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    onDeleteExpense: (Expense) -> Unit = {},
    onUpdateExpense: (Expense) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSubTabIndex by remember { mutableIntStateOf(0) } // 0: 내역, 1: 정산
    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.KOREA) }

    // 여행별 지출 데이터 완벽 격리 (현재 여행 trip.id 기준 필터링)
    val currentTripExpenses = remember(expenses, trip.id) {
        expenses.filter { it.tripId == trip.id || it.tripId.isEmpty() }
    }

    // 총 지출 자동 계산 (현재 여행의 실제 지출 amount 합산)
    val totalExpenseAmount = remember(currentTripExpenses) { currentTripExpenses.sumOf { it.amount } }

    // 터치 시 수정/삭제할 대상 지출
    var selectedExpenseForAction by remember { mutableStateOf<Expense?>(null) }

    // 1/N 정산 계산 (SettlementCalculator 사용 - 현재 여행의 members와 currentTripExpenses 사용)
    val memberBalances = remember(currentTripExpenses, trip.members) {
        SettlementCalculator.calculateMemberBalances(trip.members, currentTripExpenses)
    }

    var transfers by remember(memberBalances) {
        mutableStateOf(SettlementCalculator.calculateSettlementTransfers(memberBalances))
    }

    // MPAndroidChart 대응 분리된 카테고리별 지출 데이터 계산 (현재 여행의 currentTripExpenses 사용)
    val categoryTotals = remember(currentTripExpenses) {
        SettlementCalculator.calculateCategoryTotals(currentTripExpenses)
    }

    val currentUser = remember(trip.members) {
        trip.members.find { it.isCurrentUser } ?: trip.members.firstOrNull()
    }
    val currentUserId = currentUser?.id ?: "user_me"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "여행 경비",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = trip.title,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("expense_back_button")
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
            // [4. 경비 화면 상단] 현재 여행 이름과 총 지출
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = trip.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "총 지출",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${numberFormatter.format(totalExpenseAmount)}원",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // 내역 | 정산 탭
            TabRow(
                selectedTabIndex = selectedSubTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                val tabs = listOf("내역", "정산")
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedSubTabIndex == index,
                        onClick = { selectedSubTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedSubTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 15.sp
                            )
                        }
                    )
                }
            }

            when (selectedSubTabIndex) {
                0 -> {
                    // [3. 경비 내역 화면]
                    // 각 지출에 큰 수정/삭제 버튼을 항상 표시하지 않고 터치했을 때만 수정 또는 삭제
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                    ) {
                        if (currentTripExpenses.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "등록된 지출 내역이 없습니다.\n아래 버튼을 눌러 지출을 추가해보세요.",
                                        fontSize = 14.sp,
                                        color = TextSecondary,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        } else {
                            items(currentTripExpenses) { exp ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("expense_item_${exp.expenseId}")
                                        .clickable {
                                            selectedExpenseForAction = exp
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = exp.title,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${exp.category} · ${exp.payerName}",
                                                fontSize = 13.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Text(
                                            text = "${numberFormatter.format(exp.amount)}원",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = onAddExpenseClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("add_expense_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = "+ 지출 추가",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // [5. 정산 탭]
                    // [8. 정산 화면 정보 개선] & [9. 반대 상황도 처리]
                    val myBalance = memberBalances.find { it.isCurrentUser } ?: memberBalances.firstOrNull()
                    val myPaid = myBalance?.paidAmount ?: 0L
                    val myOwed = myBalance?.owedAmount ?: 0L
                    val myNet = myBalance?.netBalance ?: 0L

                    val myTransfers = transfers.filter { it.fromUserId == currentUserId }
                    val otherTransfers = transfers.filter { it.fromUserId != currentUserId }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
                    ) {
                        // 1. [8. 내 정산 카드] 내가 결제한 금액, 내 부담 금액, 내가 받을 돈 / 내가 보낼 돈
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "내 정산",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("my_settlement_card"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // 내가 결제한 금액
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "내가 결제한 금액",
                                                fontSize = 14.sp,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "${numberFormatter.format(myPaid)}원",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        }

                                        // 내 부담 금액
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "내 부담 금액",
                                                fontSize = 14.sp,
                                                color = TextSecondary
                                            )
                                            Text(
                                                text = "${numberFormatter.format(myOwed)}원",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary
                                            )
                                        }

                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )

                                        // 내가 받을 돈 / 내가 보낼 돈 / 정산 완료
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val label = when {
                                                myNet > 0 -> "내가 받을 돈"
                                                myNet < 0 -> "내가 보낼 돈"
                                                else -> "정산 금액"
                                            }
                                            val amountColor = when {
                                                myNet > 0 -> SuccessGreen
                                                myNet < 0 -> MaterialTheme.colorScheme.primary
                                                else -> TextSecondary
                                            }
                                            val amountText = when {
                                                myNet != 0L -> "${numberFormatter.format(kotlin.math.abs(myNet))}원"
                                                else -> "0원"
                                            }

                                            Text(
                                                text = label,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = amountText,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = amountColor
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 내가 보낼 돈 (myTransfers에 항목이 있는 경우 송금 카드 노출)
                        if (myTransfers.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "송금할 내역",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    myTransfers.forEach { transfer ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("my_transfer_${transfer.id}"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "${numberFormatter.format(transfer.amount)}원",
                                                        fontSize = 18.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${transfer.toUserName}에게",
                                                        fontSize = 14.sp,
                                                        color = TextSecondary
                                                    )
                                                }

                                                Button(
                                                    onClick = {
                                                        transfers = transfers.map {
                                                            if (it.id == transfer.id) it.copy(isCompleted = !it.isCompleted) else it
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (transfer.isCompleted) SuccessGreen else MaterialTheme.colorScheme.primary
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = if (transfer.isCompleted) "송금 완료" else "송금 완료",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. 다른 정산
                        if (otherTransfers.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "다른 정산",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    otherTransfers.forEach { transfer ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("other_transfer_${transfer.id}"),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = transfer.fromUserName,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextPrimary
                                                    )
                                                    Text(" → ", fontSize = 14.sp, color = TextSecondary)
                                                    Text(
                                                        text = transfer.toUserName,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                Text(
                                                    text = "${numberFormatter.format(transfer.amount)}원",
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // [10. 이미 정확히 정산된 경우]
                        if (transfers.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "정산할 금액이 없습니다.",
                                        fontSize = 14.sp,
                                        color = TextSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // 구분선 (-----------------------)
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }

                        // 3. [14. 카테고리별 막대그래프 유지]
                        item {
                            CategoryBarChart(
                                categoryTotals = categoryTotals,
                                modifier = Modifier.testTag("category_bar_chart")
                            )
                        }
                    }
                }
            }
        }
    }

    // 지출 항목 터치 시 노출되는 수정 / 삭제 다이얼로그
    selectedExpenseForAction?.let { expense ->
        ExpenseEditDeleteDialog(
            expense = expense,
            members = trip.members,
            onDismiss = { selectedExpenseForAction = null },
            onUpdate = { updatedExp ->
                onUpdateExpense(updatedExp)
                selectedExpenseForAction = null
            },
            onDelete = { expToDelete ->
                onDeleteExpense(expToDelete)
                selectedExpenseForAction = null
            }
        )
    }
}

/**
 * 카테고리별 지출 막대그래프 컴포넌트
 *
 * [7. MPAndroidChart 적용을 고려한 구조]
 * 추후 Android Studio에서 MPAndroidChart의 BarChart로 교체 시:
 *   val entries = categoryTotals.mapIndexed { index, item ->
 *       BarEntry(index.toFloat(), item.totalAmount.toFloat())
 *   }
 *   val dataSet = BarDataSet(entries, "카테고리별 지출")
 *   val barData = BarData(dataSet)
 *   barChart.data = barData
 *
 * 현재는 데이터 로직과 분리된 순수 Compose 막대그래프로 렌더링합니다.
 */
@Composable
fun CategoryBarChart(
    categoryTotals: List<CategoryTotal>,
    modifier: Modifier = Modifier
) {
    val numberFormatter = remember { NumberFormat.getNumberInstance(Locale.KOREA) }
    val maxAmount = categoryTotals.maxOfOrNull { it.totalAmount }?.coerceAtLeast(1L) ?: 1L

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
                text = "카테고리별 지출",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categoryTotals.forEach { item ->
                    val ratio = if (maxAmount > 0) {
                        (item.totalAmount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 카테고리명 (식비, 숙박, 교통, 관광, 쇼핑, 기타)
                        Text(
                            text = item.category,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            modifier = Modifier.width(38.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // 막대그래프 Bar (비율에 맞춰 너비 설정)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(14.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(7.dp)
                                )
                        ) {
                            if (ratio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = ratio)
                                        .fillMaxHeight()
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(7.dp)
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // 지출 금액
                        Text(
                            text = "${numberFormatter.format(item.totalAmount)}원",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.totalAmount > 0) TextPrimary else TextSecondary,
                            modifier = Modifier.widthIn(min = 68.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 지출 항목 터치 시 열리는 수정 / 삭제 다이얼로그
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseEditDeleteDialog(
    expense: Expense,
    members: List<TripMember>,
    onDismiss: () -> Unit,
    onUpdate: (Expense) -> Unit,
    onDelete: (Expense) -> Unit
) {
    var title by remember { mutableStateOf(expense.title) }
    var amountText by remember { mutableStateOf(expense.amount.toString()) }

    val categories = remember { EXPENSE_CATEGORIES }
    var selectedCategory by remember { mutableStateOf(expense.category) }
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }

    var selectedPayerId by remember { mutableStateOf(expense.payerId) }
    var isPayerDropdownExpanded by remember { mutableStateOf(false) }

    val selectedParticipantIds = remember {
        mutableStateListOf<String>().apply {
            addAll(expense.participantIds)
        }
    }

    val selectedPayer = members.find { it.id == selectedPayerId } ?: members.firstOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "지출 수정 / 삭제",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // 지출 내용
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("지출 내용") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // 금액
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() } },
                    label = { Text("금액") },
                    suffix = { Text("원") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                // 카테고리
                ExposedDropdownMenuBox(
                    expanded = isCategoryDropdownExpanded,
                    onExpandedChange = { isCategoryDropdownExpanded = !isCategoryDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("카테고리") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isCategoryDropdownExpanded,
                        onDismissRequest = { isCategoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    isCategoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 결제한 사람
                ExposedDropdownMenuBox(
                    expanded = isPayerDropdownExpanded,
                    onExpandedChange = { isPayerDropdownExpanded = !isPayerDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedPayer?.name ?: "선택",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("결제한 사람") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPayerDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isPayerDropdownExpanded,
                        onDismissRequest = { isPayerDropdownExpanded = false }
                    ) {
                        members.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m.name) },
                                onClick = {
                                    selectedPayerId = m.id
                                    isPayerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // 함께 부담할 사람
                Text(
                    text = "함께 부담할 사람",
                    fontSize = 13.sp,
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
                                    if (selectedParticipantIds.size > 1) selectedParticipantIds.remove(m.id)
                                } else {
                                    selectedParticipantIds.add(m.id)
                                }
                            }
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = m.name, fontSize = 14.sp, color = TextPrimary)
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (checked) selectedParticipantIds.add(m.id)
                                else if (selectedParticipantIds.size > 1) selectedParticipantIds.remove(m.id)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 액션 버튼들: [삭제] (좌측) / [취소] [저장] (우측)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onDelete(expense) },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("삭제", fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onDismiss) {
                            Text("취소")
                        }

                        val amountLong = amountText.toLongOrNull() ?: 0L
                        val isValid = title.isNotBlank() && amountLong > 0 && selectedParticipantIds.isNotEmpty()

                        Button(
                            onClick = {
                                if (isValid) {
                                    val payer = selectedPayer ?: members.first()
                                    val selectedMembers = members.filter { selectedParticipantIds.contains(it.id) }
                                    val updatedExpense = expense.copy(
                                        title = title.trim(),
                                        amount = amountLong,
                                        category = selectedCategory,
                                        payerId = payer.id,
                                        payerName = payer.name,
                                        participantIds = selectedParticipantIds.toList(),
                                        participantNames = selectedMembers.map { it.name }
                                    )
                                    onUpdate(updatedExpense)
                                }
                            },
                            enabled = isValid,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("저장", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
