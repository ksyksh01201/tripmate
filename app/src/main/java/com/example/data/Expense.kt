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
}
