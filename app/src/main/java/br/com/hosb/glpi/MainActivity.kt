package br.com.hosb.glpi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import br.com.hosb.glpi.ui.MainViewModel
import br.com.hosb.glpi.ui.login.AppLockScreen
import br.com.hosb.glpi.ui.login.LoginScreen
import br.com.hosb.glpi.ui.theme.GlpiHosbTheme
import br.com.hosb.glpi.ui.tickets.NewTicketSheet
import br.com.hosb.glpi.ui.tickets.TicketDashboardScreen
import br.com.hosb.glpi.util.AppVisibility
import br.com.hosb.glpi.util.TicketSyncWorker

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as GlpiApplication
        MainViewModel.Factory(app.credentialManager, app.repository, app.notificationHelper)
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permissão de notificações processada pelo sistema */ }

    private var backgroundedAt: Long = 0L
    private var biometricPromptVisible = false

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            GlpiHosbTheme {
                val windowSizeClass = calculateWindowSizeClass(this)
                val isExpandedScreen = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                val uiState by viewModel.uiState.collectAsState()

                // Mantém a sincronização em segundo plano ativa enquanto a sessão estiver logada
                LaunchedEffect(uiState.isAuthenticated) {
                    if (uiState.isAuthenticated) {
                        TicketSyncWorker.schedule(applicationContext)
                        requestIgnoreBatteryOptimizationsOnce()
                    } else {
                        TicketSyncWorker.cancel(applicationContext)
                    }
                }

                when {
                    !uiState.isAuthenticated -> LoginScreen(
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
                            triggerBiometricPrompt(
                                title = "Entrar com Biometria — GLPI HOSB",
                                onAuthenticated = viewModel::loginWithBiometrics
                            )
                        }
                    )

                    uiState.isLocked -> AppLockScreen(
                        username = uiState.credentials.username,
                        onUnlockRequest = {
                            triggerBiometricPrompt(
                                title = "Desbloquear GLPI HOSB",
                                onAuthenticated = viewModel::unlockApp
                            )
                        },
                        onLogout = viewModel::logout
                    )

                    else -> {
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
    }

    override fun onStart() {
        super.onStart()
        AppVisibility.isForeground = true
        val awayMs = if (backgroundedAt == 0L) 0L else SystemClock.elapsedRealtime() - backgroundedAt
        if (AppVisibility.suppressNextLock) {
            AppVisibility.suppressNextLock = false
        } else if (backgroundedAt != 0L && awayMs >= LOCK_GRACE_MS) {
            // Ao voltar para o app (mesmo já logado) exige biometria, como no Telegram
            viewModel.lockApp()
        }
        backgroundedAt = 0L
    }

    override fun onStop() {
        super.onStop()
        AppVisibility.isForeground = false
        if (!isChangingConfigurations && !biometricPromptVisible) {
            backgroundedAt = SystemClock.elapsedRealtime()
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

    /**
     * Pede (uma única vez) para o Android/One UI não suspender o app em segundo plano,
     * garantindo a chegada das notificações com o app fechado.
     */
    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizationsOnce() {
        val prefs = getSharedPreferences("glpi_hosb_ui_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("battery_prompt_shown", false)) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (pm.isIgnoringBatteryOptimizations(packageName)) return
        prefs.edit().putBoolean("battery_prompt_shown", true).apply()
        try {
            AppVisibility.suppressNextLock = true
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        } catch (_: Exception) {
            AppVisibility.suppressNextLock = false
        }
    }

    private fun triggerBiometricPrompt(title: String, onAuthenticated: () -> Unit) {
        if (biometricPromptVisible) return
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val canAuth = biometricManager.canAuthenticate(authenticators)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // Sem biometria/bloqueio de tela cadastrado no aparelho
            onAuthenticated()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("Use sua impressão digital, rosto ou o PIN do aparelho")
            .setAllowedAuthenticators(authenticators)
            .build()

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    biometricPromptVisible = false
                    onAuthenticated()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    biometricPromptVisible = false
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        Toast.makeText(this@MainActivity, "Biometria: $errString", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        biometricPromptVisible = true
        biometricPrompt.authenticate(promptInfo)
    }

    companion object {
        /** Tolerância para não bloquear em trocas muito rápidas de app (ex.: puxar a barra de notificações). */
        private const val LOCK_GRACE_MS = 3_000L
    }
}
