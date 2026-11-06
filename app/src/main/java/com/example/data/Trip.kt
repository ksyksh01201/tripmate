package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

/**
 * Firestore "trips" 컬렉션에 저장되는 여행 문서.
 *
 * Firestore가 문서를 객체로 바꿀 때(toObject) 빈 생성자가 필요해서 모든 필드에 기본값을 넣었습니다.
 * 날짜는 "yyyy-MM-dd" 문자열로 저장합니다. (예: 2026-08-01)
 */
data class Trip(
    val tripId: String = "",
    val title: String = "",                 // 여행 이름 (예: 부산 여행)
    val destination: String = "",           // 여행지 (예: 부산)
    val startDate: String = "",             // 시작일 (예: 2026-08-01)
    val endDate: String = "",               // 종료일 (예: 2026-08-03)
    val companionType: String = "",         // 동행자 유형 (CompanionType.name, 예: "FRIEND")
    val inviteCode: String = "",            // 초대 코드 (예: TRIP0101)
    val createdBy: String = "",             // 방장 UID
    val members: List<String> = emptyList(),// 참여자 UID 목록 (첫 번째 = 방장)
    val createdAt: Timestamp? = null        // 생성 시간
) {
    /** 현재 방 인원수. 여행룸 화면 분기에 사용합니다. (Firestore에는 저장하지 않음) */
    @get:Exclude
    val memberCount: Int
        get() = members.size

    /** true면 "초대 코드" 화면, false면 "여행 밸런스 게임" 화면을 보여줍니다. */
    @get:Exclude
    val isWaitingForMates: Boolean
        get() = members.size <= 1

    /** 저장된 문자열을 enum으로 바꿔 줍니다. 알 수 없는 값이면 null. */
    @get:Exclude
    val companionEnum: CompanionType?
        get() = CompanionType.fromName(companionType)

    /** 화면에 보여 줄 날짜 문구 (예: 2026.08.01 ~ 2026.08.03) */
    @get:Exclude
    val dateRangeText: String
        get() = "${startDate.replace('-', '.')} ~ ${endDate.replace('-', '.')}"
}

/** "누구와 함께하나요?" 선택 칩 6개 */
enum class CompanionType(val label: String) {
    FRIEND("친구"),
    ALONE("혼자"),
    FAMILY("가족"),
    COUPLE("연인"),
    COWORKER("동료"),
    OTHER("기타");

    companion object {
        fun fromName(name: String?): CompanionType? = entries.firstOrNull { it.name == name }
    }
}

/** "여행 도시" 드롭다운에 보여 줄 국내 여행 도시 목록 */
object TravelCities {
    val domestic: List<String> = listOf(
        "서울", "부산", "제주", "강릉", "여수",
        "경주", "전주", "속초", "인천", "대구",
        "광주", "대전", "통영", "포항", "춘천"
    )
}
