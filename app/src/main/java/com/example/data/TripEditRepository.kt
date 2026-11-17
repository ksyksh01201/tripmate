package com.example.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 여행방 편집 화면에서 쓰는 기능 (여행 이름·날짜 수정 / 여행방 나가기 / 여행 삭제).
 * 기존 TripRepository 는 그대로 두고 편집 기능만 따로 모았습니다.
 */
class TripEditRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val trips = db.collection("trips")

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw TripException("로그인 정보가 없어요. 앱을 다시 실행해 주세요.")

    /** 여행 이름과 날짜(yyyy-MM-dd) 저장 */
    suspend fun updateTrip(tripId: String, title: String, startDate: String, endDate: String) {
        requireUid()
        trips.document(tripId).update(
            mapOf(
                "title" to title.trim(),
                "startDate" to startDate,
                "endDate" to endDate
            )
        ).awaitTask()
    }

    /**
     * 여행방 나가기: members 에서 나를 뺍니다.
     * 내가 마지막 한 명이면 방이 비어 버리므로 여행을 통째로 삭제합니다.
     */
    suspend fun leaveTrip(trip: Trip) {
        val uid = requireUid()
        val others = trip.members.filter { it != uid }
        if (others.isEmpty() && trip.createdBy == uid) {
            deleteTrip(trip)
            return
        }
        val updates = mutableMapOf<String, Any>("members" to FieldValue.arrayRemove(uid))
        // 방장이 나가면 남은 사람 중 가장 먼저 들어온 사람이 새 방장이 됩니다.
        if (trip.createdBy == uid) updates["createdBy"] = others.first()
        trips.document(trip.tripId).update(updates).awaitTask()
    }

    /**
     * 여행 삭제 (방장만 가능).
     * Firestore 는 하위 컬렉션을 자동으로 지우지 않아서 지출·일정을 먼저 지운 뒤 여행 문서를 지웁니다.
     */
    suspend fun deleteTrip(trip: Trip) {
        val uid = requireUid()
        if (trip.createdBy != uid) throw TripException("여행 삭제는 방장만 할 수 있어요.")
        val tripRef = trips.document(trip.tripId)
        for (sub in listOf("expenses", "schedules")) {
            val docs = tripRef.collection(sub).get().awaitTask().documents
            // 한 번에 최대 500개까지 지울 수 있어서 나눠서 지웁니다.
            docs.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it.reference) }
                batch.commit().awaitTask()
            }
        }
        tripRef.delete().awaitTask()
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { e -> cont.resumeWithException(e) }
    addOnCanceledListener { cont.cancel() }
}
