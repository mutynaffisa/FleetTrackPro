package com.industri.fleettrack.ui.manifest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.industri.fleettrack.data.local.entity.DeliveryOrderEntity
import com.industri.fleettrack.data.repository.DeliveryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ManifestUiState {
    object Loading : ManifestUiState()
    data class Success(val orders: List<DeliveryOrderEntity>) : ManifestUiState()
    data class Error(val message: String) : ManifestUiState()
}

class ManifestViewModel(private val repository: DeliveryRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<ManifestUiState>(ManifestUiState.Loading)
    val uiState: StateFlow<ManifestUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.manifestOrdersFlow.collect { listOrders ->
                _uiState.value = ManifestUiState.Success(listOrders)
            }
        }
        refreshData()
    }

    fun refreshData() {
        viewModelScope.launch {
            _uiState.value = ManifestUiState.Loading
            val result = repository.refreshManifest()
            result.onFailure { error ->
                _uiState.value = ManifestUiState.Error(error.localizedMessage ?: "Gagal memuat data")
            }
        }
    }

    fun markAsDelivered(orderId: String) {
        viewModelScope.launch {
            repository.updateDeliveryStatus(orderId, "DELIVERED")
        }
    }

    class Factory(private val repository: DeliveryRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ManifestViewModel(repository) as T
        }
    }
}