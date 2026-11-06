package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CompanionType
import com.example.data.Trip
import com.example.data.TripException
import com.example.data.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 홈 화면 + 새 여행 만들기 / 초대 코드 참여 팝업의 상태 */
data class HomeUiState(
    val isSubmitting: Boolean = false,      // 생성/참여 요청 중이면 true (버튼 로딩 표시)
    val errorMessage: String? = null,       // 팝업에 보여 줄 오류 문구
    val openTripId: String? = null          // 값이 생기면 해당 여행룸으로 이동
)

class HomeViewModel : ViewModel() {

    // Firebase 설정(google-services.json)이 없으면 여기서 실패하므로, 앱이 꺼지지 않게 감쌉니다.
    private val repository: TripRepository? = runCatching { TripRepository() }.getOrNull()

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /** 내 여행 목록. null = 아직 불러오는 중 */
    val myTrips: StateFlow<List<Trip>?> =
        (repository?.getMyTrips() ?: emptyFlow())
            .map<List<Trip>, List<Trip>?> { it }
            .catch { e ->
                _uiState.update { it.copy(errorMessage = e.toUserMessage()) }
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = if (repository == null) emptyList() else null
            )

    fun createTrip(
        destination: String,
        startDate: String,
        endDate: String,
        companionType: CompanionType
    ) = submit {
        repository!!.createTrip(
            title = "$destination 여행",
            destination = destination,
            startDate = startDate,
            endDate = endDate,
            companionType = companionType
        )
    }

    fun joinTrip(inviteCode: String) = submit {
        repository!!.joinTripWithCode(inviteCode)
    }

    /** 여행룸으로 이동한 뒤 호출해서 이동 신호를 지웁니다. */
    fun onTripOpened() = _uiState.update { it.copy(openTripId = null) }

    /** 팝업을 닫거나 다시 입력할 때 오류 문구를 지웁니다. */
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun submit(block: suspend () -> Trip) {
        if (_uiState.value.isSubmitting) return
        if (repository == null) {
            _uiState.update { it.copy(errorMessage = FIREBASE_NOT_READY) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                val trip = block()
                _uiState.update { it.copy(isSubmitting = false, openTripId = trip.tripId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSubmitting = false, errorMessage = e.toUserMessage()) }
            }
        }
    }

    private fun Throwable.toUserMessage(): String = when (this) {
        is TripException -> message ?: DEFAULT_ERROR
        else -> "$DEFAULT_ERROR (${message ?: javaClass.simpleName})"
    }

    private companion object {
        const val DEFAULT_ERROR = "요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요."
        const val FIREBASE_NOT_READY = "Firebase 설정이 없어요. google-services.json을 확인해 주세요."
    }
}
