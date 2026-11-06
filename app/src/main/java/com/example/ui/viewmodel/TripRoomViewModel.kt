package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Trip
import com.example.data.TripRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** 여행룸 화면 상태 */
sealed interface TripRoomUiState {
    data object Loading : TripRoomUiState
    data object NotFound : TripRoomUiState
    data class Error(val message: String) : TripRoomUiState

    /**
     * members.size == 1 → 초대 코드 화면 (trip.isWaitingForMates == true)
     * members.size >= 2 → 여행 밸런스 게임 화면
     */
    data class Loaded(val trip: Trip, val myUid: String?) : TripRoomUiState
}

/**
 * 여행룸 화면용 ViewModel.
 * getTripDetail()로 Firestore 문서를 실시간 관찰하므로,
 * 누군가 초대 코드로 들어오면 uiState가 즉시 바뀌고 화면도 자동으로 전환됩니다.
 */
class TripRoomViewModel(tripId: String) : ViewModel() {

    private val repository: TripRepository? = runCatching { TripRepository() }.getOrNull()

    val uiState: StateFlow<TripRoomUiState> =
        (repository?.getTripDetail(tripId)
            ?.map { trip ->
                if (trip == null) TripRoomUiState.NotFound
                else TripRoomUiState.Loaded(trip, repository?.currentUid)
            }
            ?: flowOf(TripRoomUiState.Error("Firebase 설정이 없어요. google-services.json을 확인해 주세요.")))
            .catch { e ->
                emit(TripRoomUiState.Error("여행 정보를 불러오지 못했어요. (${e.message})"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TripRoomUiState.Loading
            )
}
