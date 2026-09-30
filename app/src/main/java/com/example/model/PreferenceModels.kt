package com.example.model

data class BalanceOption(
    val text: String,
    val subText: String = "",
    val tag: String = "",
    val tags: List<String> = if (tag.isNotEmpty()) listOf(tag) else emptyList(),
    val categoryWeights: Map<String, Int> = emptyMap()
)

data class BalanceQuestion(
    val id: Int,
    val question: String,
    val optionA: BalanceOption,
    val optionB: BalanceOption
)

data class CategoryScore(
    val category: String,
    val percentage: Int
)

data class PersonalPreferenceResult(
    val title: String,
    val description: String,
    val topTraits: List<String>,
    val categoryScores: List<CategoryScore>
)

data class ConsensusItem(
    val title: String,
    val votesRatio: String, // e.g., "4명 중 4명 선택"
    val percentage: Int
)

data class ConflictItem(
    val optionA: String,
    val optionB: String,
    val percentA: Int,
    val percentB: Int,
    val ratioText: String = "",
    val description: String = ""
)

data class GroupPreferenceResult(
    val summary: String,
    val topCategories: List<CategoryScore>,
    val consensusList: List<ConsensusItem>,
    val conflictList: List<ConflictItem>,
    val completedMemberCount: Int = 0
)

data class RecommendedPlace(
    val id: String,
    val name: String,
    val categories: List<String>,
    val matchRate: Int,
    val description: String,
    val location: String = "부산",
    var isLiked: Boolean = false
)

object PreferenceQuestions {
    fun getJosaGwaWa(word: String): String {
        if (word.isEmpty()) return "과"
        val lastChar = word.last()
        if (lastChar < '가' || lastChar > '힣') return "과"
        val hasBatchim = (lastChar.code - 0xAC00) % 28 != 0
        return if (hasBatchim) "과" else "와"
    }

    fun getJosaEulReul(word: String): String {
        if (word.isEmpty()) return "을"
        val lastChar = word.last()
        if (lastChar < '가' || lastChar > '힣') return "을"
        val hasBatchim = (lastChar.code - 0xAC00) % 28 != 0
        return if (hasBatchim) "을" else "를"
    }

    val questions: List<BalanceQuestion> = listOf(
        BalanceQuestion(
            id = 1,
            question = "여행에서 더 중요한 것은?",
            optionA = BalanceOption(
                text = "맛집 탐방",
                subText = "줄 서서라도 유명한 로컬 맛집 정복!",
                tag = "맛집",
                tags = listOf("맛집")
            ),
            optionB = BalanceOption(
                text = "관광 명소",
                subText = "지역 랜드마크와 역사적 유적지 관람!",
                tag = "관광",
                tags = listOf("관광", "사진")
            )
        ),
        BalanceQuestion(
            id = 2,
            question = "여행 스타일은?",
            optionA = BalanceOption(
                text = "계획적인 여행",
                subText = "분 단위 동선과 사전 예약 필수!",
                tag = "계획",
                tags = listOf("계획")
            ),
            optionB = BalanceOption(
                text = "즉흥적인 여행",
                subText = "발길 닿는 대로, 여유로운 즉흥 선택!",
                tag = "즉흥",
                tags = listOf("즉흥")
            )
        ),
        BalanceQuestion(
            id = 3,
            question = "아침 시간 활용은?",
            optionA = BalanceOption(
                text = "아침부터 알차게",
                subText = "일찍 일어나서 하루를 길고 꽉 차게!",
                tag = "관광",
                tags = listOf("관광")
            ),
            optionB = BalanceOption(
                text = "여유로운 오전",
                subText = "늦잠 자고 브런치로 여유롭게 시작!",
                tag = "휴식",
                tags = listOf("휴식")
            )
        ),
        BalanceQuestion(
            id = 4,
            question = "선호하는 장소 분위기는?",
            optionA = BalanceOption(
                text = "유명 관광지",
                subText = "핫플레이스와 인증샷 필수 코스!",
                tag = "관광",
                tags = listOf("관광", "사진")
            ),
            optionB = BalanceOption(
                text = "로컬 장소",
                subText = "현지인들만 아는 조용하고 한적한 골목!",
                tag = "로컬",
                tags = listOf("로컬")
            )
        ),
        BalanceQuestion(
            id = 5,
            question = "여행 활동의 핵심은?",
            optionA = BalanceOption(
                text = "액티비티",
                subText = "서핑, 레저, 테마파크로 온몸으로 즐기기!",
                tag = "액티비티",
                tags = listOf("액티비티")
            ),
            optionB = BalanceOption(
                text = "휴식",
                subText = "오션뷰 카페에서 물멍 때리고 힐링하기!",
                tag = "휴식",
                tags = listOf("휴식")
            )
        ),
        BalanceQuestion(
            id = 6,
            question = "남는 것은 무엇?",
            optionA = BalanceOption(
                text = "사진 중심",
                subText = "인생샷 건질 수 있는 뷰포인트가 최고!",
                tag = "사진",
                tags = listOf("사진")
            ),
            optionB = BalanceOption(
                text = "맛집 중심",
                subText = "입이 즐거워야 진짜 남는 여행이지!",
                tag = "맛집",
                tags = listOf("맛집")
            )
        ),
        BalanceQuestion(
            id = 7,
            question = "하루 방문 장소 수는?",
            optionA = BalanceOption(
                text = "많은 장소 방문",
                subText = "체력 닿는 한 여러 곳을 두루두루!",
                tag = "관광",
                tags = listOf("관광")
            ),
            optionB = BalanceOption(
                text = "적은 장소를 여유롭게",
                subText = "한두 군데에서 깊이 있게 머물기!",
                tag = "휴식",
                tags = listOf("휴식")
            )
        ),
        BalanceQuestion(
            id = 8,
            question = "여행 경비 철학은?",
            optionA = BalanceOption(
                text = "가성비",
                subText = "합리적인 소비로 알뜰하고 똑똑하게!",
                tag = "가성비",
                tags = listOf("가성비")
            ),
            optionB = BalanceOption(
                text = "프리미엄",
                subText = "여행지에서만큼은 아낌없이 플렉스!",
                tag = "프리미엄",
                tags = listOf("프리미엄")
            )
        ),
        BalanceQuestion(
            id = 9,
            question = "어디로 떠나고 싶나요?",
            optionA = BalanceOption(
                text = "도시",
                subText = "화려한 야경, 편리한 교통과 문화 인프라!",
                tag = "도시",
                tags = listOf("도시")
            ),
            optionB = BalanceOption(
                text = "자연",
                subText = "푸른 바다와 산, 맑은 공기 속 피톤치드!",
                tag = "자연",
                tags = listOf("자연")
            )
        ),
        BalanceQuestion(
            id = 10,
            question = "자유시간이 주어진다면?",
            optionA = BalanceOption(
                text = "쇼핑",
                subText = "기념품, 지역 특산품, 소품샵 투어!",
                tag = "쇼핑",
                tags = listOf("쇼핑")
            ),
            optionB = BalanceOption(
                text = "체험",
                subText = "요트 투어, 원데이 클래스, 공방 체험!",
                tag = "체험",
                tags = listOf("체험")
            )
        )
    )

    fun calculatePersonalResult(selectedAnswers: Map<Int, Boolean>): PersonalPreferenceResult {
        val travelThemeCategories = listOf(
            "관광", "사진", "맛집", "휴식", "액티비티",
            "자연", "쇼핑", "체험", "로컬"
        )
        val styleCategories = listOf(
            "계획", "즉흥", "가성비", "프리미엄", "도시"
        )
        val allCategories = travelThemeCategories + styleCategories

        // [3. 개인 취향 계산]
        // 각 취향 점수는 해당 취향과 관련된 질문에서 사용자가 그 취향을 선택한 비율을 기준으로 계산
        fun scoreFor(cat: String): Pair<CategoryScore, Int> {
            val opportunities = questions.count { q ->
                cat in q.optionA.tags || cat in q.optionB.tags
            }
            val selected = selectedAnswers.count { (qId, isOptionA) ->
                val q = questions.find { it.id == qId } ?: return@count false
                val chosen = if (isOptionA) q.optionA else q.optionB
                cat in chosen.tags
            }
            val percentage = if (opportunities > 0) {
                Math.round((selected.toDouble() / opportunities) * 100).toInt()
            } else 0
            return Pair(CategoryScore(cat, percentage), selected)
        }

        // 여행 취향(관광, 사진, 맛집, 휴식, 쇼핑 등)을 상위 취향(topTraits)의 우선 기준으로 정렬
        val themeScores = travelThemeCategories.map { scoreFor(it) }.sortedWith(
            compareByDescending<Pair<CategoryScore, Int>> { it.first.percentage }
                .thenByDescending { it.second } // 선택 횟수가 많은 항목 우선
                .thenBy { it.first.category }
        )

        val otherScores = styleCategories.map { scoreFor(it) }.sortedWith(
            compareByDescending<Pair<CategoryScore, Int>> { it.first.percentage }
                .thenByDescending { it.second }
                .thenBy { it.first.category }
        )

        val categoryPercentages = (themeScores + otherScores).map { it.first }

        // [4. 개인 결과] 여행 취향 상위 3개 추출
        val top3Categories = themeScores.take(3).map { it.first }
        val top1 = top3Categories.getOrNull(0)?.category ?: "관광"
        val top2 = top3Categories.getOrNull(1)?.category ?: "사진"
        val top3 = top3Categories.getOrNull(2)?.category ?: "맛집"

        // 대표 여행 성향 문구 (실제 상위 취향을 기반으로 생성)
        val title = "${top1}${getJosaGwaWa(top1)} ${top2}${getJosaEulReul(top2)} 좋아하는 여행자"
        val desc = "선택하신 취향 중 ${top1}${getJosaGwaWa(top1)} ${top2} 성향이 가장 높게 나타났어요."

        return PersonalPreferenceResult(
            title = title,
            description = desc,
            topTraits = listOf(top1, top2, top3),
            categoryScores = categoryPercentages
        )
    }

    fun calculateGroupResult(
        completedMembers: List<TripMember>
    ): GroupPreferenceResult {
        val activeMembers = completedMembers.filter { it.preferenceCompleted && it.preferenceResult != null }
        if (activeMembers.isEmpty()) {
            return GroupPreferenceResult(
                summary = "아직 완료된 취향 결과가 없어요.",
                topCategories = emptyList(),
                consensusList = emptyList(),
                conflictList = emptyList(),
                completedMemberCount = 0
            )
        }

        val totalMembers = activeMembers.size
        val travelThemeCategories = listOf(
            "관광", "사진", "맛집", "휴식", "액티비티",
            "자연", "쇼핑", "체험", "로컬"
        )
        val styleCategories = listOf(
            "계획", "즉흥", "가성비", "프리미엄", "도시"
        )

        // [5. 그룹 취향 계산]
        // 각 사용자의 취향 점수를 평균내서 그룹 취향 점수 계산
        fun averageFor(cat: String): CategoryScore {
            val sum = activeMembers.sumOf { member ->
                member.preferenceResult?.categoryScores?.find { it.category == cat }?.percentage ?: 0
            }
            val avg = Math.round(sum.toDouble() / totalMembers).toInt()
            return CategoryScore(cat, avg)
        }

        val themeAverages = travelThemeCategories.map { averageFor(it) }.sortedWith(
            compareByDescending<CategoryScore> { it.percentage }
                .thenBy { it.category }
        )

        val otherAverages = styleCategories.map { averageFor(it) }.sortedWith(
            compareByDescending<CategoryScore> { it.percentage }
                .thenBy { it.category }
        )

        val categoryAverages = themeAverages + otherAverages

        val top1 = themeAverages.getOrNull(0)?.category ?: "관광"
        val top2 = themeAverages.getOrNull(1)?.category ?: "사진"
        val top3 = themeAverages.getOrNull(2)?.category ?: "맛집"

        // [가장 잘 맞아요]
        val consensus = mutableListOf<ConsensusItem>()
        val top3Cats = themeAverages.take(3)
        top3Cats.forEach { catScore ->
            val agreeCount = activeMembers.count { m ->
                val score = m.preferenceResult?.categoryScores?.find { it.category == catScore.category }?.percentage ?: 0
                score >= 50 || m.preferenceResult?.topTraits?.contains(catScore.category) == true
            }.coerceAtLeast(1)
            consensus.add(
                ConsensusItem(
                    title = catScore.category,
                    votesRatio = "${catScore.category} · ${agreeCount}/${totalMembers}명",
                    percentage = (agreeCount * 100) / totalMembers
                )
            )
        }

        // [9. 의견이 갈리는 항목]
        // 사용자들의 선택이 서로 다른 항목이 있다면 "의견이 갈려요" 영역에 표시
        val conflicts = mutableListOf<ConflictItem>()
        if (totalMembers > 1) {
            val opposingQuestions = listOf(
                Triple(2, Pair("계획", "즉흥"), "계획적인 일정을 선호하지만 유연한 시간도 필요해요."),
                Triple(5, Pair("액티비티", "휴식"), "신나는 활동과 편안한 힐링 시간을 조율해보세요."),
                Triple(8, Pair("가성비", "프리미엄"), "가성비와 특별한 소비를 적절히 배분해보세요."),
                Triple(9, Pair("도시", "자연"), "도심 관광과 자연 속 여유를 함께 즐겨보세요."),
                Triple(10, Pair("쇼핑", "체험"), "쇼핑 시간과 체험 프로그램을 함께 계획해보세요."),
                Triple(1, Pair("맛집", "관광"), "유명 관광지와 맛집 코스를 균형 있게 섞어보세요."),
                Triple(4, Pair("관광", "로컬"), "대표 핫플과 숨은 골목을 조화롭게 방문해보세요.")
            )

            for ((qId, pair, desc) in opposingQuestions) {
                val countA = activeMembers.count { it.preferenceAnswers[qId] == true }
                val countB = activeMembers.count { it.preferenceAnswers[qId] == false }
                // 선택이 서로 다른 경우에만 추가
                if (countA > 0 && countB > 0) {
                    val percentA = (countA * 100) / totalMembers
                    val percentB = 100 - percentA
                    conflicts.add(
                        ConflictItem(
                            optionA = pair.first,
                            optionB = pair.second,
                            percentA = percentA,
                            percentB = percentB,
                            ratioText = "${pair.first} $countA vs ${pair.second} $countB",
                            description = desc
                        )
                    )
                }
            }
        }

        val summary = "우리 그룹은 ${top1}${getJosaGwaWa(top1)} ${top2}${getJosaEulReul(top2)} 가장 선호하는 여행 그룹이에요."

        return GroupPreferenceResult(
            summary = summary,
            topCategories = categoryAverages,
            consensusList = consensus,
            conflictList = conflicts,
            completedMemberCount = totalMembers
        )
    }

    fun calculateGroupResult(
        userPersonalResult: PersonalPreferenceResult?,
        memberCount: Int = 1
    ): GroupPreferenceResult {
        if (userPersonalResult == null) {
            return GroupPreferenceResult(
                summary = "아직 완료된 취향 결과가 없어요.",
                topCategories = emptyList(),
                consensusList = emptyList(),
                conflictList = emptyList(),
                completedMemberCount = 0
            )
        }
        val dummyMember = TripMember(
            id = "user_me",
            name = "나",
            preferenceCompleted = true,
            preferenceResult = userPersonalResult
        )
        return calculateGroupResult(listOf(dummyMember))
    }
}

