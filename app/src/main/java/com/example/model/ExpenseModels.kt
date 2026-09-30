package com.example.model

val EXPENSE_CATEGORIES = listOf("식비", "숙박", "교통", "관광", "쇼핑", "기타")

data class Expense(
    val expenseId: String,
    val tripId: String,
    val title: String,
    val amount: Long,
    val category: String, // 식비, 숙박, 교통, 관광, 쇼핑, 기타
    val payerId: String,
    val payerName: String,
    val participantIds: List<String>,
    val participantNames: List<String>,
    val date: String = "",
    val memo: String = ""
) {
    val expenseName: String get() = title
    val payer: String get() = payerName
    val participants: List<String> get() = participantNames
}

data class CategoryTotal(
    val category: String,
    val totalAmount: Long
)

data class SettlementTransfer(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: Long,
    var isCompleted: Boolean = false
)

data class MemberBalance(
    val userId: String,
    val userName: String,
    val isCurrentUser: Boolean,
    val paidAmount: Long,
    val owedAmount: Long,
    val netBalance: Long // paidAmount - owedAmount
)

data class CategoryExpense(
    val category: String,
    val totalAmount: Long,
    val percentage: Int
)

data class DailyExpense(
    val dateStr: String,
    val dayLabel: String,
    val totalAmount: Long
)

object MockExpenseDataProvider {
    fun getInitialExpenses(tripId: String = ""): List<Expense> = emptyList()
}
