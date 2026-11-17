package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BudgetCalculator
import com.example.data.BudgetSummary
import com.example.data.Expense
import java.util.Locale

// 피그마 "가계부 · 소비 분석 / 정산하기" 색상
private val AText = Color(0xFF202020)
private val AGray = Color(0xFF727272)
private val ABorder = Color(0xFFEEEEEE)
private val ABlue = Color(0xFF4A90E2)
private val ASendRed = Color(0xFFCF592D)
private val ChartColors = listOf(
    Color(0xFF5B8FDE), Color(0xFFF0825A), Color(0xFF5FB3A8), Color(0xFF9187CF),
    Color(0xFFF5B942), Color(0xFF6CC3E0), Color(0xFFB0B0B0)
)
private val SettleMemberColors = listOf(
    Color(0xFF262452), Color(0xFFFF7A4D), Color(0xFF4A90E2),
    Color(0xFF34C38F), Color(0xFF9B6BDF), Color(0xFFF5A623)
)

// ---------- 소비 분석 ----------

/** 해시태그별 소비: 원형 그래프 + 총 지출 + 분류별 금액/비율 */
@Composable
internal fun SpendingAnalysisSection(expenses: List<Expense>) {
    val byCategory = BudgetCalculator.byCategory(expenses)
    val total = byCategory.sumOf { it.second }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 16.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
            .border(1.dp, ABorder, RoundedCornerShape(15.dp))
            .padding(14.dp)
            .testTag("budget_analysis_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("해시태그별 소비", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AText)
            Text("총 ${expenses.size}건", fontSize = 12.sp, color = AGray)
        }

        if (total == 0L) {
            Text(
                text = "아직 지출 내역이 없어요.\n지출 목록에서 지출을 추가하면 분석이 나와요.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = AGray
            )
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PieChart(
                values = byCategory.map { it.second },
                modifier = Modifier.size(160.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("총 지출", fontSize = 12.sp, color = AGray)
                Text(won(total), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = AText)
                Text("${byCategory.size}개 카테고리", fontSize = 12.sp, color = AGray)
            }
        }

        Column {
            byCategory.forEachIndexed { index, (category, amount) ->
                HorizontalDivider(color = ABorder)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(33.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ChartColors[index % ChartColors.size])
                    )
                    Text("#$category", modifier = Modifier.weight(1f), fontSize = 13.sp, color = AText)
                    Text(won(amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AText)
                    Text(
                        text = String.format(Locale.KOREA, "%.1f%%", amount * 100.0 / total),
                        modifier = Modifier.width(49.dp),
                        textAlign = TextAlign.End,
                        fontSize = 12.sp,
                        color = AGray
                    )
                }
            }
        }
    }
}

/** 원형 그래프 (조각 사이 흰 선) */
@Composable
private fun PieChart(values: List<Long>, modifier: Modifier = Modifier) {
    val total = values.sum().toFloat()
    Canvas(modifier = modifier) {
        if (total <= 0f) return@Canvas
        val gap = 2.5.dp.toPx()
        val diameter = size.minDimension - gap
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        var start = -90f
        values.forEachIndexed { i, v ->
            val sweep = v / total * 360f
            drawArc(
                color = ChartColors[i % ChartColors.size],
                startAngle = start,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = Size(diameter, diameter)
            )
            start += sweep
        }
        // 조각 경계선
        if (values.size > 1) {
            start = -90f
            values.forEach { v ->
                val sweep = v / total * 360f
                drawArc(
                    color = Color.White,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = gap)
                )
                start += sweep
            }
        }
    }
}

// ---------- 정산하기 ----------

/** 정산 현황: 멤버별 받을/보낼 금액 + 남은 송금 */
@Composable
internal fun SettlementSection(
    members: List<String>,
    myUid: String?,
    expenses: List<Expense>,
    summary: BudgetSummary
) {
    val transfers = BudgetCalculator.transfers(members, summary)
    val equalSplit = expenses.all { it.splitAmong.isEmpty() || it.splitAmong.toSet() == members.toSet() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("정산 현황", modifier = Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AText)
            Text(
                text = if (equalSplit) "${members.size}명 균등 분담" else "${members.size}명 분담",
                fontSize = 12.sp,
                color = AGray
            )
        }

        members.forEachIndexed { index, uid ->
            MemberSettleCard(
                name = memberName(uid, members, myUid),
                initial = if (uid == myUid) "나" else "${index + 1}",
                color = SettleMemberColors[index % SettleMemberColors.size],
                paid = summary.paid[uid] ?: 0,
                share = summary.share[uid] ?: 0,
                balance = summary.balanceOf(uid),
                tag = "settle_member_$index"
            )
        }

        // 송금 현황
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFFEDF5FE))
                .border(1.dp, Color(0xFFDCE6F2), RoundedCornerShape(15.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag("settle_transfers"),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (transfers.isEmpty()) "남은 정산 없음" else "남은 정산 ${transfers.size}건",
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color = AGray
                )
                val done = transfers.isEmpty()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (done) Color(0xFFE3EEFC) else Color(0xFFFFF0E9))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (done) "정산 완료" else "미정산",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (done) ABlue else ASendRed
                    )
                }
            }
            if (transfers.isEmpty()) {
                Text("모두 같은 금액을 냈어요!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AText)
            }
            transfers.forEach { t ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${memberName(t.from, members, myUid)} → ${memberName(t.to, members, myUid)}",
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AText
                    )
                    Text(won(t.amount), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ABlue)
                }
            }
        }
    }
}

@Composable
private fun MemberSettleCard(
    name: String,
    initial: String,
    color: Color,
    paid: Long,
    share: Long,
    balance: Long,
    tag: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
            .border(1.dp, ABorder, RoundedCornerShape(15.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(initial, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
            Spacer(Modifier.width(8.dp))
            Text(name, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AText)
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = when {
                        balance > 0 -> "받을 금액"
                        balance < 0 -> "보낼 금액"
                        else -> "정산 없음"
                    },
                    fontSize = 11.sp,
                    color = AGray
                )
                Text(
                    text = won(kotlin.math.abs(balance)),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        balance > 0 -> ABlue
                        balance < 0 -> ASendRed
                        else -> AGray
                    }
                )
            }
        }
        HorizontalDivider(color = ABorder)
        Row {
            Text("결제액  ${won(paid)}", modifier = Modifier.weight(1f), fontSize = 12.sp, color = AGray)
            Text("분담액  ${won(share)}", fontSize = 12.sp, color = AGray)
        }
    }
}
