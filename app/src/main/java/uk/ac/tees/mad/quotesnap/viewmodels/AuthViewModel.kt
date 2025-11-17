package uk.ac.tees.mad.quotesnap.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {


    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState = _loginUiState.asStateFlow()

    private val _signUpUiState = MutableStateFlow(SignUpUiState())
    val signUpUiState = _signUpUiState.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    init {

        checkAuthStatus()
    }

    // is user logged in
    private fun checkAuthStatus() {
        _authState.value = if (firebaseAuth.currentUser != null) {
            AuthState.Authenticated
        } else {
            AuthState.Unauthenticated
        }
    }

    fun updateEmailLogin(newValue: String) {
        _loginUiState.update {
            it.copy(
                email = newValue,
                errorMessage = null
            )
        }
    }

    fun updatePasswordLogin(newValue: String) {
        _loginUiState.update {
            it.copy(
                password = newValue, errorMessage = null
            )
        }
    }

    fun updateFullNameSignUp(newValue: String) {
        _signUpUiState.update {
            it.copy(
                fullName = newValue, errorMessage = null
            )
        }
    }

    fun updateEmailSignUp(newValue: String) {
        _signUpUiState.update {
            it.copy(
                email = newValue, errorMessage = null
            )
        }
    }

    fun updatePasswordSignUp(newValue: String) {
        _signUpUiState.update {
            it.copy(
                password = newValue, errorMessage = null
            )
        }
    }

    fun login() {
        val email = _loginUiState.value.email
        val password = _loginUiState.value.password

        viewModelScope.launch {
            try {
                _loginUiState.update { it.copy(isLoading = true, errorMessage = null) }
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
                _loginUiState.update { it.copy(isLoading = false) }
                _authState.value = AuthState.Authenticated
            } catch (e: Exception) {
                _loginUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Something went wrong"
                    )
                }

            }
        }
    }

    fun signUp(){
        val fullName=_signUpUiState.value.fullName
        val email=_signUpUiState.value.email
        val password=_signUpUiState.value.password


        viewModelScope.launch {
            try {
                _signUpUiState.update { it.copy(isLoading = true, errorMessage = null) }

                // Create user with email and password
                val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                _signUpUiState.update { it.copy(isLoading = false) }
                _authState.value = AuthState.Authenticated
            }catch (e: Exception){
                _signUpUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to sign up"
                    )
                }
            }
        }
    }

    // forgot password
    fun forgotPassword(
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
    ){
        val email=_loginUiState.value.email
        if(email.isBlank()){
            onError("Please enter email")
            return
        }
        viewModelScope.launch {
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener{task->
                    if(task.isSuccessful ){
                        Log.d("Auth", "Reset email sent to: $email")
                        onSuccess()
                    }else{
                        onError(task.exception?.message ?: "Failed to send reset email")
                    }
                }
        }
    }

    // Sign Out
    fun signOut() {
        firebaseAuth.signOut()
        _authState.value = AuthState.Unauthenticated
        clearLoginState()
        clearSignUpState()
    }

    // Clear states
    private fun clearLoginState() {
        _loginUiState.value = LoginUiState()
    }

    private fun clearSignUpState() {
        _signUpUiState.value = SignUpUiState()
    }

    fun clearLoginError() {
        _loginUiState.update { it.copy(errorMessage = null) }
    }

    fun clearSignUpError() {
        _signUpUiState.update { it.copy(errorMessage = null) }
    }

}


data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

data class SignUpUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

// Auth State for state handling
sealed class AuthState {
    object Idle : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
}