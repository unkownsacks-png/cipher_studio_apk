package com.cipher.studio.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cipher.studio.domain.repository.AuthRepository
import com.cipher.studio.domain.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    // Form State
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _key = MutableStateFlow("")
    val key: StateFlow<String> = _key.asStateFlow()

    // UI State (Idle, Loading, Error, Success)
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Login Success Event (To trigger navigation)
    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess.asStateFlow()

    fun onEmailChange(newValue: String) {
        _email.value = newValue
    }

    fun onKeyChange(newValue: String) {
        _key.value = newValue
    }

    fun handleAccess() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null // Reset error

            val result = repository.verifyAccess(_email.value, _key.value)

            when (result) {
                is AuthResult.Success -> {
                    _isLoading.value = false
                    _loginSuccess.value = true
                }
                is AuthResult.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message
                }
            }
        }
    }

    // Reset error dialog
    fun clearError() {
        _errorMessage.value = null
    }
}