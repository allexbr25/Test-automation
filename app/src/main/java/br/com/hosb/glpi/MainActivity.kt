package br.com.hosb.glpi

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import br.com.hosb.glpi.ui.MainViewModel
import br.com.hosb.glpi.ui.login.LoginScreen
import br.com.hosb.glpi.ui.theme.GlpiHosbTheme
import br.com.hosb.glpi.ui.tickets.NewTicketSheet
import br.com.hosb.glpi.ui.tickets.TicketDashboardScreen

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Protege telas com informações de chamados e credenciais contra capturas não autorizadas
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        enableEdgeToEdge()

        val app = application as GlpiApplication
        val factory = MainViewModel.Factory(app.credentialManager, app.repository)

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
                        onLoginSubmit = viewModel::login
                    )
                } else {
                    TicketDashboardScreen(
                        uiState = uiState,
                        isExpandedScreen = isExpandedScreen,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onStatusFilterChange = viewModel::selectStatusFilter,
                        onSelectTicket = viewModel::selectTicket,
                        onOpenNewTicket = { viewModel.toggleNewTicketSheet(true) },
                        onAddFollowup = viewModel::addFollowup,
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
