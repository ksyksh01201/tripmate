package com.example

import com.example.model.Expense
import com.example.model.Trip
import com.example.model.TripMember
import com.example.service.SettlementCalculator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testNewTripHasOnlyOneHostMember() {
        // [2. 여행 생성 직후 - 최초 참여자는 방장 1명만 존재]
        val trip = Trip(
            id = "trip_test_1",
            title = "서울 여행",
            destination = "서울",
            startDate = "10.20",
            endDate = "10.22",
            expectedCount = 4,
            inviteCode = "TRIP4A53",
            members = listOf(
                TripMember("user_me", "나 (방장)", isCurrentUser = true, isOwner = true, preferenceCompleted = false)
            )
        )

        assertEquals(1, trip.members.size)
        assertEquals("나 (방장)", trip.members[0].name)
        assertTrue(trip.members[0].isOwner)
        assertTrue(trip.members[0].isCurrentUser)
        assertEquals(4, trip.expectedCount)
    }

    @Test
    fun testUniqueInviteCodesAndDataSeparation() {
        val busanTrip = Trip(
            id = "trip_busan",
            title = "부산 우정여행",
            destination = "부산",
            startDate = "10.10",
            endDate = "10.12",
            expectedCount = 4,
            inviteCode = "TRIP4A53",
            members = listOf(
                TripMember("user_me", "나 (방장)", isCurrentUser = true, isOwner = true)
            )
        )

        val jejuTrip = Trip(
            id = "trip_jeju",
            title = "제주 여행",
            destination = "제주",
            startDate = "11.05",
            endDate = "11.08",
            expectedCount = 4,
            inviteCode = "TRIP8B21",
            members = listOf(
                TripMember("user_me", "나 (방장)", isCurrentUser = true, isOwner = true)
            )
        )

        val trips = listOf(busanTrip, jejuTrip)

        // 고유 초대코드 확인
        assertNotEquals(busanTrip.inviteCode, jejuTrip.inviteCode)
        assertNotEquals(busanTrip.id, jejuTrip.id)

        // 정상 코드 조회
        val foundBusan = trips.find { it.inviteCode.equals("TRIP4A53", ignoreCase = true) }
        assertNotNull(foundBusan)
        assertEquals("부산 우정여행", foundBusan?.title)

        // 잘못된 코드 조회 -> null (새로운 여행 생성 안 됨)
        val notFound = trips.find { it.inviteCode.equals("INVALID_CODE", ignoreCase = true) }
        assertNull(notFound)
    }

    @Test
    fun testTripExpenseIsolation() {
        // 1. 부산 우정여행 생성
        val busanMembers = listOf(
            TripMember("user_me", "나", isCurrentUser = true),
            TripMember("user_b", "사용자B")
        )
        val busanTripId = "trip_busan_1"
        val busanExpense1 = Expense(
            expenseId = "exp_1",
            tripId = busanTripId,
            title = "호텔",
            amount = 150000L,
            category = "숙박",
            payerId = "user_me",
            payerName = "나",
            participantIds = listOf("user_me", "user_b"),
            participantNames = listOf("나", "사용자B")
        )
        val busanExpense2 = Expense(
            expenseId = "exp_2",
            tripId = busanTripId,
            title = "식비",
            amount = 70000L,
            category = "식비",
            payerId = "user_b",
            payerName = "사용자B",
            participantIds = listOf("user_me", "user_b"),
            participantNames = listOf("나", "사용자B")
        )
        val busanTrip = Trip(
            id = busanTripId,
            title = "부산 우정여행",
            destination = "부산",
            startDate = "10.10",
            endDate = "10.12",
            expectedCount = 2,
            inviteCode = "BUSAN1",
            members = busanMembers,
            expenses = listOf(busanExpense1, busanExpense2)
        )

        // 부산 총 지출 = 220,000원
        val busanTotal = busanTrip.expenses.sumOf { it.amount }
        assertEquals(220000L, busanTotal)

        // 4. 새로운 제주 여행 생성
        val jejuMembers = listOf(
            TripMember("user_me", "나", isCurrentUser = true),
            TripMember("user_c", "사용자C")
        )
        val jejuTripId = "trip_jeju_1"
        val jejuExpense1 = Expense(
            expenseId = "exp_3",
            tripId = jejuTripId,
            title = "렌터카",
            amount = 100000L,
            category = "교통",
            payerId = "user_me",
            payerName = "나",
            participantIds = listOf("user_me", "user_c"),
            participantNames = listOf("나", "사용자C")
        )
        val jejuTrip = Trip(
            id = jejuTripId,
            title = "제주 여행",
            destination = "제주",
            startDate = "11.05",
            endDate = "11.08",
            expectedCount = 2,
            inviteCode = "JEJU1",
            members = jejuMembers,
            expenses = listOf(jejuExpense1)
        )

        // 제주 총 지출 = 100,000원
        val jejuTotal = jejuTrip.expenses.sumOf { it.amount }
        assertEquals(100000L, jejuTotal)

        // 6. 부산 여행 경비 확인 (부산 지출에 렌터카 미포함 검증)
        val busanExpenseTitles = busanTrip.expenses.map { it.title }
        assertTrue(busanExpenseTitles.contains("호텔"))
        assertTrue(busanExpenseTitles.contains("식비"))
        assertFalse(busanExpenseTitles.contains("렌터카"))

        // 7. 제주 여행 경비 확인 (제주 지출에 호텔, 식비 미포함 검증)
        val jejuExpenseTitles = jejuTrip.expenses.map { it.title }
        assertEquals(1, jejuExpenseTitles.size)
        assertTrue(jejuExpenseTitles.contains("렌터카"))

        // 카테고리별 막대그래프 분리 검증
        val busanCategoryTotals = SettlementCalculator.calculateCategoryTotals(busanTrip.expenses)
        val busanMap = busanCategoryTotals.associate { it.category to it.totalAmount }
        assertEquals(150000L, busanMap["숙박"])
        assertEquals(70000L, busanMap["식비"])
        assertEquals(0L, busanMap["교통"])

        val jejuCategoryTotals = SettlementCalculator.calculateCategoryTotals(jejuTrip.expenses)
        val jejuMap = jejuCategoryTotals.associate { it.category to it.totalAmount }
        assertEquals(100000L, jejuMap["교통"])
        assertEquals(0L, jejuMap["숙박"])
        assertEquals(0L, jejuMap["식비"])

        // 1/N 정산 결과 분리 검증
        val busanBalances = SettlementCalculator.calculateMemberBalances(busanMembers, busanTrip.expenses)
        val busanTransfers = SettlementCalculator.calculateSettlementTransfers(busanBalances)
        // 부산: 나(호텔 150000 냄, 부담 110000, net +40000), 사용자B(식비 70000 냄, 부담 110000, net -40000)
        // 사용자B -> 나 40,000원 송금
        assertEquals(1, busanTransfers.size)
        assertEquals("user_b", busanTransfers[0].fromUserId)
        assertEquals("user_me", busanTransfers[0].toUserId)
        assertEquals(40000L, busanTransfers[0].amount)

        val jejuBalances = SettlementCalculator.calculateMemberBalances(jejuMembers, jejuTrip.expenses)
        val jejuTransfers = SettlementCalculator.calculateSettlementTransfers(jejuBalances)
        // 제주: 나(렌터카 100000 냄, 부담 50000, net +50000), 사용자C(0 냄, 부담 50000, net -50000)
        // 사용자C -> 나 50,000원 송금
        assertEquals(1, jejuTransfers.size)
        assertEquals("user_c", jejuTransfers[0].fromUserId)
        assertEquals("user_me", jejuTransfers[0].toUserId)
        assertEquals(50000L, jejuTransfers[0].amount)
    }

    @Test
    fun testDynamicMatchRateCalculation() {
        // [4. 적합도 계산 - 그룹 점수의 평균]
        // 맛집 85, 사진 75, 휴식 70 -> (85 + 75 + 70) / 3 = 76.666... -> 약 77%
        val groupScores = mapOf("맛집" to 85, "사진" to 75, "휴식" to 70, "관광" to 65)

        val gwanganriTags = listOf("맛집", "사진", "휴식")
        val gwanganriMatch = com.example.model.PlaceDataProvider.calculateMatchRate(gwanganriTags, groupScores)
        assertEquals(77, gwanganriMatch)

        val haeundaeTags = listOf("관광", "맛집", "사진")
        val haeundaeMatch = com.example.model.PlaceDataProvider.calculateMatchRate(haeundaeTags, groupScores)
        assertEquals(75, haeundaeMatch)

        val gamcheonTags = listOf("사진", "관광")
        val gamcheonMatch = com.example.model.PlaceDataProvider.calculateMatchRate(gamcheonTags, groupScores)
        assertEquals(70, gamcheonMatch)

        // 그룹 취향 데이터가 없는 경우 -> 0%
        val emptyMatch = com.example.model.PlaceDataProvider.calculateMatchRate(gwanganriTags, emptyMap())
        assertEquals(0, emptyMatch)
    }

    @Test
    fun testPlaceRecommendationSortingByMatchRate() {
        val groupScores = mapOf("맛집" to 85, "사진" to 75, "휴식" to 70, "관광" to 65, "쇼핑" to 50)
        val places = com.example.model.PlaceDataProvider.getInitialPlaces("부산", groupScores)

        // 적합도가 높은 장소부터 정렬되어 있는지 확인
        for (i in 0 until places.size - 1) {
            assertTrue(places[i].matchRate >= places[i + 1].matchRate)
        }

        // 광안리(77%)가 해운대(75%)보다 앞서야 함
        val gwanganriIndex = places.indexOfFirst { it.name.contains("광안리") }
        val haeundaeIndex = places.indexOfFirst { it.name.contains("해운대") }
        assertTrue(gwanganriIndex < haeundaeIndex)
    }

    @Test
    fun testTripPlacesIsolationAndStepProgress() {
        val busanTrip = Trip(
            id = "trip_busan_1",
            title = "부산 우정여행",
            destination = "부산",
            startDate = "10.10",
            endDate = "10.12",
            expectedCount = 4,
            inviteCode = "TRIP4A53"
        )
        val jejuTrip = Trip(
            id = "trip_jeju_1",
            title = "제주 힐링여행",
            destination = "제주",
            startDate = "11.05",
            endDate = "11.08",
            expectedCount = 2,
            inviteCode = "TRIP8B21"
        )

        // 장소 선택 완료
        val selectedForBusan = listOf(
            com.example.model.Place("p_gwanganri", "광안리", listOf("맛집", "사진", "휴식"), "", 35.15, 129.11, 77, 0, isSelected = true),
            com.example.model.Place("p_haeundae", "해운대", listOf("관광", "맛집", "사진"), "", 35.15, 129.16, 75, 0, isSelected = true)
        )

        val updatedBusan = busanTrip.copy(
            placesSelected = true,
            selectedPlaces = selectedForBusan
        )

        // 부산 여행의 상태 및 selectedPlaces 확인
        assertTrue(updatedBusan.placesSelected)
        assertEquals(2, updatedBusan.selectedPlaces.size)
        assertEquals("광안리", updatedBusan.selectedPlaces[0].name)
        assertEquals("해운대", updatedBusan.selectedPlaces[1].name)

        // 제주 여행의 상태는 섞이지 않고 비어 있어야 함
        assertFalse(jejuTrip.placesSelected)
        assertTrue(jejuTrip.selectedPlaces.isEmpty())

        // 선택하지 않은 장소는 일정에 전달되지 않고, 여행 일수 기준 DAY가 생성됨
        val schedules = com.example.model.PlaceDataProvider.generateDays(
            startDate = updatedBusan.startDate,
            endDate = updatedBusan.endDate
        )
        assertEquals(3, schedules.size)
        assertEquals("10.10", schedules[0].dateStr)
        assertEquals("10.11", schedules[1].dateStr)
        assertEquals("10.12", schedules[2].dateStr)
        // 임의의 mock 일정이 자동으로 생성되지 않음
        assertTrue(schedules.all { it.items.isEmpty() })
    }

    @Test
    fun testDynamicDayGenerationFromDates() {
        // [2. 여행 날짜를 기준으로 DAY 생성]
        // 10.10 ~ 10.12 -> 3일 (10.10, 10.11, 10.12)
        val daysBusan = com.example.model.PlaceDataProvider.generateDays("10.10", "10.12")
        assertEquals(3, daysBusan.size)
        assertEquals(1, daysBusan[0].dayNumber)
        assertEquals("10.10", daysBusan[0].dateStr)
        assertEquals(2, daysBusan[1].dayNumber)
        assertEquals("10.11", daysBusan[1].dateStr)
        assertEquals(3, daysBusan[2].dayNumber)
        assertEquals("10.12", daysBusan[2].dateStr)

        // 11.05 ~ 11.06 -> 2일 (11.05, 11.06)
        val daysJeju = com.example.model.PlaceDataProvider.generateDays("11.05", "11.06")
        assertEquals(2, daysJeju.size)
        assertEquals("11.05", daysJeju[0].dateStr)
        assertEquals("11.06", daysJeju[1].dateStr)

        // 단일 날짜 여행 (10.10 ~ 10.10) -> 1일
        val singleDay = com.example.model.PlaceDataProvider.generateDays("10.10", "10.10")
        assertEquals(1, singleDay.size)
        assertEquals("10.10", singleDay[0].dateStr)
    }

    @Test
    fun testTimeSortingAndSchedulePlacement() {
        // [7. 시간순 정렬 & 11. 일정 데이터]
        val tripId = "trip_busan_cuj"
        val item1 = com.example.model.ScheduleItem(
            id = "s1",
            tripId = tripId,
            placeId = "p_gwanganri",
            placeName = "광안리",
            time = "18:00",
            date = "10.10"
        )
        val item2 = com.example.model.ScheduleItem(
            id = "s2",
            tripId = tripId,
            placeId = "p_haeundae",
            placeName = "해운대",
            time = "10:00",
            date = "10.10"
        )
        val item3 = com.example.model.ScheduleItem(
            id = "s3",
            tripId = tripId,
            placeId = "p_gamcheon",
            placeName = "감천문화마을",
            time = "14:00",
            date = "10.10"
        )

        // 18:00, 10:00, 14:00 순서로 입력했더라도 10:00, 14:00, 18:00 순으로 정렬
        val unsorted = listOf(item1, item2, item3)
        val sorted = unsorted.sortedBy { com.example.model.PlaceDataProvider.timeToMinutes(it.time) }

        assertEquals("해운대", sorted[0].placeName)
        assertEquals("10:00", sorted[0].time)
        assertEquals("감천문화마을", sorted[1].placeName)
        assertEquals("14:00", sorted[1].time)
        assertEquals("광안리", sorted[2].placeName)
        assertEquals("18:00", sorted[2].time)
    }

    @Test
    fun testTripScheduleIsolationAndConfirmation() {
        // [12. 여행별 일정 분리 & 13. 일정 확정]
        var busanTrip = Trip(
            id = "trip_busan_final",
            title = "부산 우정여행",
            destination = "부산",
            startDate = "10.10",
            endDate = "10.12",
            expectedCount = 3,
            inviteCode = "BUSAN123",
            selectedPlaces = listOf(
                com.example.model.Place("p_haeundae", "해운대", listOf("관광"), "", 35.15, 129.16),
                com.example.model.Place("p_gamcheon", "감천문화마을", listOf("사진"), "", 35.09, 129.01),
                com.example.model.Place("p_gwanganri", "광안리", listOf("맛집"), "", 35.15, 129.11)
            )
        )

        val jejuTrip = Trip(
            id = "trip_jeju_final",
            title = "제주 힐링여행",
            destination = "제주",
            startDate = "11.05",
            endDate = "11.06",
            expectedCount = 2,
            inviteCode = "JEJU999"
        )

        val busanDays = com.example.model.PlaceDataProvider.generateDays("10.10", "10.12").toMutableList()
        // DAY 1에 해운대(10:00), 감천문화마을(14:00)
        busanDays[0] = busanDays[0].copy(
            items = listOf(
                com.example.model.ScheduleItem("s1", busanTrip.id, "p_haeundae", "해운대", "관광", "10:00", date = "10.10"),
                com.example.model.ScheduleItem("s2", busanTrip.id, "p_gamcheon", "감천문화마을", "사진", "14:00", date = "10.10")
            )
        )
        // DAY 2에 광안리(18:00)
        busanDays[1] = busanDays[1].copy(
            items = listOf(
                com.example.model.ScheduleItem("s3", busanTrip.id, "p_gwanganri", "광안리", "맛집", "18:00", date = "10.11")
            )
        )

        // 일정 확정
        busanTrip = busanTrip.copy(
            scheduleConfirmed = true,
            status = "✓ 여행 준비 완료",
            daySchedules = busanDays
        )

        assertTrue(busanTrip.scheduleConfirmed)
        assertEquals("✓ 여행 준비 완료", busanTrip.status)
        assertEquals(2, busanTrip.daySchedules[0].items.size)
        assertEquals(1, busanTrip.daySchedules[1].items.size)
        assertEquals(0, busanTrip.daySchedules[2].items.size)

        // 제주 여행 일정은 분리되어 비어 있음
        assertFalse(jejuTrip.scheduleConfirmed)
        assertTrue(jejuTrip.daySchedules.isEmpty())
    }

    @Test
    fun testTrollAndChunguTripScheduleIsolation() {
        // [11. 여행별 일정 완전 분리]
        // troll: 10.15 ~ 10.17
        // 10.15 더베이101, 흰여울문화마을 / 10.16 국제시장
        val trollDays = com.example.model.PlaceDataProvider.generateDays("10.15", "10.17").toMutableList()
        trollDays[0] = trollDays[0].copy(
            items = listOf(
                com.example.model.ScheduleItem("item_troll_1", "trip_troll", "p_thebay", "더베이101", "사진", "10:00", date = "10.15"),
                com.example.model.ScheduleItem("item_troll_2", "trip_troll", "p_huinnyeoul", "흰여울문화마을", "사진", "12:00", date = "10.15")
            )
        )
        trollDays[1] = trollDays[1].copy(
            items = listOf(
                com.example.model.ScheduleItem("item_troll_3", "trip_troll", "p_gukje", "국제시장", "맛집", "10:00", date = "10.16")
            )
        )

        val trollTrip = Trip(
            id = "trip_troll",
            title = "troll",
            destination = "부산",
            startDate = "10.15",
            endDate = "10.17",
            expectedCount = 2,
            inviteCode = "TRIP_TROLL",
            scheduleConfirmed = true,
            daySchedules = trollDays
        )

        // chungu: 10.11 ~ 10.13
        // 10.11 광안리, 더베이101, 흰여울문화마을 / 10.12 국제시장, 해운대
        val chunguDays = com.example.model.PlaceDataProvider.generateDays("10.11", "10.13").toMutableList()
        chunguDays[0] = chunguDays[0].copy(
            items = listOf(
                com.example.model.ScheduleItem("item_chungu_1", "trip_chungu", "p_gwanganri", "광안리", "맛집", "10:00", date = "10.11"),
                com.example.model.ScheduleItem("item_chungu_2", "trip_chungu", "p_thebay", "더베이101", "사진", "12:00", date = "10.11"),
                com.example.model.ScheduleItem("item_chungu_3", "trip_chungu", "p_huinnyeoul", "흰여울문화마을", "휴식", "13:00", date = "10.11")
            )
        )
        chunguDays[1] = chunguDays[1].copy(
            items = listOf(
                com.example.model.ScheduleItem("item_chungu_4", "trip_chungu", "p_gukje", "국제시장", "맛집", "09:00", date = "10.12"),
                com.example.model.ScheduleItem("item_chungu_5", "trip_chungu", "p_haeundae", "해운대", "관광", "10:00", date = "10.12")
            )
        )

        val chunguTrip = Trip(
            id = "trip_chungu",
            title = "chungu",
            destination = "부산",
            startDate = "10.11",
            endDate = "10.13",
            expectedCount = 3,
            inviteCode = "TRIP_CHUNGU",
            scheduleConfirmed = true,
            daySchedules = chunguDays
        )

        val allTrips = listOf(trollTrip, chunguTrip)

        // 1. troll 선택 시: troll의 tripId 일정만 가져옴
        val selectedScheduleTripId1 = "trip_troll"
        val activeTrip1 = allTrips.find { it.id == selectedScheduleTripId1 }!!
        val trollScheduleItems = activeTrip1.daySchedules.flatMap { it.items }.filter { it.tripId == selectedScheduleTripId1 }
        
        assertEquals(3, trollScheduleItems.size)
        assertTrue(trollScheduleItems.all { it.tripId == "trip_troll" })
        assertEquals("더베이101", trollScheduleItems[0].placeName)
        assertEquals("흰여울문화마을", trollScheduleItems[1].placeName)
        assertEquals("국제시장", trollScheduleItems[2].placeName)
        // chungu에만 있는 광안리, 해운대가 troll에 절대 포함되지 않음
        assertFalse(trollScheduleItems.any { it.placeName == "광안리" })
        assertFalse(trollScheduleItems.any { it.placeName == "해운대" })

        // 2. chungu 선택 시: chungu의 tripId 일정만 가져옴
        val selectedScheduleTripId2 = "trip_chungu"
        val activeTrip2 = allTrips.find { it.id == selectedScheduleTripId2 }!!
        val chunguScheduleItems = activeTrip2.daySchedules.flatMap { it.items }.filter { it.tripId == selectedScheduleTripId2 }

        assertEquals(5, chunguScheduleItems.size)
        assertTrue(chunguScheduleItems.all { it.tripId == "trip_chungu" })
        assertEquals("광안리", chunguScheduleItems[0].placeName)
        assertEquals("더베이101", chunguScheduleItems[1].placeName)
        assertEquals("흰여울문화마을", chunguScheduleItems[2].placeName)
        assertEquals("국제시장", chunguScheduleItems[3].placeName)
        assertEquals("해운대", chunguScheduleItems[4].placeName)

        // 3. 지도 보기 데이터 전달 확인 (DAY 1 기준)
        val trollDay1MapPlaces = trollTrip.daySchedules[0].items.map { it.placeName }
        assertEquals(listOf("더베이101", "흰여울문화마을"), trollDay1MapPlaces)

        val chunguDay1MapPlaces = chunguTrip.daySchedules[0].items.map { it.placeName }
        assertEquals(listOf("광안리", "더베이101", "흰여울문화마을"), chunguDay1MapPlaces)

        // 4. 여행 개수에 따른 하단 네비게이션 일정 분기 로직 (Requirement 9 & 10)
        // 0개: SCHEDULE_TRIP_SELECT 빈 상태
        val emptyTrips = emptyList<Trip>()
        val destinationScreen0 = if (emptyTrips.isEmpty()) "SCHEDULE_TRIP_SELECT" else if (emptyTrips.size == 1) "FULL_SCHEDULE" else "SCHEDULE_TRIP_SELECT"
        assertEquals("SCHEDULE_TRIP_SELECT", destinationScreen0)

        // 1개: 해당 여행 일정 바로 이동
        val singleTripList = listOf(trollTrip)
        val destinationScreen1 = if (singleTripList.isEmpty()) "SCHEDULE_TRIP_SELECT" else if (singleTripList.size == 1) "FULL_SCHEDULE" else "SCHEDULE_TRIP_SELECT"
        assertEquals("FULL_SCHEDULE", destinationScreen1)

        // 2개 이상: 여행 선택 화면 필수 이동
        val destinationScreen2 = if (allTrips.isEmpty()) "SCHEDULE_TRIP_SELECT" else if (allTrips.size == 1) "FULL_SCHEDULE" else "SCHEDULE_TRIP_SELECT"
        assertEquals("SCHEDULE_TRIP_SELECT", destinationScreen2)
    }

    @Test
    fun testDayTabFiltering_chinguAndPi() {
        // [최종 테스트 검증]
        // chingu 여행: 10.11 ~ 10.13
        // 10.11: 10:00 더베이101, 11:00 흰여울문화마을, 12:00 국제시장
        // 10.12: 12:00 해운대
        // 10.13: 일정 없음
        val chinguDays = com.example.model.PlaceDataProvider.generateDays("10.11", "10.13").toMutableList()
        chinguDays[0] = chinguDays[0].copy(
            items = listOf(
                com.example.model.ScheduleItem("c1", "trip_chingu", "p_thebay", "더베이101", "사진", "10:00", date = "10.11"),
                com.example.model.ScheduleItem("c2", "trip_chingu", "p_huin", "흰여울문화마을", "사진", "11:00", date = "10.11"),
                com.example.model.ScheduleItem("c3", "trip_chingu", "p_gukje", "국제시장", "맛집", "12:00", date = "10.11")
            )
        )
        chinguDays[1] = chinguDays[1].copy(
            items = listOf(
                com.example.model.ScheduleItem("c4", "trip_chingu", "p_haeundae", "해운대", "관광", "12:00", date = "10.12")
            )
        )
        // DAY 3 is empty
        val chinguTrip = Trip(
            id = "trip_chingu",
            title = "chingu",
            destination = "부산",
            startDate = "10.11",
            endDate = "10.13",
            expectedCount = 2,
            inviteCode = "CHINGU123",
            scheduleConfirmed = true,
            daySchedules = chinguDays
        )

        // pi 여행: 10.13 ~ 10.14
        val piTrip = Trip(
            id = "trip_pi",
            title = "pi",
            destination = "부산",
            startDate = "10.13",
            endDate = "10.14",
            expectedCount = 2,
            inviteCode = "PI123",
            scheduleConfirmed = true
        )

        // 1. chingu의 DAY는 3개 (10.11, 10.12, 10.13)
        assertEquals(3, chinguTrip.daySchedules.size)
        assertEquals("10.11", chinguTrip.daySchedules[0].dateStr)
        assertEquals("10.12", chinguTrip.daySchedules[1].dateStr)
        assertEquals("10.13", chinguTrip.daySchedules[2].dateStr)

        // 2. DAY 1 선택(selectedDay = 0) 시 -> 10.11 일정 3개만 반환
        val day1 = chinguTrip.daySchedules[0]
        assertEquals(3, day1.items.size)
        assertEquals(listOf("더베이101", "흰여울문화마을", "국제시장"), day1.items.map { it.placeName })
        assertFalse(day1.items.any { it.placeName == "해운대" })

        // 3. DAY 2 선택(selectedDay = 1) 시 -> 10.12 해운대 1개만 반환
        val day2 = chinguTrip.daySchedules[1]
        assertEquals(1, day2.items.size)
        assertEquals("해운대", day2.items[0].placeName)
        assertFalse(day2.items.any { it.placeName == "더베이101" })

        // 4. DAY 3 선택(selectedDay = 2) 시 -> 등록된 일정 없음 (0개)
        val day3 = chinguTrip.daySchedules[2]
        assertTrue(day3.items.isEmpty())

        // 5. pi 여행의 DAY는 2개만 생성 (10.13, 10.14) -> DAY 3 표시 안 됨
        val piDays = com.example.model.PlaceDataProvider.generateDays(piTrip.startDate, piTrip.endDate)
        assertEquals(2, piDays.size)
        assertEquals("10.13", piDays[0].dateStr)
        assertEquals("10.14", piDays[1].dateStr)

        // 6. 지도 보기: DAY 1에서는 3개 장소만, DAY 2에서는 1개 장소만 전달
        val mapPlacesDay1 = day1.items.map { it.placeName }
        assertEquals(listOf("더베이101", "흰여울문화마을", "국제시장"), mapPlacesDay1)

        val mapPlacesDay2 = day2.items.map { it.placeName }
        assertEquals(listOf("해운대"), mapPlacesDay2)
    }

    @Test
    fun testSettlementCalculation_Test1() {
        // [최종 테스트 1]
        // 참여자: 나, 사용자B
        val memberMe = TripMember(id = "user_me", name = "나", isCurrentUser = true)
        val memberB = TripMember(id = "user_b", name = "사용자B", isCurrentUser = false)
        val members = listOf(memberMe, memberB)

        // hotel 100,000원 결제자=나, 부담=나+사용자B
        val exp1 = Expense(
            expenseId = "e1",
            tripId = "trip_chingu",
            title = "hotel",
            amount = 100000L,
            category = "숙박",
            payerId = "user_me",
            payerName = "나",
            participantIds = listOf("user_me", "user_b"),
            participantNames = listOf("나", "사용자B")
        )
        // hamburger 20,000원 결제자=사용자B, 부담=나+사용자B
        val exp2 = Expense(
            expenseId = "e2",
            tripId = "trip_chingu",
            title = "hamburger",
            amount = 20000L,
            category = "식비",
            payerId = "user_b",
            payerName = "사용자B",
            participantIds = listOf("user_me", "user_b"),
            participantNames = listOf("나", "사용자B")
        )
        // train 40,000원 결제자=사용자B, 부담=나+사용자B
        val exp3 = Expense(
            expenseId = "e3",
            tripId = "trip_chingu",
            title = "train",
            amount = 40000L,
            category = "교통",
            payerId = "user_b",
            payerName = "사용자B",
            participantIds = listOf("user_me", "user_b"),
            participantNames = listOf("나", "사용자B")
        )
        val expenses = listOf(exp1, exp2, exp3)

        // 총 지출: 160,000원
        val total = expenses.sumOf { it.amount }
        assertEquals(160000L, total)

        val balances = com.example.service.SettlementCalculator.calculateMemberBalances(members, expenses)
        val meBalance = balances.find { it.userId == "user_me" }!!
        val bBalance = balances.find { it.userId == "user_b" }!!

        // 나: 결제 100,000원 / 부담 80,000원 / 잔액 +20,000원 (받을 돈 20,000원)
        assertEquals(100000L, meBalance.paidAmount)
        assertEquals(80000L, meBalance.owedAmount)
        assertEquals(20000L, meBalance.netBalance)

        // 사용자B: 결제 60,000원 / 부담 80,000원 / 잔액 -20,000원 (보낼 돈 20,000원)
        assertEquals(60000L, bBalance.paidAmount)
        assertEquals(80000L, bBalance.owedAmount)
        assertEquals(-20000L, bBalance.netBalance)

        // 최종 정산: 사용자B -> 나 20,000원
        val transfers = com.example.service.SettlementCalculator.calculateSettlementTransfers(balances)
        assertEquals(1, transfers.size)
        val transfer = transfers[0]
        assertEquals("user_b", transfer.fromUserId)
        assertEquals("user_me", transfer.toUserId)
        assertEquals(20000L, transfer.amount)

        // 카테고리별 지출 확인
        val catTotals = com.example.service.SettlementCalculator.calculateCategoryTotals(expenses)
        assertEquals(20000L, catTotals.find { it.category == "식비" }?.totalAmount)
        assertEquals(100000L, catTotals.find { it.category == "숙박" }?.totalAmount)
        assertEquals(40000L, catTotals.find { it.category == "교통" }?.totalAmount)
        assertEquals(0L, catTotals.find { it.category == "관광" }?.totalAmount)
    }

    @Test
    fun testSettlementCalculation_Test2() {
        // [최종 테스트 2]
        // hotel 100,000원 (결제자: 나)
        // hamburger 20,000원 (결제자: 나)
        // train 40,000원 (결제자: 나)
        // 두 명 함께 부담
        val memberMe = TripMember(id = "user_me", name = "나", isCurrentUser = true)
        val memberB = TripMember(id = "user_b", name = "사용자B", isCurrentUser = false)
        val members = listOf(memberMe, memberB)

        val expenses = listOf(
            Expense("e1", "trip_chingu", "hotel", 100000L, "숙박", "user_me", "나", listOf("user_me", "user_b"), listOf("나", "사용자B")),
            Expense("e2", "trip_chingu", "hamburger", 20000L, "식비", "user_me", "나", listOf("user_me", "user_b"), listOf("나", "사용자B")),
            Expense("e3", "trip_chingu", "train", 40000L, "교통", "user_me", "나", listOf("user_me", "user_b"), listOf("나", "사용자B"))
        )

        val balances = com.example.service.SettlementCalculator.calculateMemberBalances(members, expenses)
        val meBalance = balances.find { it.userId == "user_me" }!!
        val bBalance = balances.find { it.userId == "user_b" }!!

        // 나: 결제 160,000원 / 부담 80,000원 / 받을 돈 80,000원
        assertEquals(160000L, meBalance.paidAmount)
        assertEquals(80000L, meBalance.owedAmount)
        assertEquals(80000L, meBalance.netBalance)

        // 사용자B: 결제 0원 / 부담 80,000원 / 보낼 돈 80,000원
        assertEquals(0L, bBalance.paidAmount)
        assertEquals(80000L, bBalance.owedAmount)
        assertEquals(-80000L, bBalance.netBalance)

        // 최종 정산: 사용자B -> 나 80,000원
        val transfers = com.example.service.SettlementCalculator.calculateSettlementTransfers(balances)
        assertEquals(1, transfers.size)
        assertEquals("user_b", transfers[0].fromUserId)
        assertEquals("user_me", transfers[0].toUserId)
        assertEquals(80000L, transfers[0].amount)
    }

    @Test
    fun testSettlementCalculation_PartialSharingAndBalanced() {
        // [11. 일부 사람만 부담하는 지출]
        val memberMe = TripMember(id = "user_me", name = "나", isCurrentUser = true)
        val memberB = TripMember(id = "user_b", name = "사용자B", isCurrentUser = false)
        val memberC = TripMember(id = "user_c", name = "사용자C", isCurrentUser = false)
        val members = listOf(memberMe, memberB, memberC)

        // 택시 30,000원 결제자: 나, 부담 참여자: 나, 사용자B (사용자C 제외)
        val expTaxi = Expense(
            expenseId = "e_taxi",
            tripId = "trip_chingu",
            title = "택시",
            amount = 30000L,
            category = "교통",
            payerId = "user_me",
            payerName = "나",
            participantIds = listOf("user_me", "user_b"),
            participantNames = listOf("나", "사용자B")
        )

        val balances = com.example.service.SettlementCalculator.calculateMemberBalances(members, listOf(expTaxi))
        val meBalance = balances.find { it.userId == "user_me" }!!
        val bBalance = balances.find { it.userId == "user_b" }!!
        val cBalance = balances.find { it.userId == "user_c" }!!

        assertEquals(15000L, meBalance.owedAmount)
        assertEquals(15000L, bBalance.owedAmount)
        assertEquals(0L, cBalance.owedAmount) // participantIds에 없는 C는 0원 부담

        // [10. 이미 정확히 정산된 경우]
        // 나 80,000원 결제 및 80,000원 부담, 사용자B 80,000원 결제 및 80,000원 부담
        val balancedExpenses = listOf(
            Expense("e_b1", "trip_chingu", "식사1", 80000L, "식비", "user_me", "나", listOf("user_me"), listOf("나")),
            Expense("e_b2", "trip_chingu", "식사2", 80000L, "식비", "user_b", "사용자B", listOf("user_b"), listOf("사용자B"))
        )
        val balancedMembers = listOf(memberMe, memberB)
        val balancesBalanced = com.example.service.SettlementCalculator.calculateMemberBalances(balancedMembers, balancedExpenses)
        val transfersBalanced = com.example.service.SettlementCalculator.calculateSettlementTransfers(balancesBalanced)

        // 정산할 금액이 없음 -> transfers는 비어 있어야 함
        assertTrue(transfersBalanced.isEmpty())
    }

    @Test
    fun testBalanceQuestionTagsMapping() {
        // [2. 질문별 취향 태그 연결 검증]
        val questions = com.example.model.PreferenceQuestions.questions
        assertEquals(10, questions.size)

        // Q1: 맛집 탐방 (맛집) vs 관광 명소 (관광)
        assertEquals("맛집", questions[0].optionA.tag)
        assertEquals("관광", questions[0].optionB.tag)

        // Q2: 계획적인 여행 (계획) vs 즉흥적인 여행 (즉흥)
        assertEquals("계획", questions[1].optionA.tag)
        assertEquals("즉흥", questions[1].optionB.tag)

        // Q4: 유명 관광지 (관광) vs 로컬 장소 (로컬)
        assertEquals("관광", questions[3].optionA.tag)
        assertEquals("로컬", questions[3].optionB.tag)

        // Q5: 액티비티 vs 휴식
        assertEquals("액티비티", questions[4].optionA.tag)
        assertEquals("휴식", questions[4].optionB.tag)

        // Q6: 사진 중심 vs 맛집 중심
        assertEquals("사진", questions[5].optionA.tag)
        assertEquals("맛집", questions[5].optionB.tag)

        // Q8: 가성비 vs 프리미엄
        assertEquals("가성비", questions[7].optionA.tag)
        assertEquals("프리미엄", questions[7].optionB.tag)

        // Q9: 도시 vs 자연
        assertEquals("도시", questions[8].optionA.tag)
        assertEquals("자연", questions[8].optionB.tag)

        // Q10: 쇼핑 vs 체험
        assertEquals("쇼핑", questions[9].optionA.tag)
        assertEquals("체험", questions[9].optionB.tag)
    }

    @Test
    fun testPersonalPreferenceCalculation_75PercentTourAnd67PercentPhoto() {
        // [1. Mock 퍼센트 제거 & 3. 개인 취향 계산]
        // 관광 관련 기회 4번 중 3번 선택 -> 관광 = 75%
        // 사진 관련 기회 3번 중 2번 선택 -> 사진 = 67%
        // Q1: B (관광 명소: 관광, 사진) -> 관광 1, 사진 1
        // Q2: A (계획)
        // Q3: A (아침부터 알차게: 관광) -> 관광 2
        // Q4: A (유명 관광지: 관광, 사진) -> 관광 3, 사진 2
        // Q5: B (휴식)
        // Q6: B (맛집 중심) -> 사진 선택 안 함 (2/3 유지)
        // Q7: B (적은 장소를 여유롭게: 휴식) -> 관광 선택 안 함 (3/4 유지)
        // Q8: A (가성비)
        // Q9: A (도시)
        // Q10: A (쇼핑)
        val answers = mapOf(
            1 to false, // optionB: 관광 명소 (관광, 사진)
            2 to true,  // optionA: 계획적인 여행
            3 to true,  // optionA: 아침부터 알차게 (관광)
            4 to true,  // optionA: 유명 관광지 (관광, 사진)
            5 to false, // optionB: 휴식
            6 to false, // optionB: 맛집 중심
            7 to false, // optionB: 적은 장소를 여유롭게 (휴식)
            8 to true,  // optionA: 가성비
            9 to true,  // optionA: 도시
            10 to true  // optionA: 쇼핑
        )

        val result = com.example.model.PreferenceQuestions.calculatePersonalResult(answers)

        // 관광: 4번 중 3번 선택 -> 정확히 75%
        val tourScore = result.categoryScores.find { it.category == "관광" }
        assertNotNull(tourScore)
        assertEquals(75, tourScore?.percentage)

        // 사진: 3번 중 2번 선택 -> 66.666% -> 반올림 67%
        val photoScore = result.categoryScores.find { it.category == "사진" }
        assertNotNull(photoScore)
        assertEquals(67, photoScore?.percentage)

        // 상위 3개 취향에 포함 확인
        assertEquals(3, result.topTraits.size)
        assertTrue(result.topTraits.contains("관광"))
        assertTrue(result.topTraits.contains("사진"))
    }

    @Test
    fun testPersonalResultTitleGeneration() {
        // [4. 개인 결과 대표 여행 성향 문구]
        // 관광 + 사진이 상위 취향인 경우
        val answers = mapOf(
            1 to false, // 관광, 사진
            2 to true,
            3 to true,  // 관광
            4 to true,  // 관광, 사진
            5 to false,
            6 to true,  // 사진
            7 to true,  // 관광
            8 to true,
            9 to true,
            10 to true
        )
        val result = com.example.model.PreferenceQuestions.calculatePersonalResult(answers)
        assertEquals("관광과 사진을 좋아하는 여행자", result.title)
    }

    @Test
    fun testGroupPreferenceCalculation_AverageScores() {
        // [5. 그룹 취향 계산 - 사용자별 취향 점수 평균]
        // 예: 나: 관광 80, 사용자B: 관광 60, 사용자C: 관광 70 -> 그룹 관광 = 70%
        val memberMe = TripMember(
            id = "user_me",
            name = "나",
            preferenceCompleted = true,
            preferenceResult = com.example.model.PersonalPreferenceResult(
                title = "관광과 사진을 좋아하는 여행자",
                description = "",
                topTraits = listOf("관광", "사진", "맛집"),
                categoryScores = listOf(
                    com.example.model.CategoryScore("관광", 80),
                    com.example.model.CategoryScore("사진", 75),
                    com.example.model.CategoryScore("맛집", 60)
                )
            )
        )

        val memberB = TripMember(
            id = "user_b",
            name = "사용자B",
            preferenceCompleted = true,
            preferenceResult = com.example.model.PersonalPreferenceResult(
                title = "관광과 사진을 좋아하는 여행자",
                description = "",
                topTraits = listOf("관광", "사진", "맛집"),
                categoryScores = listOf(
                    com.example.model.CategoryScore("관광", 60),
                    com.example.model.CategoryScore("사진", 65),
                    com.example.model.CategoryScore("맛집", 70)
                )
            )
        )

        val memberC = TripMember(
            id = "user_c",
            name = "사용자C",
            preferenceCompleted = true,
            preferenceResult = com.example.model.PersonalPreferenceResult(
                title = "관광과 맛집을 좋아하는 여행자",
                description = "",
                topTraits = listOf("관광", "맛집", "사진"),
                categoryScores = listOf(
                    com.example.model.CategoryScore("관광", 70),
                    com.example.model.CategoryScore("사진", 70),
                    com.example.model.CategoryScore("맛집", 65)
                )
            )
        )

        val members = listOf(memberMe, memberB, memberC)
        val groupResult = com.example.model.PreferenceQuestions.calculateGroupResult(members)

        // [6. 참여 인원] N = 3
        assertEquals(3, groupResult.completedMemberCount)

        // 관광: (80 + 60 + 70) / 3 = 70%
        val groupTour = groupResult.topCategories.find { it.category == "관광" }
        assertEquals(70, groupTour?.percentage)

        // 사진: (75 + 65 + 70) / 3 = 70%
        val groupPhoto = groupResult.topCategories.find { it.category == "사진" }
        assertEquals(70, groupPhoto?.percentage)

        // 맛집: (60 + 70 + 65) / 3 = 65%
        val groupFood = groupResult.topCategories.find { it.category == "맛집" }
        assertEquals(65, groupFood?.percentage)
    }

    @Test
    fun testGroupPreference_SingleMemberAndMultipleMembers() {
        // [7. 혼자만 완료한 경우]
        val memberMe = TripMember(
            id = "user_me",
            name = "나",
            preferenceCompleted = true,
            preferenceResult = com.example.model.PersonalPreferenceResult(
                title = "관광과 사진을 좋아하는 여행자",
                description = "",
                topTraits = listOf("관광", "사진", "맛집"),
                categoryScores = listOf(
                    com.example.model.CategoryScore("관광", 75),
                    com.example.model.CategoryScore("사진", 67),
                    com.example.model.CategoryScore("맛집", 50)
                )
            )
        )

        val singleResult = com.example.model.PreferenceQuestions.calculateGroupResult(listOf(memberMe))
        // 참여 인원 = 1명
        assertEquals(1, singleResult.completedMemberCount)
        // 나 혼자이므로 의견 갈림 없음
        assertTrue(singleResult.conflictList.isEmpty())
        // 그룹 관광 점수 = 75%
        assertEquals(75, singleResult.topCategories.find { it.category == "관광" }?.percentage)
    }

    @Test
    fun testGroupPreferenceConflicts_PlanVsSpontaneous() {
        // [9. 의견이 갈리는 항목]
        // 계획 vs 즉흥: 나(계획: true), 사용자B(계획: true), 사용자C(즉흥: false)
        // 계획 2 vs 즉흥 1
        val memberMe = TripMember(
            id = "user_me",
            name = "나",
            preferenceCompleted = true,
            preferenceAnswers = mapOf(2 to true), // 계획
            preferenceResult = com.example.model.PersonalPreferenceResult(
                title = "", description = "", topTraits = listOf("계획"),
                categoryScores = listOf(com.example.model.CategoryScore("계획", 100))
            )
        )

        val memberB = TripMember(
            id = "user_b",
            name = "사용자B",
            preferenceCompleted = true,
            preferenceAnswers = mapOf(2 to true), // 계획
            preferenceResult = com.example.model.PersonalPreferenceResult(
                title = "", description = "", topTraits = listOf("계획"),
                categoryScores = listOf(com.example.model.CategoryScore("계획", 100))
            )
        )

        val memberC = TripMember(
            id = "user_c",
            name = "사용자C",
            preferenceCompleted = true,
            preferenceAnswers = mapOf(2 to false), // 즉흥
            preferenceResult = com.example.model.PersonalPreferenceResult(
                title = "", description = "", topTraits = listOf("즉흥"),
                categoryScores = listOf(com.example.model.CategoryScore("즉흥", 100))
            )
        )

        val groupResult = com.example.model.PreferenceQuestions.calculateGroupResult(listOf(memberMe, memberB, memberC))

        val planConflict = groupResult.conflictList.find { it.optionA == "계획" && it.optionB == "즉흥" }
        assertNotNull(planConflict)
        assertEquals("계획 2 vs 즉흥 1", planConflict?.ratioText)
        assertEquals("계획적인 일정을 선호하지만 유연한 시간도 필요해요.", planConflict?.description)
    }
}
