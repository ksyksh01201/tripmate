package com.example.ui.screens

import com.example.data.TripMemberNames
import com.example.ui.components.CategoryColor
import com.example.ui.components.CategoryPalette
import com.example.ui.components.MemberNameCache
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.BudgetCalculator
import com.example.data.Expense
import com.example.data.ExpenseCategories
import com.example.data.Trip
import com.example.ui.viewmodel.BudgetViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 피그마 "가계부 · 지출 목록" 색상
private val BudgetBlue = Color(0xFF4A90E2)
private val BudgetText = Color(0xFF202020)
private val BudgetGray = Color(0xFF727272)
private val BudgetLine = Color(0xFFDCE6F2)
private val ExpenseBorder = Color(0xFFEEEEEE)
private val TabSelectedBlue = Color(0xFF197CF0)
private val BudgetMemberColors = listOf(
    Color(0xFF262452), Color(0xFFFF7A4D), Color(0xFF4A90E2),
    Color(0xFF34C38F), Color(0xFF9B6BDF), Color(0xFFF5A623)
)

private enum class BudgetTab(val label: String) { List("지출 목록"), Analysis("소비 분석"), Settle("정산하기") }

/** 멤버 표시 이름. 나는 "나", 다른 사람은 여행방에 저장된 이름 (없으면 멤버2 …) */
internal fun memberName(uid: String, members: List<String>, myUid: String?): String =
    if (uid == myUid) "나"
    else MemberNameCache.names[uid]?.takeIf { it.isNotBlank() } ?: "멤버${members.indexOf(uid) + 1}"

private fun memberInitial(uid: String, members: List<String>, myUid: String?): String =
    if (uid == myUid) "나"
    else MemberNameCache.names[uid]?.takeIf { it.isNotBlank() }?.take(1) ?: "${members.indexOf(uid) + 1}"

private fun memberColor(uid: String, members: List<String>): Color =
    BudgetMemberColors[members.indexOf(uid).coerceAtLeast(0) % BudgetMemberColors.size]

internal fun won(amount: Long): String = NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"

/**
 * 여행룸 하단 [가계부] 탭 내용 (피그마 새 UI).
 * [지출 목록 | 소비 분석 | 정산하기] → (지출 목록) 총 공동 지출액 카드 → 지출 내역
 * [지출 추가] 버튼은 목록을 스크롤해도 화면 아래에 떠 있습니다.
 */
@Composable
fun BudgetContent(trip: Trip, myUid: String?) {
    val viewModel: BudgetViewModel = viewModel(
        key = "budget_${trip.tripId}",
        factory = viewModelFactory { initializer { BudgetViewModel(trip.tripId) } }
    )
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val loadError by viewModel.loadError.collectAsStateWithLifecycle()
    val addState by viewModel.addState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(BudgetTab.List) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    // 수정 중인 지출 id (지출 목록에서 항목을 누르면 설정)
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }

    // 저장이 끝나면 팝업 닫기
    LaunchedEffect(addState.savedCount) {
        if (addState.savedCount > 0) {
            showAdd = false
            editingId = null
        }
    }
    // 멤버 이름 받아 오기 (결제한 사람 / 나눌 사람에 표시)
    LaunchedEffect(trip.tripId) {
        runCatching {
            TripMemberNames.observe(trip.tripId).collect { MemberNameCache.names.putAll(it) }
        }
    }

    val members = trip.members
    val list = expenses.orEmpty()
    val summary = remember(members, list) { BudgetCalculator.summarize(members, list) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            BudgetTabs(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(start = 21.dp, end = 21.dp, top = 18.dp, bottom = 18.dp)
            )

            when (tab) {
                BudgetTab.List -> {
                    TotalSpendCard(
                        total = summary.total,
                        perPersonText = splitSummaryText(summary.total, members, list, summary.share[myUid]),
                        members = members,
                        myUid = myUid,
                        paid = summary.paid,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(17.dp))
                    ExpenseListSection(
                        expenses = expenses,
                        loadError = loadError,
                        members = members,
                        myUid = myUid,
                        onClick = { e ->
                            viewModel.clearError()
                            editingId = e.expenseId
                        }
                    )
                    // 떠 있는 [지출 추가] 버튼에 마지막 지출이 가리지 않게 여백
                    Spacer(Modifier.height(80.dp))
                }
                BudgetTab.Analysis -> SpendingAnalysisSection(expenses = list)
                BudgetTab.Settle -> SettlementSection(
                    members = members,
                    myUid = myUid,
                    expenses = list,
                    summary = summary
                )
            }
        }

        // 지출 목록에서만 아래에 떠 있는 [지출 추가]
        if (tab == BudgetTab.List) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 17.dp)
                    .width(136.dp)
                    .height(48.dp)
                    .shadow(4.dp, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .background(BudgetBlue)
                    .clickable {
                        viewModel.clearError()
                        showAdd = true
                    }
                    .testTag("budget_add_expense_button"),
                contentAlignment = Alignment.Center
            ) {
                Text("지출 추가", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    if (showAdd) {
        AddExpenseDialog(
            members = members,
            myUid = myUid,
            isSaving = addState.isSaving,
            errorMessage = addState.errorMessage,
            onDismiss = { showAdd = false },
            onSave = { title, amount, category, paidBy, splitAmong ->
                viewModel.addExpense(title, amount, category, paidBy, splitAmong, System.currentTimeMillis())
            }
        )
    }

    // 지출 목록 항목을 누르면: 지난번에 작성한 내용이 채워진 수정 팝업
    val editing = editingId?.let { id -> list.find { it.expenseId == id } }
    if (editing != null) {
        AddExpenseDialog(
            members = members,
            myUid = myUid,
            isSaving = addState.isSaving,
            errorMessage = addState.errorMessage,
            onDismiss = { editingId = null },
            initial = editing,
            onSave = { title, amount, category, paidBy, splitAmong ->
                viewModel.updateExpense(editing.expenseId, title, amount, category, paidBy, splitAmong)
            }
        )
    }
}

/** "2명 · 1인당 90,000원" (모든 지출을 전원이 나눌 때) / 아니면 "내 부담 OO원" */
private fun splitSummaryText(total: Long, members: List<String>, expenses: List<Expense>, myShare: Long?): String {
    if (members.isEmpty() || total == 0L) return ""
    val everyoneSplits = expenses.all { it.splitAmong.isEmpty() || it.splitAmong.toSet() == members.toSet() }
    return if (everyoneSplits) {
        "${members.size}명 · 1인당 ${won(total / members.size)}"
    } else {
        "내 부담 ${won(myShare ?: 0)}"
    }
}

/** [지출 목록 | 소비 분석 | 정산하기] 한 줄 탭 (사이에 세로 구분선, 선택된 탭은 파란 글씨) */
@Composable
private fun BudgetTabs(selected: BudgetTab, onSelect: (BudgetTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, BudgetLine, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BudgetTab.entries.forEachIndexed { i, t ->
            if (i > 0) {
                Box(
                    Modifier
                        .width(1.dp)
                        .height(26.dp)
                        .background(BudgetLine)
                )
            }
            val isSelected = t == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(t) }
                    .testTag("budget_tab_${t.name}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = t.label,
                    fontSize = if (isSelected) 14.sp else 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) TabSelectedBlue else BudgetGray
                )
            }
        }
    }
}

/** 총 공동 지출액 카드: 큰 금액 + 오른쪽 아래 멤버별 결제 금액 */
@Composable
private fun TotalSpendCard(
    total: Long,
    perPersonText: String,
    members: List<String>,
    myUid: String?,
    paid: Map<String, Long>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(10.dp), ambientColor = BudgetBlue, spotColor = BudgetBlue)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, BudgetBlue, RoundedCornerShape(10.dp))
            .padding(start = 23.dp, end = 14.dp, top = 13.dp, bottom = 12.dp)
            .testTag("budget_member_card")
    ) {
        Text("총 공동 지출액", fontSize = 13.sp, color = BudgetGray)
        Spacer(Modifier.height(6.dp))
        Text(
            text = won(total),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = BudgetText,
            modifier = Modifier.testTag("budget_total")
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            // 1/N 계산 결과
            Text(
                text = perPersonText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = BudgetBlue,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 2.dp)
                    .testTag("budget_per_person")
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                members.forEachIndexed { index, uid ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .padding(horizontal = 10.dp)
                                .width(1.dp)
                                .height(26.dp)
                                .background(BudgetLine)
                        )
                    }
                    Column {
                        Text(memberName(uid, members, myUid), fontSize = 10.sp, color = BudgetGray)
                        Text(won(paid[uid] ?: 0), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BudgetText)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseListSection(
    expenses: List<Expense>?,
    loadError: String?,
    members: List<String>,
    myUid: String?,
    onClick: (Expense) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            loadError != null -> Text(loadError, fontSize = 13.sp, color = Color(0xFFE53935))
            expenses == null -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BudgetBlue)
            }
            expenses.isEmpty() -> Text(
                text = "아직 지출 내역이 없어요.\n아래 [지출 추가]로 첫 지출을 기록해 보세요.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = BudgetGray
            )
            else -> expenses.forEach { e -> ExpenseItem(e, members, myUid, onClick = { onClick(e) }) }
        }
    }
}

/** 해시태그(분류) 칩 색: 글자색 to 배경색 (소비 분석 그래프와 같은 색 계열) */
private fun categoryColors(category: String): Pair<Color, Color> =
    CategoryPalette.of(category).let { it.text to it.soft }

/** 지출 1건 카드 (피그마 "지출 내역") */
@Composable
private fun ExpenseItem(expense: Expense, members: List<String>, myUid: String?, onClick: () -> Unit = {}) {
    val splitCount = expense.splitAmong.ifEmpty { members }.size
    val (chipText, chipBg) = categoryColors(expense.category)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
            .border(1.dp, ExpenseBorder, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("expense_${expense.expenseId}"),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(
            modifier = Modifier.height(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = expense.title,
                    modifier = Modifier.weight(1f, fill = false),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BudgetText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(9.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(25.dp))
                        .background(chipBg)
                        .padding(horizontal = 11.dp, vertical = 2.dp)
                ) {
                    Text(expense.category, fontSize = 11.sp, color = chipText)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(won(expense.amount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BudgetText)
        }
        Row(
            modifier = Modifier.height(22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(expenseTime(expense.spentAt), modifier = Modifier.weight(1f), fontSize = 11.sp, color = BudgetGray)
            Text("결제자 ${memberName(expense.paidBy, members, myUid)}", fontSize = 11.sp, color = BudgetGray)
            Box(
                Modifier
                    .padding(horizontal = 5.dp)
                    .width(1.dp)
                    .height(15.dp)
                    .background(Color(0x669E9E9E))
            )
            Text("결제 인원 ${splitCount}명", fontSize = 11.sp, color = BudgetGray)
        }
    }
}

private fun expenseTime(millis: Long): String =
    SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA).format(Date(millis))

// ---------- 지출 추가 팝업 ----------

@Composable
private fun AddExpenseDialog(
    members: List<String>,
    myUid: String?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    initial: Expense? = null,
    onSave: (title: String, amount: Long, category: String, paidBy: String, splitAmong: List<String>) -> Unit
) {
    // initial이 있으면 "지출 수정": 지난번에 작성한 내용으로 채워서 열기
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var amountText by rememberSaveable { mutableStateOf(initial?.amount?.takeIf { it > 0 }?.toString().orEmpty()) }
    var category by rememberSaveable {
        mutableStateOf(initial?.category?.takeIf { it in ExpenseCategories.all } ?: ExpenseCategories.all.first())
    }
    var paidBy by rememberSaveable {
        mutableStateOf(
            initial?.paidBy?.takeIf { it in members }
                ?: myUid?.takeIf { it in members } ?: members.firstOrNull().orEmpty()
        )
    }
    var splitAmong by remember {
        mutableStateOf(initial?.splitAmong?.filter { it in members }?.ifEmpty { null } ?: members)
    }

    val amount = amountText.toLongOrNull() ?: 0L
    val canSave = title.isNotBlank() && amount > 0 && paidBy.isNotEmpty() && splitAmong.isNotEmpty() && !isSaving

    Dialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                DialogHeader(title = if (initial != null) "지출 수정" else "지출 추가", onClose = onDismiss, closeEnabled = !isSaving)
                Spacer(Modifier.height(20.dp))

                FieldLabel("지출 내용")
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(30) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_field"),
                    placeholder = { Text("예) 해운대 돼지국밥", color = Color(0xFF9E9E9E)) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = budgetFieldColors()
                )
                Spacer(Modifier.height(16.dp))

                FieldLabel("금액")
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> amountText = input.filter { it.isDigit() }.trimStart('0').take(9) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_field"),
                    placeholder = { Text("숫자만 입력", color = Color(0xFF9E9E9E)) },
                    suffix = { Text("원", color = BudgetText) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = budgetFieldColors()
                )
                if (amount > 0) {
                    Text(
                        text = won(amount),
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                        fontSize = 13.sp,
                        color = BudgetGray
                    )
                }
                Spacer(Modifier.height(16.dp))

                FieldLabel("분류")
                ChipRows(
                    items = ExpenseCategories.all,
                    label = { "#$it" },
                    isSelected = { it == category },
                    onClick = { category = it },
                    tagPrefix = "expense_category",
                    colorOf = { CategoryPalette.of(it) }
                )
                Spacer(Modifier.height(16.dp))

                FieldLabel("결제한 사람")
                ChipRows(
                    items = members,
                    label = { memberName(it, members, myUid) },
                    isSelected = { it == paidBy },
                    onClick = { paidBy = it },
                    tagPrefix = "expense_payer"
                )
                Spacer(Modifier.height(16.dp))

                FieldLabel("함께 나눌 사람")
                ChipRows(
                    items = members,
                    label = { memberName(it, members, myUid) },
                    isSelected = { it in splitAmong },
                    onClick = { uid ->
                        splitAmong = if (uid in splitAmong) splitAmong - uid else members.filter { it in splitAmong || it == uid }
                    },
                    tagPrefix = "expense_split"
                )
                if (splitAmong.isNotEmpty() && amount > 0) {
                    Text(
                        text = "${splitAmong.size}명이 1/N → 1인당 약 ${won(amount / splitAmong.size)}",
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = BudgetBlue
                    )
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(errorMessage, fontSize = 13.sp, color = Color(0xFFE53935))
                }

                Spacer(Modifier.height(24.dp))
                PrimaryDialogButton(
                    text = if (initial != null) "수정하기" else "추가하기",
                    enabled = canSave,
                    isLoading = isSaving,
                    testTag = "expense_save_button",
                    onClick = { onSave(title, amount, category, paidBy, splitAmong) }
                )
            }
        }
    }
}

@Composable
private fun budgetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BudgetBlue,
    unfocusedBorderColor = Color(0xFFE0E3E8),
    cursorColor = BudgetBlue
)

/** 4개씩 줄 바꿈되는 선택 칩 */
@Composable
private fun <T> ChipRows(
    items: List<T>,
    label: (T) -> String,
    isSelected: (T) -> Boolean,
    onClick: (T) -> Unit,
    tagPrefix: String,
    colorOf: ((T) -> CategoryColor)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { i, item ->
                    val selected = isSelected(item)
                    // 분류 칩은 고유 색: 선택 = 진한 색 배경, 미선택 = 연한 색 배경
                    val palette = colorOf?.invoke(item)
                    val bg = when {
                        palette != null -> if (selected) palette.main else palette.soft
                        else -> if (selected) BudgetBlue else Color.White
                    }
                    val line = when {
                        palette != null -> if (selected) palette.main else Color.Transparent
                        else -> if (selected) BudgetBlue else Color(0xFFE0E3E8)
                    }
                    val fg = when {
                        selected -> Color.White
                        palette != null -> palette.text
                        else -> BudgetText
                    }
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(bg)
                            .border(1.dp, line, RoundedCornerShape(19.dp))
                            .clickable { onClick(item) }
                            .padding(horizontal = 14.dp)
                            .testTag("${tagPrefix}_${items.indexOf(item)}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label(item),
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = fg
                        )
                    }
                }
            }
        }
    }
}
