package com.example.data

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 여행 문서의 memberNames 맵(uid → 이름)을 다룹니다.
 * 각자 여행방에 들어오면 자기 이름을 저장해서, 다른 사람 화면에도 이름이 보이게 합니다.
 */
object TripMemberNames {
    private val trips get() = FirebaseFirestore.getInstance().collection("trips")

    /** 여행의 memberNames를 실시간으로 받습니다. */
    fun observe(tripId: String): Flow<Map<String, String>> = callbackFlow {
        val reg = trips.document(tripId).addSnapshotListener { snap, _ ->
            @Suppress("UNCHECKED_CAST")
            val raw = snap?.get("memberNames") as? Map<String, Any?> ?: emptyMap()
            trySend(raw.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap())
        }
        awaitClose { reg.remove() }
    }

    /** 내 이름을 여행 문서에 저장합니다. (멤버만 수정 가능하므로 참여 후에 호출) */
    suspend fun saveMyName(tripId: String, uid: String, name: String) {
        if (name.isBlank()) return
        trips.document(tripId).update("memberNames.$uid", name).awaitTask()
    }

    /** 예전에 넣었던 가짜 테스트 멤버(uid가 "test_"로 시작)를 여행에서 지웁니다. */
    suspend fun removeTestMembers(tripId: String, members: List<String>) {
        val testIds = members.filter { it.startsWith("test_") }
        if (testIds.isEmpty()) return
        val updates = mutableMapOf<String, Any>("members" to FieldValue.arrayRemove(*testIds.toTypedArray()))
        testIds.forEach { updates["memberNames.$it"] = FieldValue.delete() }
        trips.document(tripId).update(updates).awaitTask()
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
