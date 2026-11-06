package com.example.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 가계부 지출 저장소. 지출은 여행 문서 아래 expenses 컬렉션에 저장됩니다.
 * trips/{tripId}/expenses/{expenseId}
 */
class ExpenseRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private fun expenses(tripId: String) =
        db.collection("trips").document(tripId).collection("expenses")

    /** 지출 목록 실시간 관찰 (최근 결제가 위로) */
    fun getExpenses(tripId: String): Flow<List<Expense>> = callbackFlow {
        val registration = expenses(tripId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents.orEmpty()
                .mapNotNull { doc -> doc.toObject(Expense::class.java)?.copy(expenseId = doc.id) }
                .sortedByDescending { it.spentAt }
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    /** 지출 추가 */
    suspend fun addExpense(
        tripId: String,
        title: String,
        amount: Long,
        category: String,
        paidBy: String,
        splitAmong: List<String>,
        spentAt: Long
    ) {
        val uid = auth.currentUser?.uid ?: throw TripException("로그인 정보가 없어요. 앱을 다시 실행해 주세요.")
        val doc = expenses(tripId).document()
        val expense = Expense(
            expenseId = doc.id,
            title = title.trim(),
            amount = amount,
            category = category,
            paidBy = paidBy,
            splitAmong = splitAmong,
            spentAt = spentAt,
            createdBy = uid,
            createdAt = System.currentTimeMillis()
        )
        doc.set(expense).awaitTask()
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { e -> cont.resumeWithException(e) }
    addOnCanceledListener { cont.cancel() }
}
