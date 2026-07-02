package com.aegis.ui.threatlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.core.ThreatLevel
import com.aegis.data.db.entity.ThreatEvent
import com.aegis.data.repository.ThreatRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ThreatLogViewModel(
    private val threatRepository: ThreatRepository
) : ViewModel() {

    private val _threats = MutableStateFlow<List<ThreatEvent>>(emptyList())
    val threats: StateFlow<List<ThreatEvent>> = _threats.asStateFlow()

    private val _filterLevel = MutableStateFlow(ThreatLevel.SUSPICIOUS)
    val filterLevel: StateFlow<ThreatLevel> = _filterLevel.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        observeThreats()
    }

    private fun observeThreats() {
        viewModelScope.launch {
            _isLoading.value = true
            threatRepository.getAllThreats().collect {
                _threats.value = it
                _isLoading.value = false
            }
        }
    }

    fun setFilterLevel(level: ThreatLevel) {
        _filterLevel.value = level
    }

    fun getFilteredThreats(): List<ThreatEvent> {
        return _threats.value.filter { it.threatLevel >= _filterLevel.value.value }
    }
}
