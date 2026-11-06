package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Schedule
import com.example.data.ScheduleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleEditState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedCount: Int = 0 // 저장이 끝날 때마다 1씩 증가 → 팝업 닫기 신호
)

/** 여행룸 [일정] 탭용 ViewModel */
class ScheduleViewModel(private val tripId: String) : ViewModel() {

    private val repository: ScheduleRepository? = runCatching { ScheduleRepository() }.getOrNull()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    /** null = 불러오는 중 */
    val schedules: StateFlow<List<Schedule>?> =
        (repository?.getSchedules(tripId) ?: flowOf(emptyList()))
            .onEach { _loadError.value = null }
            .retryWhen { e, _ ->
                _loadError.value = "일정을 불러오지 못했어요. 잠시 후 자동으로 다시 시도해요. (${e.message})"
                delay(3_000)
                true
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _editState = MutableStateFlow(ScheduleEditState())
    val editState: StateFlow<ScheduleEditState> = _editState.asStateFlow()

    fun addSchedule(schedule: Schedule) {
        val repo = repository ?: run {
            _editState.update { it.copy(errorMessage = "Firebase 설정이 없어요. google-services.json을 확인해 주세요.") }
            return
        }
        if (_editState.value.isSaving) return
        _editState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                repo.addSchedule(tripId, schedule)
                _editState.update { it.copy(isSaving = false, savedCount = it.savedCount + 1) }
            } catch (e: Exception) {
                _editState.update { it.copy(isSaving = false, errorMessage = "저장하지 못했어요. (${e.message})") }
            }
        }
    }

    fun delete(schedule: Schedule) = runAction { it.deleteSchedule(tripId, schedule.scheduleId) }

    fun swap(a: Schedule, b: Schedule) = runAction { it.swapOrder(tripId, a, b) }

    private fun runAction(block: suspend (ScheduleRepository) -> Unit) {
        val repo = repository ?: return
        viewModelScope.launch {
            try {
                block(repo)
            } catch (e: Exception) {
                _loadError.value = "변경하지 못했어요. (${e.message})"
            }
        }
    }

    fun clearError() = _editState.update { it.copy(errorMessage = null) }
}
