package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Expense
import com.example.data.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddExpenseState(
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val savedCount: Int = 0 // 저장이 끝날 때마다 1씩 증가 → 팝업 닫기 신호
)

/** 가계부(지출 목록) 화면용 ViewModel */
class BudgetViewModel(private val tripId: String) : ViewModel() {

    private val repository: ExpenseRepository? = runCatching { ExpenseRepository() }.getOrNull()

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError.asStateFlow()

    /** null = 불러오는 중 */
    val expenses: StateFlow<List<Expense>?> =
        (repository?.getExpenses(tripId) ?: flowOf(emptyList()))
            .catch { e ->
                _loadError.value = "지출 내역을 불러오지 못했어요. (${e.message})"
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _addState = MutableStateFlow(AddExpenseState())
    val addState: StateFlow<AddExpenseState> = _addState.asStateFlow()

    fun addExpense(
        title: String,
        amount: Long,
        category: String,
        paidBy: String,
        splitAmong: List<String>,
        spentAt: Long
    ) {
        val repo = repository ?: run {
            _addState.update { it.copy(errorMessage = "Firebase 설정이 없어요. google-services.json을 확인해 주세요.") }
            return
        }
        if (_addState.value.isSaving) return
        _addState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                repo.addExpense(tripId, title, amount, category, paidBy, splitAmong, spentAt)
                _addState.update { it.copy(isSaving = false, savedCount = it.savedCount + 1) }
            } catch (e: Exception) {
                _addState.update { it.copy(isSaving = false, errorMessage = "저장하지 못했어요. (${e.message})") }
            }
        }
    }

    fun clearError() = _addState.update { it.copy(errorMessage = null) }
}
