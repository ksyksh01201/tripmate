package com.example.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source

data class TripMemberSummary(
    val memberCount: Int = 2,
    val memberNames: List<String> = emptyList()
)

class BalanceGameManager {

    private val db = FirebaseFirestore.getInstance()

    fun createGame(
        tripId: String,
        title: String,
        optionA: String,
        optionB: String,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit,
        order: Long = System.currentTimeMillis()
    ) {
        val gameRef = db.collection("trips").document(tripId)
            .collection("balance_games").document()

        val newGame = BalanceGame(
            gameId = gameRef.id,
            title = title,
            optionA = optionA,
            optionB = optionB,
            countA = 0,
            countB = 0,
            order = order
        )

        gameRef.set(newGame)
            .addOnSuccessListener { onSuccess(gameRef.id) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun createGameFromTemplate(
        tripId: String,
        template: GameTemplate,
        onSuccess: (String) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val gameRef = db.collection("trips").document(tripId)
            .collection("balance_games").document("template_${template.id}")

        val data = hashMapOf<String, Any>(
            "gameId" to gameRef.id,
            "title" to template.title,
            "optionA" to template.optionA,
            "optionB" to template.optionB,
            "order" to template.id.toLong()
        )

        gameRef.set(data, SetOptions.merge())
            .addOnSuccessListener { onSuccess(gameRef.id) }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun createGamesFromTemplates(
        tripId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit,
        templates: List<GameTemplate> = BalanceGameTemplates.list
    ) {
        val collection = db.collection("trips").document(tripId)
            .collection("balance_games")

        db.runBatch { batch ->
            templates.forEach { template ->
                val gameRef = collection.document("template_${template.id}")
                val data = hashMapOf<String, Any>(
                    "gameId" to gameRef.id,
                    "title" to template.title,
                    "optionA" to template.optionA,
                    "optionB" to template.optionB,
                    "order" to template.id.toLong()
                )
                batch.set(gameRef, data, SetOptions.merge())
            }
        }.addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    private fun seedMarkerRef(tripId: String) =
        db.collection("trips").document(tripId)
            .collection("balance_meta").document("seed")

    private fun resultCacheRef(tripId: String) =
        db.collection("trips").document(tripId)
            .collection("balance_meta").document("result")

    fun isSeeded(
        tripId: String,
        onResult: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val firstRef = db.collection("trips").document(tripId)
            .collection("balance_games").document("template_1")

        seedMarkerRef(tripId).get(Source.SERVER)
            .addOnSuccessListener { marker ->
                if (marker.exists()) {
                    onResult(true)
                } else {
                    firstRef.get(Source.SERVER)
                        .addOnSuccessListener { first -> onResult(first.exists()) }
                        .addOnFailureListener { e -> onFailure(e) }
                }
            }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun seedQuestionsIfAbsent(
        tripId: String,
        templates: List<GameTemplate>,
        onSuccess: (Boolean) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val gamesRef = db.collection("trips").document(tripId).collection("balance_games")
        val markerRef = seedMarkerRef(tripId)
        val firstRef = gamesRef.document("template_1")

        db.runTransaction { transaction ->
            val markerExists = transaction.get(markerRef).exists()
            val firstExists = transaction.get(firstRef).exists()

            if (markerExists || firstExists) {
                false
            } else {
                templates.forEach { template ->
                    val gameRef = gamesRef.document("template_${template.id}")
                    val data = hashMapOf<String, Any>(
                        "gameId" to gameRef.id,
                        "title" to template.title,
                        "optionA" to template.optionA,
                        "optionB" to template.optionB,
                        "order" to template.id.toLong()
                    )
                    transaction.set(gameRef, data, SetOptions.merge())
                }
                transaction.set(markerRef, hashMapOf("seededAt" to FieldValue.serverTimestamp()))
                true
            }
        }.addOnSuccessListener { created ->
            onSuccess(created)
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    fun listenToGames(
        tripId: String,
        onUpdate: (List<BalanceGame>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return db.collection("trips").document(tripId)
            .collection("balance_games")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val gameList = snapshot.documents
                        .mapNotNull { doc ->
                            doc.toObject(BalanceGame::class.java)?.copy(gameId = doc.id)
                        }
                        .sortedWith(compareBy<BalanceGame> { it.order }.thenBy { it.gameId })
                    onUpdate(gameList)
                }
            }
    }

    fun getGamesOnce(
        tripId: String,
        onSuccess: (List<BalanceGame>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("trips").document(tripId)
            .collection("balance_games")
            .get()
            .addOnSuccessListener { snapshot ->
                val gameList = snapshot.documents
                    .mapNotNull { doc ->
                        doc.toObject(BalanceGame::class.java)?.copy(gameId = doc.id)
                    }
                    .sortedWith(compareBy<BalanceGame> { it.order }.thenBy { it.gameId })
                onSuccess(gameList)
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun vote(
        tripId: String,
        gameId: String,
        userId: String,
        selectedOption: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        if (selectedOption != "A" && selectedOption != "B") {
            onFailure(IllegalArgumentException("selectedOption은 A 또는 B여야 합니다."))
            return
        }

        val gameRef = db.collection("trips").document(tripId)
            .collection("balance_games").document(gameId)

        val voteRef = gameRef.collection("votes").document(userId)

        val voteData = hashMapOf(
            "userId" to userId,
            "selectedOption" to selectedOption,
            "votedAt" to FieldValue.serverTimestamp()
        )

        db.runTransaction { transaction ->
            val prevOption = transaction.get(voteRef).getString("selectedOption")

            if (prevOption != selectedOption) {
                val deltaA = (if (selectedOption == "A") 1L else 0L) - (if (prevOption == "A") 1L else 0L)
                val deltaB = (if (selectedOption == "B") 1L else 0L) - (if (prevOption == "B") 1L else 0L)
                transaction.set(
                    gameRef,
                    mapOf<String, Any>(
                        "gameId" to gameId,
                        "countA" to FieldValue.increment(deltaA),
                        "countB" to FieldValue.increment(deltaB)
                    ),
                    SetOptions.merge()
                )
            }

            transaction.set(voteRef, voteData, SetOptions.merge())
            null
        }.addOnSuccessListener {
            resultCacheRef(tripId).delete()
            onSuccess()
        }.addOnFailureListener { e ->
            onFailure(e)
        }
    }

    fun submitParticipantAnswers(
        tripId: String,
        memberIndex: Int,
        answersByTemplateId: Map<Int, String>,
        onComplete: () -> Unit = {}
    ) {
        val userId = "test_member_${memberIndex + 1}"
        answersByTemplateId.forEach { (templateId, option) ->
            if (option == "A" || option == "B") {
                vote(
                    tripId = tripId,
                    gameId = "template_$templateId",
                    userId = userId,
                    selectedOption = option,
                    onSuccess = {},
                    onFailure = {}
                )
            }
        }
        onComplete()
    }

    fun loadParticipantAnswers(
        tripId: String,
        memberCount: Int,
        questionIds: List<Int>,
        onLoaded: (Map<Int, Map<Int, String>>) -> Unit
    ) {
        if (memberCount <= 0 || questionIds.isEmpty()) {
            onLoaded(emptyMap())
            return
        }
        val restored = mutableMapOf<Int, MutableMap<Int, String>>()
        val totalCalls = memberCount * questionIds.size
        var completedCalls = 0

        for (mIdx in 0 until memberCount) {
            val userId = "test_member_${mIdx + 1}"
            val memberMap = restored.getOrPut(mIdx) { mutableMapOf() }
            for (qId in questionIds) {
                getUserVote(
                    tripId = tripId,
                    gameId = "template_$qId",
                    userId = userId,
                    onResult = { option ->
                        if (option == "A" || option == "B") {
                            memberMap[qId] = option
                        }
                        completedCalls++
                        if (completedCalls >= totalCalls) {
                            onLoaded(restored)
                        }
                    },
                    onFailure = {
                        completedCalls++
                        if (completedCalls >= totalCalls) {
                            onLoaded(restored)
                        }
                    }
                )
            }
        }
    }

    fun getUserVote(
        tripId: String,
        gameId: String,
        userId: String,
        onResult: (String?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection("trips").document(tripId)
            .collection("balance_games").document(gameId)
            .collection("votes").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    onResult(document.getString("selectedOption"))
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun getCachedAnalysisResult(
        tripId: String,
        onResult: (BalanceGameAnalysisResult?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        resultCacheRef(tripId).get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    onResult(null)
                    return@addOnSuccessListener
                }
                val summary = doc.getString("summary").orEmpty()
                val rawTips = doc.get("compromiseTips") as? List<*>
                val compromiseTips = rawTips?.mapNotNull { it as? String }.orEmpty()
                val rawPlaces = doc.get("places") as? List<*>
                val places = rawPlaces?.mapNotNull { item ->
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    RecommendedPlace(
                        name = map["name"] as? String ?: "",
                        category = map["category"] as? String ?: "",
                        reason = map["reason"] as? String ?: "",
                        tag = map["tag"] as? String ?: ""
                    )
                }.orEmpty()

                if (places.isNotEmpty()) {
                    onResult(
                        BalanceGameAnalysisResult(
                            summary = summary,
                            compromiseTips = compromiseTips,
                            places = places
                        )
                    )
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun saveAnalysisResult(
        tripId: String,
        result: BalanceGameAnalysisResult,
        memberCount: Int? = null,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val placesData = result.places.map { place ->
            hashMapOf(
                "name" to place.name,
                "category" to place.category,
                "reason" to place.reason,
                "tag" to place.tag
            )
        }
        val payload = hashMapOf<String, Any>(
            "summary" to result.summary,
            "compromiseTips" to result.compromiseTips,
            "places" to placesData,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (memberCount != null) {
            payload["memberCount"] = memberCount.coerceIn(2, 4)
        }
        resultCacheRef(tripId).set(payload, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onFailure(e) }
    }

    fun listenToLatestGameMemberCount(
        tripId: String,
        onUpdate: (Int?) -> Unit
    ): ListenerRegistration {
        return resultCacheRef(tripId)
            .addSnapshotListener { doc, error ->
                if (error != null || doc == null || !doc.exists()) {
                    onUpdate(null)
                    return@addSnapshotListener
                }
                val savedCount = doc.getLong("memberCount")?.toInt()
                onUpdate(savedCount?.coerceIn(2, 4))
            }
    }

    fun listenToTripMembers(
        tripId: String,
        fallbackCount: Int = 2,
        onUpdate: (TripMemberSummary) -> Unit
    ): ListenerRegistration {
        return db.collection("trips").document(tripId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    onUpdate(TripMemberSummary(memberCount = fallbackCount))
                    return@addSnapshotListener
                }

                val rawMembers = (snapshot.get("members") as? List<*>)
                    ?: (snapshot.get("memberIds") as? List<*>)
                    ?: (snapshot.get("participantIds") as? List<*>)

                val explicitNames = (snapshot.get("memberNames") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.filter { it.isNotBlank() }
                    .orEmpty()

                val parsedNames = if (explicitNames.isNotEmpty()) {
                    explicitNames
                } else {
                    rawMembers?.mapNotNull { item ->
                        when (item) {
                            is Map<*, *> -> (item["name"] as? String)
                                ?: (item["nickname"] as? String)
                                ?: (item["userName"] as? String)
                            is String -> item.takeIf { it.isNotBlank() }
                            else -> null
                        }
                    }.orEmpty()
                }

                val count = when {
                    !rawMembers.isNullOrEmpty() -> rawMembers.size
                    parsedNames.isNotEmpty() -> parsedNames.size
                    else -> snapshot.getLong("memberCount")?.toInt() ?: fallbackCount
                }.coerceAtLeast(1)

                onUpdate(TripMemberSummary(memberCount = count, memberNames = parsedNames))
            }
    }

    fun listenToTripMemberCount(
        tripId: String,
        fallbackCount: Int = 2,
        onUpdate: (Int) -> Unit
    ): ListenerRegistration {
        return listenToTripMembers(
            tripId = tripId,
            fallbackCount = fallbackCount,
            onUpdate = { summary -> onUpdate(summary.memberCount) }
        )
    }
}