package com.tripsyc.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripsyc.app.data.api.ApiClient
import com.tripsyc.app.data.api.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val email: String = "",
    val code: String = "",
    val isLoading: Boolean = false,
    val isVerifying: Boolean = false,
    val error: String? = null,
    val otpSent: Boolean = false,
    val codeVerified: Boolean = false,
    val verifiedUser: User? = null
)

class AuthViewModel : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun updateEmail(email: String) {
        _state.value = _state.value.copy(email = email, error = null)
    }

    fun updateCode(code: String) {
        _state.value = _state.value.copy(code = code, error = null)
    }

    fun isEmailValid(): Boolean {
        val email = _state.value.email.trim().lowercase()
        if (email.isEmpty()) return false
        val parts = email.split("@")
        if (parts.size != 2) return false
        val domain = parts[1]
        return parts[0].isNotEmpty() && domain.contains(".") &&
            !domain.startsWith(".") && !domain.endsWith(".")
    }

    fun sendOtp() {
        val email = _state.value.email.trim().lowercase()
        if (email.isEmpty()) {
            _state.value = _state.value.copy(error = "Please enter your email")
            return
        }
        if (!isEmailValid()) {
            _state.value = _state.value.copy(error = "Please enter a valid email")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val response = ApiClient.apiService.sendOtp(mapOf("email" to email))
                if (response.success) {
                    _state.value = _state.value.copy(isLoading = false, otpSent = true)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = response.error ?: "Failed to send code. Please try again."
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to send code. Please try again."
                )
            }
        }
    }

    fun verifyCode(onSuccess: (User) -> Unit) {
        val code = _state.value.code.trim()
        val email = _state.value.email.trim().lowercase()

        if (code.length != 6) {
            _state.value = _state.value.copy(error = "Please enter the 6-digit code")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isVerifying = true, error = null)
            try {
                val response = ApiClient.apiService.verifyOtp(
                    mapOf("email" to email, "code" to code)
                )
                if (response.success) {
                    // Fetch the current user after successful verification
                    val userResponse = ApiClient.apiService.getCurrentUser()
                    val user = userResponse.body()
                    if (user != null) {
                        _state.value = _state.value.copy(
                            isVerifying = false,
                            codeVerified = true,
                            verifiedUser = user
                        )
                        onSuccess(user)
                    } else {
                        _state.value = _state.value.copy(
                            isVerifying = false,
                            error = "Verification succeeded but could not load user."
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        isVerifying = false,
                        error = "Invalid or expired code. Please try again."
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isVerifying = false,
                    error = "Something went wrong. Please try again."
                )
            }
        }
    }

    fun reset() {
        _state.value = AuthState()
    }
}
