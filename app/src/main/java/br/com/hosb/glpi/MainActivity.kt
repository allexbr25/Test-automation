package br.com.hosb.glpi

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.hosb.glpi.ui.MainViewModel
import br.com.hosb.glpi.ui.login.LoginScreen
import br.com.hosb.glpi.ui.theme.GlpiHosbTheme
import br.com.hosb.glpi.ui.tickets.NewTicketSheet
import br.com.hosb.glpi.ui.tickets.TicketDashboardScreen

class MainActivity : FragmentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permissão de notificações processada pelo sistema */ }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val app = application as GlpiApplication
        val factory = MainViewModel.Factory(
            app.credentialManager,
            app.repository,
            app.notificationHelper
        )

        setContent {
            GlpiHosbTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                val isExpandedScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

                val viewModel: MainViewModel = viewModel(factory = factory)
                val uiState by viewModel.uiState.collectAsState()

                if (!uiState.isAuthenticated) {
                    LoginScreen(
                        credentials = uiState.credentials,
                        isLoading = uiState.isLoading,
                        errorMessage = uiState.errorMessage,
                        onUsernameChange = viewModel::updateUsername,
                        onPasswordChange = viewModel::updatePassword,
                        onServerUrlChange = viewModel::updateServerUrl,
                        onAppTokenChange = viewModel::updateAppToken,
                        onUserTokenChange = viewModel::updateUserToken,
                        onLoginOriginChange = viewModel::updateLoginOrigin,
                        onRememberMeChange = viewModel::updateRememberMe,
                        onLoginSubmit = viewModel::login,
                        onBiometricLogin = {
                            triggerBiometricPrompt(onAuthenticated = viewModel::loginWithBiometrics)
                        }
                    )
                } else {
                    TicketDashboardScreen(
                        uiState = uiState,
                        isExpandedScreen = isExpandedScreen,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onStatusFilterChange = viewModel::selectStatusFilter,
                        onGroupFilterChange = viewModel::selectGroupFilter,
                        onCategoryFilterChange = viewModel::selectCategoryFilter,
                        onSortOptionChange = viewModel::selectSortOption,
                        onPageSizeChange = viewModel::selectPageSize,
                        onPageChange = viewModel::setPage,
                        onToggleFilterPanel = viewModel::toggleFilterPanel,
                        onClearAllFilters = viewModel::clearAllFilters,
                        onSwitchProfile = viewModel::switchProfile,
                        onSelectTicket = viewModel::selectTicket,
                        onOpenNewTicket = { viewModel.toggleNewTicketSheet(true) },
                        onAddFollowup = viewModel::addFollowup,
                        onAddTask = viewModel::addTask,
                        onAddSolution = viewModel::addSolution,
                        onAddDocument = viewModel::addDocument,
                        onRequestApproval = viewModel::requestApproval,
                        onDecideApproval = viewModel::decideApproval,
                        onMarkNotificationsRead = viewModel::markAllNotificationsRead,
                        onChangeStatus = viewModel::changeTicketStatus,
                        onLogout = viewModel::logout
                    )

                    if (uiState.isNewTicketSheetOpen) {
                        NewTicketSheet(
                            onDismiss = { viewModel.toggleNewTicketSheet(false) },
                            onSubmit = viewModel::createTicket
                        )
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionState = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionState != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun triggerBiometricPrompt(onAuthenticated: () -> Unit) {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // Se não houver biometria cadastrada no aparelho, autentica com as credenciais salvas
            onAuthenticated()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Entrar com Biometria — GLPI HOSB")
            .setSubtitle("Use sua impressão digital ou reconhecimento facial")
            .setAllowedAuthenticators(authenticators)
            .build()

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onAuthenticated()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        Toast.makeText(
                            this@MainActivity,
                            "Biometria: $errString",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}
