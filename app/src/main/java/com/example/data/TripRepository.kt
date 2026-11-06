package com.example.data

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 여행 데이터를 Firestore "trips" 컬렉션에 저장하고 불러옵니다.
 *
 * 화면(UI)에서 쓰는 함수
 * - createTrip()        : 새 여행 생성 + 초대 코드 발급
 * - joinTripWithCode()  : 초대 코드로 여행 참여
 * - getMyTrips()        : 내가 참여한 여행 목록 (실시간 Flow)
 * - getTripDetail()     : 여행 하나의 상세 정보 (실시간 Flow) → 여행룸 화면 분기용
 */
class TripRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val tripsCollection = db.collection("trips")

    /** 지금 로그인한 사용자의 UID (없으면 null) */
    val currentUid: String?
        get() = auth.currentUser?.uid

    /**
     * 로그인한 사용자가 있으면 그 UID를, 없으면 익명 로그인 후 UID를 돌려줍니다.
     * (현재 로그인 화면이 Firebase Auth와 아직 연결되지 않아서 익명 로그인으로 테스트합니다.)
     */
    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: throw TripException("로그인에 실패했습니다.")
    }

    /**
     * 1) 새 여행을 만들고, 만들어진 여행을 돌려줍니다.
     * - 무작위 초대 코드(예: TRIP7K2A)를 발급해 함께 저장합니다.
     * - 방장 UID를 createdBy와 members의 첫 번째 요소로 넣습니다.
     */
    suspend fun createTrip(
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        companionType: CompanionType
    ): Trip {
        val uid = ensureSignedIn()
        val docRef = tripsCollection.document() // 문서 ID를 미리 만들어 tripId로 사용
        val trip = Trip(
            tripId = docRef.id,
            title = title.ifBlank { "$destination 여행" },
            destination = destination,
            startDate = startDate,
            endDate = endDate,
            companionType = companionType.name,
            inviteCode = generateUniqueInviteCode(),
            createdBy = uid,
            members = listOf(uid),
            createdAt = Timestamp.now()
        )
        docRef.set(trip).await()
        return trip
    }

    /**
     * 2) 초대 코드로 여행에 참여하고, 참여한 여행을 돌려줍니다.
     * - 대소문자와 앞뒤 공백은 무시합니다. ("trip0101 " → "TRIP0101")
     * - 이미 멤버라면 그대로 그 여행을 돌려줍니다.
     * - 코드가 없으면 TripException("존재하지 않는 초대 코드입니다.")을 던집니다.
     */
    suspend fun joinTripWithCode(inviteCode: String): Trip {
        val uid = ensureSignedIn()
        val code = normalizeCode(inviteCode)
        if (code.isEmpty()) throw TripException("초대 코드를 입력해 주세요.")

        val snapshot = tripsCollection
            .whereEqualTo("inviteCode", code)
            .limit(1)
            .get()
            .await()
        val doc = snapshot.documents.firstOrNull()
            ?: throw TripException("존재하지 않는 초대 코드입니다.")
        val trip = doc.toObject(Trip::class.java)
            ?: throw TripException("여행 정보를 불러오지 못했습니다.")

        if (uid in trip.members) return trip

        // arrayUnion: 같은 UID가 두 번 들어가지 않고, 동시에 여러 명이 참여해도 안전합니다.
        doc.reference.update("members", FieldValue.arrayUnion(uid)).await()
        return trip.copy(members = trip.members + uid)
    }

    /**
     * 3) 내가 members에 들어 있는 여행 목록을 실시간으로 받습니다. (시작일이 가까운 순)
     * 누가 새 여행에 초대하거나 여행이 바뀌면 자동으로 새 목록이 흘러나옵니다.
     */
    fun getMyTrips(): Flow<List<Trip>> = callbackFlow {
        val uid = try {
            ensureSignedIn()
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }
        val registration = tripsCollection
            .whereArrayContains("members", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val trips = snapshot?.documents
                    ?.mapNotNull { it.toObject(Trip::class.java) }
                    ?.sortedBy { it.startDate }
                    .orEmpty()
                trySend(trips)
            }
        awaitClose { registration.remove() }
    }

    /**
     * 4) 여행 하나의 상세 정보를 실시간으로 받습니다. (여행룸 화면 분기용)
     * 다른 사람이 초대 코드로 들어와 members가 바뀌면 즉시 새 Trip이 흘러나옵니다.
     * 문서가 삭제되었거나 없으면 null이 흘러나옵니다.
     *
     * 사용 예)
     *   val trip by repository.getTripDetail(tripId).collectAsState(initial = null)
     *   if (trip.members.size >= 2) 밸런스 게임 화면 else 초대 코드 화면
     */
    fun getTripDetail(tripId: String): Flow<Trip?> = callbackFlow {
        val registration = tripsCollection
            .document(tripId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(Trip::class.java))
            }
        awaitClose { registration.remove() }
    }

    /** 다른 여행과 겹치지 않는 초대 코드를 만듭니다. */
    private suspend fun generateUniqueInviteCode(): String {
        repeat(MAX_CODE_ATTEMPTS) {
            val code = InviteCodeGenerator.generate()
            val exists = !tripsCollection
                .whereEqualTo("inviteCode", code)
                .limit(1)
                .get()
                .await()
                .isEmpty
            if (!exists) return code
        }
        throw TripException("초대 코드를 만들지 못했습니다. 다시 시도해 주세요.")
    }

    private fun normalizeCode(code: String): String = code.trim().uppercase()

    private companion object {
        const val MAX_CODE_ATTEMPTS = 5
    }
}

/**
 * 초대 코드 생성기: "TRIP" + 영문 대문자/숫자 4자리 (예: TRIP0101, TRIP7K2A)
 * 헷갈리기 쉬운 문자(O, I)는 빼서 친구에게 불러 주기 쉽게 했습니다.
 */
object InviteCodeGenerator {
    private const val PREFIX = "TRIP"
    private const val CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ0123456789"
    private const val LENGTH = 4

    fun generate(random: kotlin.random.Random = kotlin.random.Random.Default): String =
        PREFIX + (1..LENGTH).map { CHARS[random.nextInt(CHARS.length)] }.joinToString("")
}

/** 화면에 그대로 보여 줘도 되는 메시지를 담은 예외 */
class TripException(message: String) : Exception(message)

/** Firebase Task를 코루틴에서 기다릴 수 있게 해 주는 함수 (추가 라이브러리 없이 사용) */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { e -> cont.resumeWithException(e) }
    addOnCanceledListener { cont.cancel() }
}
