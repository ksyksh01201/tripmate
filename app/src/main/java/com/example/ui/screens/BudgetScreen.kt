package com.example.ui.screens

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
private val BudgetMemberColors = listOf(
    Color(0xFF262452), Color(0xFFFF7A4D), Color(0xFF4A90E2),
    Color(0xFF34C38F), Color(0xFF9B6BDF), Color(0xFFF5A623)
)

private enum class BudgetTab(val label: String) { List("지출 목록"), Analysis("소비 분석"), Settle("정산하기") }

/** 멤버 표시 이름. 아직 닉네임이 없어서 나 / 멤버2 / 멤버3 … 으로 표시 */
internal fun memberName(uid: String, members: List<String>, myUid: String?): String =
    if (uid == myUid) "나" else "멤버${members.indexOf(uid) + 1}"

private fun memberInitial(uid: String, members: List<String>, myUid: String?): String =
    if (uid == myUid) "나" else "${members.indexOf(uid) + 1}"

private fun memberColor(uid: String, members: List<String>): Color =
    BudgetMemberColors[members.indexOf(uid).coerceAtLeast(0) % BudgetMemberColors.size]

internal fun won(amount: Long): String = NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"

/**
 * 여행룸 하단 [가계부] 탭 내용.
 * 총 공동 지출액 → 멤버별 결제 금액 → [지출 목록 | 소비 분석 | 정산하기] → 지출 목록 → [+ 지출 추가]
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

    // 저장이 끝나면 팝업 닫기
    LaunchedEffect(addState.savedCount) {
        if (addState.savedCount > 0) showAdd = false
    }

    val members = trip.members
    val list = expenses.orEmpty()
    val summary = remember(members, list) { BudgetCalculator.summarize(members, list) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 공동 지출 요약
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(start = 24.dp, end = 24.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("총 공동 지출액", fontSize = 13.sp, color = BudgetGray)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = won(summary.total),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = BudgetText,
                    modifier = Modifier.testTag("budget_total")
                )
                Spacer(Modifier.width(10.dp))
                // 1/N 결과
                Text(
                    text = splitSummaryText(summary.total, members, list, summary.share[myUid]),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = BudgetBlue,
                    modifier = Modifier
                        .padding(bottom = 6.dp)
                        .testTag("budget_per_person")
                )
            }
            MemberSpendCard(members = members, myUid = myUid, paid = summary.paid, share = summary.share)
        }

        // 가계부 카테고리 탭
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BudgetTab.entries.forEach { t ->
                val selected = t == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) BudgetBlue else Color.White)
                        .border(1.dp, if (selected) BudgetBlue else BudgetLine, RoundedCornerShape(10.dp))
                        .clickable { tab = t }
                        .testTag("budget_tab_${t.name}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = t.label,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else BudgetGray
                    )
                }
            }
        }

        when (tab) {
            BudgetTab.List -> ExpenseListSection(
                expenses = expenses,
                loadError = loadError,
                members = members,
                myUid = myUid,
                onAddClick = {
                    viewModel.clearError()
                    showAdd = true
                }
            )
            BudgetTab.Analysis -> SpendingAnalysisSection(expenses = list)
            BudgetTab.Settle -> SettlementSection(
                members = members,
                myUid = myUid,
                expenses = list,
                summary = summary
            )
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
}

/** "2명 · 1인당 90,000원" (모든 지출을 전원이 나눌 때) / 아니면 "내 부담 OO원" */
private fun splitSummaryText(total: Long, members: List<String>, expenses: List<Expense>, myShare: Long?): String {
    if (members.isEmpty()) return ""
    val everyoneSplits = expenses.all { it.splitAmong.isEmpty() || it.splitAmong.toSet() == members.toSet() }
    return if (everyoneSplits) {
        "${members.size}명 · 1인당 ${won(total / members.size)}"
    } else {
        "내 부담 ${won(myShare ?: 0)}"
    }
}

/** 멤버별 결제 금액 카드 (피그마 "멤버별 지출 금액") */
@Composable
private fun MemberSpendCard(
    members: List<String>,
    myUid: String?,
    paid: Map<String, Long>,
    share: Map<String, Long>
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, BudgetLine, RoundedCornerShape(10.dp))
            .horizontalScroll(scroll)
            .padding(horizontal = 12.dp)
            .testTag("budget_member_card"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        members.forEachIndexed { index, uid ->
            if (index > 0) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .width(1.dp)
                        .height(26.dp)
                        .background(BudgetLine)
                )
            }
            Row(
                modifier = Modifier.widthIn(min = if (members.size <= 2) 130.dp else 110.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(memberColor(uid, members)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(memberInitial(uid, members, myUid), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                }
                Column {
                    Text(
                        text = "${memberName(uid, members, myUid)} · 부담 ${won(share[uid] ?: 0)}",
                        fontSize = 10.sp,
                        color = BudgetGray
                    )
                    Text(
                        text = won(paid[uid] ?: 0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BudgetText
                    )
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
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
            else -> expenses.forEach { e -> ExpenseItem(e, members, myUid) }
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BudgetBlue)
                .clickable(onClick = onAddClick)
                .testTag("budget_add_expense_button"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("지출 추가", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

/** 지출 1건 카드 (피그마 "지출 내역") */
@Composable
private fun ExpenseItem(expense: Expense, members: List<String>, myUid: String?) {
    val splitCount = expense.splitAmong.ifEmpty { members }.size
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
            .border(1.dp, ExpenseBorder, RoundedCornerShape(15.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("expense_${expense.expenseId}"),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = expense.title,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BudgetText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(won(expense.amount), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BudgetText)
        }
        Row {
            Text("#${expense.category}", modifier = Modifier.weight(1f), fontSize = 11.sp, color = BudgetBlue)
            Text("${memberName(expense.paidBy, members, myUid)} 결제", fontSize = 11.sp, color = BudgetGray)
        }
        Row {
            Text(expenseTime(expense.spentAt), modifier = Modifier.weight(1f), fontSize = 11.sp, color = BudgetGray)
            Text("${splitCount}명 분담", fontSize = 11.sp, color = BudgetGray)
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
    onSave: (title: String, amount: Long, category: String, paidBy: String, splitAmong: List<String>) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var amountText by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(ExpenseCategories.all.first()) }
    var paidBy by rememberSaveable { mutableStateOf(myUid?.takeIf { it in members } ?: members.firstOrNull().orEmpty()) }
    var splitAmong by remember { mutableStateOf(members) }

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
                DialogHeader(title = "지출 추가", onClose = onDismiss, closeEnabled = !isSaving)
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
                    tagPrefix = "expense_category"
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
                    text = "추가하기",
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
    tagPrefix: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { i, item ->
                    val selected = isSelected(item)
                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(if (selected) BudgetBlue else Color.White)
                            .border(1.dp, if (selected) BudgetBlue else Color(0xFFE0E3E8), RoundedCornerShape(19.dp))
                            .clickable { onClick(item) }
                            .padding(horizontal = 14.dp)
                            .testTag("${tagPrefix}_${items.indexOf(item)}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label(item),
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else BudgetText
                        )
                    }
                }
            }
        }
    }
}
