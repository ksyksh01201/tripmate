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
 * 여행 일정 저장소. 일정은 여행 문서 아래 schedules 컬렉션에 저장됩니다.
 * trips/{tripId}/schedules/{scheduleId}
 */
class ScheduleRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private fun schedules(tripId: String) =
        db.collection("trips").document(tripId).collection("schedules")

    /** 모든 날의 일정을 실시간으로 관찰 (일차 → 순서 순) */
    fun getSchedules(tripId: String): Flow<List<Schedule>> = callbackFlow {
        val registration = schedules(tripId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val list = snapshot?.documents.orEmpty()
                .mapNotNull { doc -> doc.toObject(Schedule::class.java)?.copy(scheduleId = doc.id) }
                .sortedWith(compareBy<Schedule>({ it.day }, { it.order }, { it.createdAt }))
            trySend(list)
        }
        awaitClose { registration.remove() }
    }

    /** 일정 추가. 그날의 맨 아래에 붙습니다. */
    suspend fun addSchedule(tripId: String, schedule: Schedule) {
        val uid = auth.currentUser?.uid ?: throw TripException("로그인 정보가 없어요. 앱을 다시 실행해 주세요.")
        val doc = schedules(tripId).document()
        val now = System.currentTimeMillis()
        doc.set(
            schedule.copy(
                scheduleId = doc.id,
                title = schedule.title.trim(),
                memo = schedule.memo.trim(),
                order = if (schedule.order == 0L) now else schedule.order,
                createdBy = uid,
                createdAt = now
            )
        ).awaitTask()
    }

    suspend fun deleteSchedule(tripId: String, scheduleId: String) {
        schedules(tripId).document(scheduleId).delete().awaitTask()
    }

    /**
     * 끌어서 바꾼 순서를 저장합니다. 화면에 보이는 순서대로 order 값을 다시 매깁니다.
     * (기존 order 값들을 작은 순으로 다시 나눠 주므로 다른 날 일정에는 영향이 없습니다)
     */
    suspend fun saveOrder(tripId: String, ordered: List<Schedule>) {
        if (ordered.isEmpty()) return
        val values = ordered.map { it.order }.sorted().toMutableList()
        // 같은 값이 있으면 순서가 섞일 수 있어서 1씩 키워 모두 다르게 만듭니다.
        for (i in 1 until values.size) if (values[i] <= values[i - 1]) values[i] = values[i - 1] + 1
        val batch = db.batch()
        ordered.forEachIndexed { i, s ->
            if (s.order != values[i]) batch.update(schedules(tripId).document(s.scheduleId), "order", values[i])
        }
        batch.commit().awaitTask()
    }

    /** 두 일정의 순서를 맞바꿉니다 (위로/아래로 이동) */
    suspend fun swapOrder(tripId: String, a: Schedule, b: Schedule) {
        val batch = db.batch()
        batch.update(schedules(tripId).document(a.scheduleId), "order", b.order)
        batch.update(schedules(tripId).document(b.scheduleId), "order", a.order)
        batch.commit().awaitTask()
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { e -> cont.resumeWithException(e) }
    addOnCanceledListener { cont.cancel() }
}
