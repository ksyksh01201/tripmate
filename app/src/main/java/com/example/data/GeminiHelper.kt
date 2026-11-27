package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * 질문 번호별 장소 추천 기준 (A쪽 / B쪽) - 5개 축.
 */
object PlaceCriteria {
    val list = listOf(
        "감성 분위기·골목 스팟 / 핫플레이스·랜드마크",
        "자연·바다·숲 힐링 명소 / 도시·전시·도심 스팟",
        "로컬 노포·현지 맛집·전통시장 / 인기 맛집·트렌디 카페",
        "여유로운 산책·뷰 명소 / 액티비티·체험·활발한 스팟",
        "낮 햇살·야경·포토 명소 / 편한 이동·휴식 중심 스팟"
    )
}

data class RecommendedPlace(
    val name: String = "",
    val category: String = "",
    val reason: String = "",
    val tag: String = ""
)

data class BalanceGameAnalysisResult(
    val summary: String = "",
    val compromiseTips: List<String> = emptyList(),
    val places: List<RecommendedPlace> = emptyList()
)

class GeminiHelper {

    private val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = "gemini-3.5-flash-lite",
        generationConfig = generationConfig {
            responseMimeType = "application/json"
        }
    )

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    suspend fun getRandom5BalanceGames(tripTitle: String): List<GameTemplate> {
        return try {
            val criteriaText = PlaceCriteria.list
                .mapIndexed { index, criteria -> "${index + 1}. $criteria" }
                .joinToString("\n")

            val prompt = """
                여행 동행자들에게 어울리는 '실제 방문할 장소'를 추천해주기 위한 밸런스 게임 질문 5개를 만들어줘.
                여행 테마: $tripTitle

                [장소 추천 기준] 번호 순서와 기준을 절대 바꾸지 말 것.
                선택지 A는 '/' 앞쪽 장소, 선택지 B는 '/' 뒤쪽 장소가 명확히 대비되어야 한다.
                $criteriaText

                [작성 규칙]
                1. 일정표, 계획, 동선 같은 말은 절대 쓰지 말고 오직 '어떤 장소에 가고 싶은지'만 물어볼 것.
                2. title: 가고 싶은 장소를 고르는 상황처럼 자연스럽고 센스 있는 질문으로 작성 (18~28자 내외).
                3. optionA, optionB: 추천해 줄 수 있는 구체적인 장소 유형과 특징이 한눈에 보이도록 작성 (15~25자 내외).
                4. 특정 가게 상호명은 쓰지 말고 장소 카테고리로 표현할 것.
                5. 반드시 아래 JSON 배열 형식으로만 응답할 것 (id는 1부터 5까지 순서대로):
                [
                  {"id": 1, "title": "질문 제목", "optionA": "선택지 A", "optionB": "선택지 B"},
                  {"id": 2, "title": "질문 제목", "optionA": "선택지 A", "optionB": "선택지 B"}
                ]
            """.trimIndent()

            val response = model.generateContent(prompt)
            val rawText = response.text ?: throw Exception("제미나이 응답이 비어있습니다.")
            Log.d("GeminiHelper", "질문 원본 응답: $rawText")

            val cleanJson = rawText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val listType = Types.newParameterizedType(List::class.java, GameTemplate::class.java)
            val adapter = moshi.adapter<List<GameTemplate>>(listType)
            val parsed = adapter.fromJson(cleanJson) ?: throw Exception("질문 데이터 변환 실패")

            if (parsed.size != 5) {
                throw Exception("질문이 5개가 아닙니다. (${parsed.size}개)")
            }
            parsed.forEachIndexed { index, q ->
                if (q.title.isBlank() || q.optionA.isBlank() || q.optionB.isBlank() || q.optionA == q.optionB) {
                    throw Exception("${index + 1}번 질문 형식이 올바르지 않습니다.")
                }
            }

            parsed.mapIndexed { index, q -> q.copy(id = index + 1) }
        } catch (e: Exception) {
            Log.w("GeminiHelper", "AI 질문 생성 실패, 비상용 질문 5개 사용: ${e.message}")
            fallback5BalanceGames(tripTitle)
        }
    }

    /**
     * 밸런스 게임 투표 결과를 분석해 케미 헤드라인 + 타협 팁 2가지 + 맞춤 여행 장소 6곳을 추천한다.
     */
    suspend fun getRecommendedPlaces(
        tripTitle: String,
        games: List<BalanceGame>
    ): BalanceGameAnalysisResult {
        return try {
            val voteSummaryText = games.take(5).mapIndexed { index, g ->
                val consensus = when {
                    g.countA > 0 && g.countB == 0 -> "만장일치 A 선호 (${g.optionA})"
                    g.countB > 0 && g.countA == 0 -> "만장일치 B 선호 (${g.optionB})"
                    g.countA == g.countB && g.countA > 0 -> "의견 반반 갈림 (A: ${g.optionA} vs B:${g.optionB})"
                    g.countA > g.countB -> "A 우세 (${g.optionA}${g.countA}표 vs ${g.optionB}${g.countB}표)"
                    g.countB > g.countA -> "B 우세 (${g.optionB}${g.countB}표 vs ${g.optionA}${g.countA}표)"
                    else -> "투표 없음"
                }
                "${index + 1}. [${g.title}] ->$consensus"
            }.joinToString("\n")

            val prompt = """
                여행 동행자들의 밸런스 게임 투표 결과를 분석해서, 케미 요약 문구, 타협 팁 2가지, 그리고 실제 방문 가능한 맞춤형 장소 6곳을 추천해줘.
                여행 테마/지역: $tripTitle

                [밸런스 게임 투표 결과]
                $voteSummaryText

                [작성 규칙]
                1. summary: 동행자들의 취향 궁합과 갈린 포인트를 줄바꿈(\n) 포함 2줄(20~35자 내외)로 임팩트 있게 요약할 것.
                2. compromiseTips: 의견이 갈렸거나 다수결인 항목을 어떻게 조율하면 좋을지 구체적인 실행 팁 정확히 2개(각 25~45자 내외). 번호(1., 2.)는 붙이지 말고 문장만 작성.
                3. places: 위 투표 결과를 바탕으로 실제 방문 가능한 장소 정확히 6곳을 추천할 것.
                4. 카테고리가 겹치지 않게 다양하게 구성할 것 (명소/해변, 맛집, 카페, 야경/포토스팟, 체험/쇼핑 등).
                5. name: 실제 존재하는 구체적인 장소명 (여행 테마가 특정 지역이면 해당 지역의 실제 명소·거리·카페·맛집 위주).
                6. category: 짧은 카테고리명.
                7. tag: 핵심 매력 해시태그 1개 (예: "#만장일치픽", "#취향절충", "#인생샷스팟").
                8. reason: 어떤 질문 선택과 연결되어 추천했는지 구체적인 이유 1~2문장.
                9. 반드시 아래 JSON 객체 형식으로만 응답할 것:
                {
                  "summary": "케미 요약\n두번째 줄",
                  "compromiseTips": [
                    "첫 번째 타협 팁 문장",
                    "두 번째 타협 팁 문장"
                  ],
                  "places": [
                    {
                      "name": "장소명",
                      "category": "카테고리",
                      "tag": "#해시태그",
                      "reason": "추천 이유"
                    }
                  ]
                }
            """.trimIndent()

            val response = model.generateContent(prompt)
            val rawText = response.text ?: throw Exception("제미나이 추천 응답이 비어있습니다.")
            Log.d("GeminiHelper", "장소 추천 원본 응답: $rawText")

            val cleanJson = rawText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val adapter = moshi.adapter(BalanceGameAnalysisResult::class.java)
            val parsed = adapter.fromJson(cleanJson) ?: throw Exception("장소 추천 데이터 변환 실패")

            if (parsed.places.isEmpty()) {
                throw Exception("추천 장소가 비어있습니다.")
            }

            parsed
        } catch (e: Exception) {
            Log.w("GeminiHelper", "AI 추천 분석 실패, 비상용 결과 사용: ${e.message}")
            fallbackAnalysisResult(tripTitle, games)
        }
    }

    private fun fallback5BalanceGames(tripTitle: String): List<GameTemplate> {
        val regionPrefix = if (tripTitle.isNotBlank()) "${tripTitle}에서 " else ""
        return listOf(
            GameTemplate(
                id = 1,
                title = "${regionPrefix}이번 여행 분위기, 어디가 더 끌려?",
                optionA = "조용한 감성 골목·소품샵",
                optionB = "활기찬 핫플·랜드마크",
                leftAxis = "감성",
                rightAxis = "핫플",
                categoryTag = "로컬·감성"
            ),
            GameTemplate(
                id = 2,
                title = "풍경은 어떤 쪽이 더 좋아?",
                optionA = "바다·숲길 자연 명소",
                optionB = "도심·전시 문화 스팟",
                leftAxis = "자연",
                rightAxis = "도시",
                categoryTag = "자연·힐링"
            ),
            GameTemplate(
                id = 3,
                title = "먹고 쉬는 스팟은 어디가 좋아?",
                optionA = "로컬 맛집·전통시장",
                optionB = "감성 카페·인기 맛집",
                leftAxis = "로컬",
                rightAxis = "트렌디",
                categoryTag = "로컬·감성"
            ),
            GameTemplate(
                id = 4,
                title = "어떤 느낌의 장소가 더 끌려?",
                optionA = "여유롭게 걷는 산책·뷰 명소",
                optionB = "직접 즐기는 체험·액티비티",
                leftAxis = "여유",
                rightAxis = "활발",
                categoryTag = "자연·힐링"
            ),
            GameTemplate(
                id = 5,
                title = "하루 마무리 스팟은 어디로 할까?",
                optionA = "야경·노을 포토 스팟",
                optionB = "편하게 쉬는 휴식 명소",
                leftAxis = "야경·감성",
                rightAxis = "휴식",
                categoryTag = "유명·핫플"
            )
        )
    }

    private fun fallbackAnalysisResult(
        tripTitle: String,
        games: List<BalanceGame>
    ): BalanceGameAnalysisResult {
        val area = tripTitle.takeIf { it.isNotBlank() } ?: "여행지"
        val tiedCount = games.take(5).count { it.countA > 0 && it.countA == it.countB }
        val summary = if (tiedCount > 0) {
            "$area 취향 궁합 체크 완료!\n갈린 선택은 센스 있게 조율해요"
        } else {
            "$area 취향 궁합 딱 맞음!\n서로 끌리는 스팟 위주로 골랐어요"
        }

        val tips = listOf(
            "취향이 갈린 항목은 오전·오후로 나눠 양쪽 취향을 골고루 챙겨보세요.",
            "동선이 가까운 스팟끼리 묶으면 이동 시간을 줄이고 여유롭게 즐길 수 있어요."
        )

        val places = listOf(
            RecommendedPlace(
                name = "$area 해변 산책로",
                category = "오션뷰 명소",
                tag = "#기본추천",
                reason = "탁 트인 풍경을 보며 가볍게 걷기 좋은 대표 산책 스팟입니다."
            ),
            RecommendedPlace(
                name = "$area 로컬 맛집 거리",
                category = "로컬 맛집",
                tag = "#먹거리픽",
                reason = "현지 분위기를 느끼며 든든한 한 끼를 즐기기 좋은 먹거리 스팟입니다."
            ),
            RecommendedPlace(
                name = "$area 감성 로스터리 카페",
                category = "감성 카페",
                tag = "#쉬어가기",
                reason = "이동 중간에 커피와 디저트를 즐기며 대화하기 좋은 카페입니다."
            ),
            RecommendedPlace(
                name = "$area 전망대 포토스팟",
                category = "포토 스팟",
                tag = "#사진맛집",
                reason = "여행의 분위기를 사진으로 남기기 좋은 전망 포인트입니다."
            ),
            RecommendedPlace(
                name = "$area 문화 전시 공간",
                category = "전시·문화",
                tag = "#실내추천",
                reason = "날씨나 시간대에 구애받지 않고 색다른 볼거리를 즐길 수 있습니다."
            ),
            RecommendedPlace(
                name = "$area 야경 명소",
                category = "야경 스팟",
                tag = "#하루마무리",
                reason = "저녁 시간대 여행 분위기를 차분하고 멋지게 마무리하기 좋습니다."
            )
        )

        return BalanceGameAnalysisResult(
            summary = summary,
            compromiseTips = tips,
            places = places
        )
    }
}