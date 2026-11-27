package com.example.data

// 밸런스 게임 질문 1개의 구조
data class BalanceGame(
    val gameId: String = "",        // 게임 문서 고유 ID
    val title: String = "",         // 질문 내용
    val optionA: String = "",       // A 선택지
    val optionB: String = "",       // B 선택지
    val countA: Int = 0,            // A를 선택한 인원수
    val countB: Int = 0,            // B를 선택한 인원수
    val order: Long = 0L            // 질문 순서
)