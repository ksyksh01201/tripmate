package com.example.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 밸런스 게임을 각자 자기 폰(계정)에서 하도록 Firestore로 맞춰 주는 곳.
 * - 질문: 여행마다 한 세트를 같이 씀 → trips/{id}/balance_meta/questions
 *   (처음 들어온 사람이 만들고, 다시 만들기를 누르면 version 이 바뀜)
 * - 답: 각자 자기 uid 칸에만 저장 → trips/{id}/balance_meta/answers_{version}
 *   { "<uid>": { "1": "A", "2": "B", ... } }
 */
object BalanceOnline {
    private val db get() = FirebaseFirestore.getInstance()
    private fun meta(tripId: String) =
        db.collection("trips").document(tripId).collection("balance_meta")

    data class SharedQuestions(val version: Long, val questions: List<GameTemplate>)

    /** 같이 쓰는 질문을 가져옴. 없거나 forceNew 면 generate()로 만들어 저장. */
    suspend fun sharedQuestions(
        tripId: String,
        forceNew: Boolean,
        generate: suspend () -> List<GameTemplate>
    ): SharedQuestions {
        val ref = meta(tripId).document("questions")
        if (!forceNew) {
            val existing = runCatching { ref.get().awaitValue() }.getOrNull()?.let { parse(it.data) }
            if (existing != null) return existing
        }
        val generated = generate().mapIndexed { i, q -> q.copy(id = i + 1) }
        val shared = SharedQuestions(System.currentTimeMillis(), generated)
        runCatching { ref.set(toMap(shared)).awaitValue() }
        return shared
    }

    /** 다른 사람이 질문을 다시 만들면 알려 줌 */
    fun listenQuestions(tripId: String, onChange: (SharedQuestions) -> Unit): ListenerRegistration =
        meta(tripId).document("questions").addSnapshotListener { snap, _ ->
            parse(snap?.data)?.let(onChange)
        }

    /** 내 답 저장 (내 uid 칸만) */
    fun saveMyAnswers(tripId: String, version: Long, uid: String, answers: Map<Int, String>) {
        if (uid.isBlank()) return
        val clean = answers.filterValues { it == "A" || it == "B" }.mapKeys { it.key.toString() }
        meta(tripId).document("answers_$version")
            .set(mapOf(uid to clean), SetOptions.merge())
    }

    /** 모두의 답 듣기: uid → (질문 id → "A"/"B") */
    fun listenAnswers(
        tripId: String,
        version: Long,
        onChange: (Map<String, Map<Int, String>>) -> Unit
    ): ListenerRegistration =
        meta(tripId).document("answers_$version").addSnapshotListener { snap, _ ->
            val data = snap?.data ?: emptyMap()
            val result = data.mapNotNull { (uid, value) ->
                val m = value as? Map<*, *> ?: return@mapNotNull null
                uid to m.mapNotNull { (k, v) ->
                    val id = k?.toString()?.toIntOrNull() ?: return@mapNotNull null
                    val opt = v as? String ?: return@mapNotNull null
                    id to opt
                }.toMap()
            }.toMap()
            onChange(result)
        }

    private fun toMap(s: SharedQuestions): Map<String, Any> = mapOf(
        "version" to s.version,
        "items" to s.questions.map {
            mapOf(
                "id" to it.id, "title" to it.title, "optionA" to it.optionA, "optionB" to it.optionB,
                "leftAxis" to it.leftAxis, "rightAxis" to it.rightAxis, "categoryTag" to it.categoryTag
            )
        }
    )

    private fun parse(data: Map<String, Any?>?): SharedQuestions? {
        if (data == null) return null
        val version = (data["version"] as? Number)?.toLong() ?: return null
        val items = (data["items"] as? List<*>)?.mapNotNull { raw ->
            val m = raw as? Map<*, *> ?: return@mapNotNull null
            val optionA = m["optionA"] as? String ?: return@mapNotNull null
            val optionB = m["optionB"] as? String ?: return@mapNotNull null
            GameTemplate(
                id = (m["id"] as? Number)?.toInt() ?: return@mapNotNull null,
                title = m["title"] as? String ?: "",
                optionA = optionA,
                optionB = optionB,
                leftAxis = m["leftAxis"] as? String ?: optionA,
                rightAxis = m["rightAxis"] as? String ?: optionB,
                categoryTag = m["categoryTag"] as? String ?: "자연·힐링"
            )
        }.orEmpty()
        if (items.isEmpty()) return null
        return SharedQuestions(version, items)
    }
}

private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitValue(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWith(Result.failure(it)) }
    }
