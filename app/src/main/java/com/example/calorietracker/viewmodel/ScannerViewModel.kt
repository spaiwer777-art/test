package com.example.calorietracker.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calorietracker.data.AppDatabase
import com.example.calorietracker.data.Food
import com.example.calorietracker.data.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScanState {
    object Idle : ScanState()
    object Loading : ScanState()
    data class Found(val food: Food) : ScanState()
    object NotFound : ScanState()
    data class Error(val message: String) : ScanState()
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FoodRepository(AppDatabase.get(application))

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private var lastScanned: String? = null

    fun onBarcodeDetected(barcode: String) {
        if (barcode == lastScanned) return // avoid re-triggering on every camera frame
        lastScanned = barcode
        _state.value = ScanState.Loading
        viewModelScope.launch {
            try {
                val local = repo.findByBarcode(barcode)
                if (local != null) {
                    _state.value = ScanState.Found(local)
                    return@launch
                }
                val online = repo.lookupBarcodeOnline(barcode)
                _state.value = if (online != null) ScanState.Found(online) else ScanState.NotFound
            } catch (e: Exception) {
                _state.value = ScanState.Error(e.message ?: "Ошибка сети")
            }
        }
    }

    fun reset() {
        lastScanned = null
        _state.value = ScanState.Idle
    }
}
