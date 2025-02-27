package com.hypergonial.chat.view.components

import androidx.compose.runtime.Composable
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import com.hypergonial.chat.EffectContainer
import com.hypergonial.chat.containAsEffect
import com.hypergonial.chat.model.Client
import com.hypergonial.chat.model.Secret
import com.hypergonial.chat.model.exceptions.ApiException
import com.hypergonial.chat.model.exceptions.ClientException
import com.hypergonial.chat.view.content.RegisterContent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The register component
 *
 * It is used to create a new account
 */
interface RegisterComponent : Displayable {
    val data: Value<State>

    /**
     * Called when the username changes
     *
     * @param username The new username
     */
    fun onUsernameChange(username: String)

    /**
     * Called when the password changes
     *
     * @param password The new password
     */
    fun onPasswordChange(password: String)

    /**
     * Called when the confirm password changes
     *
     * @param passwordConfirm The new confirm password
     */
    fun onPasswordConfirmChange(passwordConfirm: String)

    /** Called when the register button is clicked */
    fun onRegisterAttempt()

    /** Called when the back button is clicked */
    fun onBackClicked()

    @Composable override fun Display() = RegisterContent(this)

    data class State(
        /** The username entered by the user */
        val username: String = "",
        /** The password entered by the user */
        val password: Secret<String> = Secret(""),
        /** The password confirmation entered by the user */
        val passwordConfirm: Secret<String> = Secret(""),
        /** The username errors If this list is empty, the username is valid */
        val usernameErrors: List<String> = listOf(),
        /** The password errors If this list is empty, the password is valid */
        val passwordErrors: List<String> = listOf(),
        /** If true, the username is being checked for availability */
        val checkingUsernameAvailability: Boolean = false,
        /** If true, the register button can be enabled */
        val canRegister: Boolean = false,
        /** If true, the registration is in progress */
        val isRegistering: Boolean = false,
        /** If true, the registration failed */
        val registrationFailed: Boolean = false,
        /** The snackbar message to display */
        val snackbarMessage: EffectContainer<String> = "".containAsEffect(),
    )
}

/**
 * The default implementation of the register component
 *
 * @param ctx The component context
 * @param client The client to use for API calls
 * @param onRegister The callback to call when the user successfully registers
 * @param onBack The callback to call when the user clicks the back button
 */
class DefaultRegisterComponent(
    val ctx: ComponentContext,
    val client: Client,
    val onRegister: () -> Unit,
    val onBack: () -> Unit,
) : RegisterComponent, ComponentContext by ctx {
    override val data = MutableValue(RegisterComponent.State())
    private val scope = ctx.coroutineScope()
    private val usernameRegex = Regex("^([a-z0-9]|[a-z0-9]+(?:[._][a-z0-9]+)*)\$")
    private var checkUsernameAvailabilityJob: Job? = null
    private val logger = Logger.withTag("DefaultRegisterComponent")

    /** If true, the register button can be enabled */
    private fun updateCanRegister() {
        data.value =
            data.value.copy(
                canRegister =
                    data.value.username.isNotEmpty() &&
                        data.value.password.expose().isNotEmpty() &&
                        data.value.passwordConfirm.expose().isNotEmpty() &&
                        data.value.passwordErrors.isEmpty() &&
                        data.value.usernameErrors.isEmpty() &&
                        !data.value.checkingUsernameAvailability
            )
    }

    private fun updatePasswordErrors() {
        data.value =
            data.value.copy(passwordErrors = getPasswordErrors(data.value.password, data.value.passwordConfirm))
    }

    private fun updateUsernameErrors() {
        val errors = getUsernameErrors(data.value.username)
        data.value = data.value.copy(usernameErrors = errors)

        if (errors.isNotEmpty()) {
            return
        }

        checkUsernameAvailabilityJob?.cancel()

        checkUsernameAvailabilityJob =
            scope.launch {
                data.value = data.value.copy(checkingUsernameAvailability = true)
                // Do not overwhelm the server with requests, only check 1 second after last input
                delay(1000)
                checkUsernameAvailability()
                data.value = data.value.copy(checkingUsernameAvailability = false)
                updateCanRegister()
            }
    }

    private suspend fun checkUsernameAvailability() {
        val username = data.value.username.trim()
        if (username.isEmpty()) {
            return
        }

        try {
            val availability = client.checkUsernameForAvailability(username)
            if (!availability) {
                data.value = data.value.copy(usernameErrors = data.value.usernameErrors + "Username is already taken")
            }
        } catch (e: ApiException) {
            logger.e { "Failed to check username availability: ${e.message}" }
        } catch (e: ClientException) {
            logger.e { "Failed to check username availability: ${e.message}" }
        }
    }

    private fun getUsernameErrors(username: String): List<String> {
        if (username.length < 3) {
            return listOf("Username must be at least 3 characters")
        }

        if (username.length > 32) {
            return listOf("Username must be at most 32 characters")
        }

        if (!usernameRegex.matches(username)) {
            if (username.endsWith("_") || username.endsWith(".")) {
                return listOf("Username must not end with an underscore or period")
            }

            if (username.startsWith("_") || username.startsWith(".")) {
                return listOf("Username must not start with an underscore or period")
            }

            if (username.contains(" ")) {
                return listOf("Username must not contain spaces")
            }

            return listOf("Username must only contain lowercase letters, numbers, underscores, and periods")
        }

        return emptyList()
    }

    private fun getPasswordErrors(password: Secret<String>, passwordConfirm: Secret<String>): List<String> {
        if (passwordConfirm.expose().isNotEmpty() && password.expose() != passwordConfirm.expose()) {
            return listOf("Passwords do not match")
        }

        val errors = mutableListOf<String>()

        if (password.expose().length < 8) {
            errors.add("Password must be at least 8 characters")
        }

        // One for the pass-phrase people
        if (password.expose().split(" ").size > 20) {
            return errors
        }

        if (!password.expose().any { it.isDigit() }) {
            errors.add("Password must contain at least one number")
        }

        if (!password.expose().any { it.isUpperCase() }) {
            errors.add("Password must contain at least one uppercase letter")
        }

        if (!password.expose().any { it.isLowerCase() }) {
            errors.add("Password must contain at least one lowercase letter")
        }

        if (!password.expose().any { !it.isLetterOrDigit() }) {
            errors.add("Password must contain at least one special character")
        }

        return errors
    }

    override fun onUsernameChange(username: String) {
        data.value = data.value.copy(username = username.trim().take(32))
        updateUsernameErrors()
        updateCanRegister()
    }

    override fun onPasswordChange(password: String) {
        data.value = data.value.copy(password = Secret(password.trim()))
        updatePasswordErrors()
        updateCanRegister()
    }

    override fun onPasswordConfirmChange(passwordConfirm: String) {
        data.value = data.value.copy(passwordConfirm = Secret(passwordConfirm.trim()))
        updatePasswordErrors()
        updateCanRegister()
    }

    override fun onBackClicked() = onBack()

    override fun onRegisterAttempt() {
        // Save password before removal from UI
        val username = data.value.username.trim()
        val password = data.value.password.map { it.trim() }

        data.value =
            data.value.copy(
                password = Secret(""),
                passwordConfirm = Secret(""),
                isRegistering = true,
                registrationFailed = false,
                canRegister = false,
            )

        scope.launch {
            try {
                client.register(username, password)
                data.value = data.value.copy(isRegistering = false, registrationFailed = false)
                onRegister()
            } catch (e: ApiException) {
                data.value =
                    data.value.copy(
                        isRegistering = false,
                        registrationFailed = true,
                        snackbarMessage = "Failed to register, please try again later.".containAsEffect(),
                    )
                logger.e { "Registration failed: ${e.message}" }
            } catch (e: ClientException) {
                logger.e { "Registration failed: ${e.message}" }
                data.value =
                    data.value.copy(
                        isRegistering = false,
                        registrationFailed = true,
                        snackbarMessage = "Unknown error: ${e.message}".containAsEffect(),
                    )
            }
        }
    }
}
