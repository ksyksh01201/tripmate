package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Trip
import com.example.data.TripEditRepository
import com.example.data.TripException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 편집 화면에서 끝난 작업 → 화면이 팝업을 띄우거나 홈으로 이동 */
enum class TripEditDone { Saved, Left, Deleted }

data class TripEditState(
    val isWorking: Boolean = false,
    val errorMessage: String? = null,
    val done: TripEditDone? = null
)

/** 여행방 편집 화면용 ViewModel */
class TripEditViewModel : ViewModel() {

    private val repository: TripEditRepository? = runCatching { TripEditRepository() }.getOrNull()

    private val _state = MutableStateFlow(TripEditState())
    val state: StateFlow<TripEditState> = _state.asStateFlow()

    fun save(tripId: String, title: String, startDate: String, endDate: String) =
        execute(TripEditDone.Saved, "저장하지 못했어요.") { it.updateTrip(tripId, title, startDate, endDate) }

    fun leave(trip: Trip) = execute(TripEditDone.Left, "여행방에서 나가지 못했어요.") { it.leaveTrip(trip) }

    fun delete(trip: Trip) = execute(TripEditDone.Deleted, "여행을 삭제하지 못했어요.") { it.deleteTrip(trip) }

    fun consumeDone() = _state.update { it.copy(done = null) }

    fun clearError() = _state.update { it.copy(errorMessage = null) }

    private fun execute(done: TripEditDone, failText: String, block: suspend (TripEditRepository) -> Unit) {
        val repo = repository ?: run {
            _state.update { it.copy(errorMessage = "Firebase 설정이 없어요. google-services.json을 확인해 주세요.") }
            return
        }
        if (_state.value.isWorking) return
        _state.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                block(repo)
                _state.update { it.copy(isWorking = false, done = done) }
            } catch (e: TripException) {
                _state.update { it.copy(isWorking = false, errorMessage = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(isWorking = false, errorMessage = "$failText (${e.message})") }
            }
        }
    }
}
