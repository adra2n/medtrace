package com.yy.medtrace.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yy.medtrace.common.Result
import com.yy.medtrace.common.asResultWithoutLoading
import com.yy.medtrace.data.model.FamilyMember
import com.yy.medtrace.data.repository.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.O)
class FamilyViewModel(
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FamilyUiState())
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    fun loadMembers() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            memberRepository.getAllMembers()
                .asResultWithoutLoading()
                .collect { result ->
                    when (result) {
                        is Result.Success -> _uiState.update { it.copy(members = result.data, isLoading = false) }
                        is Result.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                        is Result.Loading -> {}
                    }
                }
        }
    }

    fun addMember(member: com.yy.medtrace.data.model.FamilyMember) {
        viewModelScope.launch {
            memberRepository.insert(member)
            loadMembers()
        }
    }

    fun updateMember(member: com.yy.medtrace.data.model.FamilyMember) {
        viewModelScope.launch {
            memberRepository.update(member)
            loadMembers()
        }
    }

    fun deleteMember(memberId: Long) {
        viewModelScope.launch {
            memberRepository.deleteById(memberId)
            loadMembers()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class FamilyUiState(
    val members: List<com.yy.medtrace.data.model.FamilyMember> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false
)

class FamilyViewModelFactory(
    private val memberRepository: MemberRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FamilyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FamilyViewModel(memberRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}