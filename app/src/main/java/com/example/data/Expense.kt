package com.example.data

/**
 * 가계부 지출 1건. Firestore: trips/{tripId}/expenses/{expenseId}
 */
data class Expense(
    val expenseId: String = "",
    val title: String = "",                     // 지출 내용 (예: 해운대 숙소)
    val amount: Long = 0,                       // 금액 (원)
    val category: String = "",                 // 분류 (예: 숙박) - 화면에는 #숙박
    val paidBy: String = "",                    // 결제한 사람 UID
    val splitAmong: List<String> = emptyList(), // 함께 나눌 사람 UID 목록 (1/N)
    val spentAt: Long = 0,                      // 결제 시각 (epoch millis)
    val createdBy: String = "",
    val createdAt: Long = 0
)

/** 지출 분류 (해시태그) */
object ExpenseCategories {
    val all = listOf("식비", "카페", "숙박", "교통", "관광", "쇼핑", "기타")
}

/**
 * 가계부 합계와 1/N 계산 결과
 * @param total 총 공동 지출액
 * @param paid 멤버별 실제로 낸 돈
 * @param share 멤버별 1/N로 부담해야 하는 돈
 */
data class BudgetSummary(
    val total: Long,
    val paid: Map<String, Long>,
    val share: Map<String, Long>
) {
    /** 받을 돈(+) / 보낼 돈(-) */
    fun balanceOf(uid: String): Long = (paid[uid] ?: 0) - (share[uid] ?: 0)
}

object BudgetCalculator {
    /**
     * 각 지출을 splitAmong 인원으로 1/N 나눕니다.
     * 나누어 떨어지지 않는 원 단위 나머지는 앞사람부터 1원씩 더 부담합니다.
     */
    fun summarize(members: List<String>, expenses: List<Expense>): BudgetSummary {
        val paid = members.associateWith { 0L }.toMutableMap()
        val share = members.associateWith { 0L }.toMutableMap()
        for (e in expenses) {
            paid[e.paidBy] = (paid[e.paidBy] ?: 0) + e.amount
            val people = e.splitAmong.ifEmpty { members }
            if (people.isEmpty()) continue
            val each = e.amount / people.size
            val remainder = e.amount % people.size
            people.forEachIndexed { i, uid ->
                share[uid] = (share[uid] ?: 0) + each + if (i < remainder) 1 else 0
            }
        }
        return BudgetSummary(total = expenses.sumOf { it.amount }, paid = paid, share = share)
    }

    /** 분류별 합계 (큰 금액 순) */
    fun byCategory(expenses: List<Expense>): List<Pair<String, Long>> =
        expenses.groupBy { it.category }
            .map { (category, list) -> category to list.sumOf { it.amount } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }

    /**
     * 누가 누구에게 얼마를 보내면 정산이 끝나는지 계산합니다.
     * 보낼 돈이 큰 사람과 받을 돈이 큰 사람부터 짝지어 송금 횟수를 줄입니다.
     */
    fun transfers(members: List<String>, summary: BudgetSummary): List<Transfer> {
        val receivers = members.map { it to summary.balanceOf(it) }.filter { it.second > 0 }
            .sortedByDescending { it.second }.map { it.first to it.second }.toMutableList()
        val senders = members.map { it to -summary.balanceOf(it) }.filter { it.second > 0 }
            .sortedByDescending { it.second }.map { it.first to it.second }.toMutableList()
        val result = mutableListOf<Transfer>()
        var r = 0
        var s = 0
        while (r < receivers.size && s < senders.size) {
            val (toUid, need) = receivers[r]
            val (fromUid, owe) = senders[s]
            val amount = minOf(need, owe)
            if (amount > 0) result += Transfer(from = fromUid, to = toUid, amount = amount)
            receivers[r] = toUid to (need - amount)
            senders[s] = fromUid to (owe - amount)
            if (receivers[r].second == 0L) r++
            if (senders[s].second == 0L) s++
        }
        return result
    }
}

/** 정산 송금 1건: from 이 to 에게 amount 원 보내기 */
data class Transfer(val from: String, val to: String, val amount: Long)
