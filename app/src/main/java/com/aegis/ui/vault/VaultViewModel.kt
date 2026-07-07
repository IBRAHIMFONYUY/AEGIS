package com.aegis.ui.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aegis.security.BiometricHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class VaultViewModel @Inject constructor(
    val biometricHelper: BiometricHelper
) : ViewModel() {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked = _isUnlocked.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun unlock(activity: androidx.fragment.app.FragmentActivity) {
        if (!biometricHelper.isBiometricAvailable()) {
            _errorMessage.value = "Biometric authentication is not available on this device."
            return
        }

        biometricHelper.showBiometricPrompt(
            activity = activity,
            onSuccess = {
                _isUnlocked.value = true
                _errorMessage.value = null
            },
            onError = { code, message ->
                _errorMessage.value = "Authentication error ($code): $message"
            },
            onFailed = {
                _errorMessage.value = "Authentication failed. Please try again."
            }
        )
    }

    fun lock() {
        _isUnlocked.value = false
    }
}
