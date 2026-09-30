package com.example.service

import com.example.model.*

object SettlementCalculator {

    /**
     * [1. 정산 계산의 핵심 원칙]
     * 단순히 총 여행 경비 ÷ 전체 인원으로 계산하지 않고,
     * 각 Expense마다 payerId(실제 결제자)와 participantIds(비용 부담 참여자)를 확인하여
     * 각 사용자별 paidAmount(실제 결제 금액), owedAmount(부담해야 하는 금액), netBalance(잔액)를 정확하게 계산합니다.
     */
    fun calculateMemberBalances(
        members: List<TripMember>,
        expenses: List<Expense>
    ): List<MemberBalance> {
        val memberMap = LinkedHashMap<String, TripMember>()
        members.forEach { memberMap[it.id] = it }

        // 지출(Expense)에 등장하는 결제자 및 부담 참여자가 members에 누락된 경우에도 포함하여 계산
        expenses.forEach { exp ->
            if (exp.payerId.isNotBlank() && !memberMap.containsKey(exp.payerId)) {
                val isMe = exp.payerId == "user_me" || exp.payerName.startsWith("나")
                memberMap[exp.payerId] = TripMember(
                    id = exp.payerId,
                    name = exp.payerName.ifBlank { if (isMe) "나" else "참여자" },
                    isCurrentUser = isMe
                )
            }
            exp.participantIds.forEachIndexed { idx, pId ->
                if (pId.isNotBlank() && !memberMap.containsKey(pId)) {
                    val pName = exp.participantNames.getOrNull(idx) ?: pId
                    val isMe = pId == "user_me" || pName.startsWith("나")
                    memberMap[pId] = TripMember(
                        id = pId,
                        name = pName.ifBlank { if (isMe) "나" else "참여자" },
                        isCurrentUser = isMe
                    )
                }
            }
        }

        // 기본 사용자("user_me") 보장
        if (memberMap.isEmpty()) {
            memberMap["user_me"] = TripMember(
                id = "user_me",
                name = "나",
                isCurrentUser = true
            )
        }

        val paidMap = mutableMapOf<String, Long>()
        val owedMap = mutableMapOf<String, Long>()

        memberMap.keys.forEach { id ->
            paidMap[id] = 0L
            owedMap[id] = 0L
        }

        expenses.forEach { exp ->
            // [2. 실제 결제 금액 계산]
            // 각 사용자별로 내가 실제로 결제한 금액 합산 (Expense의 payerId 기준)
            val payerKey = if (memberMap.containsKey(exp.payerId)) {
                exp.payerId
            } else {
                memberMap.values.find { it.name == exp.payerName }?.id ?: exp.payerId
            }
            paidMap[payerKey] = (paidMap[payerKey] ?: 0L) + exp.amount

            // [3. 부담해야 하는 금액 계산] & [11. 일부 사람만 부담하는 지출]
            // 각 Expense의 participantIds에 포함된 사람끼리만 해당 금액 분배 (participantIds에 없는 사람 제외)
            val validParticipants = exp.participantIds.filter { it.isNotBlank() }
            val n = validParticipants.size
            if (n > 0) {
                val base = exp.amount / n
                val remainder = (exp.amount % n).toInt()

                validParticipants.forEachIndexed { index, pId ->
                    val resolvedId = if (memberMap.containsKey(pId)) {
                        pId
                    } else {
                        val nameAtIndex = exp.participantNames.getOrNull(index)
                        memberMap.values.find { it.name == nameAtIndex }?.id ?: pId
                    }
                    val extra = if (index < remainder) 1L else 0L
                    val myShare = base + extra
                    owedMap[resolvedId] = (owedMap[resolvedId] ?: 0L) + myShare
                }
            }
        }

        // [4. 개인별 잔액 계산: balance = paidAmount - owedAmount]
        return memberMap.values.map { member ->
            val paid = paidMap[member.id] ?: 0L
            val owed = owedMap[member.id] ?: 0L
            val isCurrent = member.isCurrentUser || member.id == "user_me" || member.name.startsWith("나")
            MemberBalance(
                userId = member.id,
                userName = member.name,
                isCurrentUser = isCurrent,
                paidAmount = paid,
                owedAmount = owed,
                netBalance = paid - owed
            )
        }.sortedWith(compareByDescending<MemberBalance> { it.isCurrentUser }.thenBy { it.userName })
    }

    /**
     * [12. 여러 명 정산 & 4. 잔액 기준 송금 계산]
     * balance > 0 (creditor: 돈을 받아야 하는 사람)
     * balance < 0 (debtor: 돈을 보내야 하는 사람)
     * debtor가 creditor에게 필요한 금액을 송금하도록 계산.
     */
    fun calculateSettlementTransfers(
        balances: List<MemberBalance>
    ): List<SettlementTransfer> {
        // Creditors (받아야 할 사람: netBalance > 0)
        val creditors = balances
            .filter { it.netBalance > 0 }
            .map { it.copy() }
            .toMutableList()

        // Debtors (보내야 할 사람: netBalance < 0)
        val debtors = balances
            .filter { it.netBalance < 0 }
            .map { it.copy(netBalance = -it.netBalance) } // 양수로 변환하여 잔여 송금액 추적
            .toMutableList()

        val transfers = mutableListOf<SettlementTransfer>()
        var transferIndex = 1

        var cIdx = 0
        var dIdx = 0

        while (cIdx < creditors.size && dIdx < debtors.size) {
            val creditor = creditors[cIdx]
            val debtor = debtors[dIdx]

            val settleAmount = minOf(creditor.netBalance, debtor.netBalance)
            if (settleAmount > 0) {
                transfers.add(
                    SettlementTransfer(
                        id = "transfer_$transferIndex",
                        fromUserId = debtor.userId,
                        fromUserName = debtor.userName,
                        toUserId = creditor.userId,
                        toUserName = creditor.userName,
                        amount = settleAmount,
                        isCompleted = false
                    )
                )
                transferIndex++

                val newCBalance = creditor.netBalance - settleAmount
                val newDBalance = debtor.netBalance - settleAmount

                creditors[cIdx] = creditor.copy(netBalance = newCBalance)
                debtors[dIdx] = debtor.copy(netBalance = newDBalance)

                if (newCBalance == 0L) cIdx++
                if (newDBalance == 0L) dIdx++
            } else {
                if (creditor.netBalance == 0L) cIdx++
                if (debtor.netBalance == 0L) dIdx++
            }
        }

        return transfers
    }

    val CATEGORIES = listOf("식비", "숙박", "교통", "관광", "쇼핑", "기타")

    /**
     * [14. 카테고리 그래프 유지]
     * 카테고리별 지출 합계 계산 (식비, 숙박, 교통, 관광, 쇼핑, 기타 6개 카테고리)
     */
    fun calculateCategoryTotals(expenses: List<Expense>): List<CategoryTotal> {
        val groupedSums = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }

        return CATEGORIES.map { cat ->
            CategoryTotal(
                category = cat,
                totalAmount = groupedSums[cat] ?: 0L
            )
        }
    }

    fun getCategoryTotalsMap(expenses: List<Expense>): Map<String, Long> {
        return calculateCategoryTotals(expenses).associate { it.category to it.totalAmount }
    }

    fun calculateCategoryExpenses(expenses: List<Expense>): List<CategoryExpense> {
        val total = expenses.sumOf { it.amount }
        if (total == 0L) return emptyList()

        val grouped = expenses.groupBy { it.category }
        return grouped.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val pct = ((sum.toDouble() / total) * 100).toInt()
            CategoryExpense(cat, sum, pct)
        }.sortedByDescending { it.totalAmount }
    }

    fun calculateDailyExpenses(expenses: List<Expense>): List<DailyExpense> {
        val grouped = expenses.groupBy { it.date }.toSortedMap()
        var dayCount = 1
        return grouped.map { (date, list) ->
            val sum = list.sumOf { it.amount }
            DailyExpense(
                dateStr = date,
                dayLabel = "DAY $dayCount",
                totalAmount = sum
            ).also { dayCount++ }
        }
    }
}
