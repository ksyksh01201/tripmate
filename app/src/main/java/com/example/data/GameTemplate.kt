package com.example.data

data class GameTemplate(
    val id: Int,
    val title: String,
    val optionA: String,
    val optionB: String,
    val leftAxis: String = optionA,
    val rightAxis: String = optionB,
    val categoryTag: String = "자연·힐링"
)

object BalanceGameTemplates {
    val list = listOf(
        GameTemplate(
            id = 1,
            title = "이번 여행 분위기, 어디가 더 끌려?",
            optionA = "감성",
            optionB = "핫플",
            leftAxis = "감성",
            rightAxis = "핫플",
            categoryTag = "로컬·감성"
        ),
        GameTemplate(
            id = 2,
            title = "풍경은 어떤 쪽이 더 좋아?",
            optionA = "자연",
            optionB = "도시",
            leftAxis = "자연",
            rightAxis = "도시",
            categoryTag = "자연·힐링"
        ),
        GameTemplate(
            id = 3,
            title = "방문할 장소의 느낌은?",
            optionA = "로컬",
            optionB = "유명",
            leftAxis = "로컬",
            rightAxis = "유명",
            categoryTag = "로컬·감성"
        ),
        GameTemplate(
            id = 4,
            title = "여행 일정 템포는 어떻게 할까?",
            optionA = "여유·힐링",
            optionB = "활발·탐방",
            leftAxis = "여유",
            rightAxis = "활발",
            categoryTag = "자연·힐링"
        ),
        GameTemplate(
            id = 5,
            title = "저녁·스팟 선택 기준은?",
            optionA = "로컬·골목",
            optionB = "핫플·랜드마크",
            leftAxis = "로컬",
            rightAxis = "핫플",
            categoryTag = "유명·핫플"
        )
    )
}