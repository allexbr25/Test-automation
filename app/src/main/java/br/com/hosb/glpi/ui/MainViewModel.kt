package br.com.hosb.glpi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.hosb.glpi.data.repository.GlpiRepository
import br.com.hosb.glpi.data.security.SecureCredentialManager
import br.com.hosb.glpi.domain.model.AuthCredentials
import br.com.hosb.glpi.domain.model.HosbSeedData
import br.com.hosb.glpi.domain.model.LoginOrigin
import br.com.hosb.glpi.domain.model.Ticket
import br.com.hosb.glpi.domain.model.TicketPriority
import br.com.hosb.glpi.domain.model.TicketStatus
import br.com.hosb.glpi.domain.model.TicketType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GlpiUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val credentials: AuthCredentials = AuthCredentials(),
    val activeSessionToken: String? = null,
    val tickets: List<Ticket> = HosbSeedData.realHosbTickets,
    val selectedStatusFilter: TicketStatus? = null,
    val searchQuery: String = "",
    val selectedTicket: Ticket? = null,
    val isNewTicketSheetOpen: Boolean = false,
    val flagSecureEnabled: Boolean = true
) {
    val filteredTickets: List<Ticket>
        get() = tickets.filter { ticket ->
            val matchesStatus = selectedStatusFilter == null || ticket.status == selectedStatusFilter
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                ticket.id.toString().contains(q) ||
                ticket.title.lowercase().contains(q) ||
                ticket.category.lowercase().contains(q) ||
                ticket.assignedGroup.lowercase().contains(q) ||
                ticket.requesterName.lowercase().contains(q) ||
                ticket.location.lowercase().contains(q)
            matchesStatus && matchesSearch
        }
}

class MainViewModel(
    private val credentialManager: SecureCredentialManager,
    private val repository: GlpiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GlpiUiState(
            credentials = credentialManager.loadCredentials()
        )
    )
    val uiState: StateFlow<GlpiUiState> = _uiState.asStateFlow()

    fun updateUsername(username: String) {
        _uiState.update { it.copy(credentials = it.credentials.copy(username = username), errorMessage = null) }
    }

    fun updatePassword(password: String) {
        _uiState.update { it.copy(credentials = it.credentials.copy(password = password), errorMessage = null) }
    }

    fun updateServerUrl(url: String) {
        _uiState.update { it.copy(credentials = it.credentials.copy(serverUrl = url), errorMessage = null) }
    }

    fun updateAppToken(appToken: String) {
        _uiState.update { it.copy(credentials = it.credentials.copy(appToken = appToken), errorMessage = null) }
    }

    fun updateUserToken(userToken: String) {
        _uiState.update { it.copy(credentials = it.credentials.copy(userToken = userToken), errorMessage = null) }
    }

    fun updateLoginOrigin(origin: LoginOrigin) {
        _uiState.update { it.copy(credentials = it.credentials.copy(loginOrigin = origin)) }
    }

    fun updateRememberMe(remember: Boolean) {
        _uiState.update { it.copy(credentials = it.credentials.copy(rememberMe = remember)) }
    }

    fun login() {
        val currentCreds = _uiState.value.credentials
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.authenticate(currentCreds)
            result.fold(
                onSuccess = { sessionToken ->
                    val loadedTickets = repository.fetchTickets()
                    // Limpa a senha da memória imediatamente após autenticar
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            activeSessionToken = sessionToken,
                            tickets = loadedTickets,
                            selectedTicket = loadedTickets.firstOrNull(),
                            credentials = it.credentials.copy(password = "")
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Falha na autenticação com glpi.hosb.com.br"
                        )
                    }
                }
            )
        }
    }

    fun selectStatusFilter(status: TicketStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectTicket(ticket: Ticket?) {
        _uiState.update { it.copy(selectedTicket = ticket) }
    }

    fun toggleNewTicketSheet(open: Boolean) {
        _uiState.update { it.copy(isNewTicketSheetOpen = open) }
    }

    fun createTicket(
        title: String,
        description: String,
        type: TicketType,
        priority: TicketPriority,
        category: String,
        location: String
    ) {
        viewModelScope.launch {
            val requester = _uiState.value.credentials.username.ifBlank { "Alex Barbosa Ribeiro" }
            val created = repository.createNewTicket(
                title = title,
                description = description,
                type = type,
                priority = priority,
                category = category,
                location = location,
                requesterName = requester
            )
            val updatedList = repository.fetchTickets()
            _uiState.update {
                it.copy(
                    tickets = updatedList,
                    selectedTicket = created,
                    isNewTicketSheetOpen = false
                )
            }
        }
    }

    fun addFollowup(ticketId: Int, content: String, isPrivate: Boolean, entryType: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val author = _uiState.value.credentials.username.ifBlank { "Alex Barbosa Ribeiro" }
            val updatedTicket = repository.addFollowupToTicket(
                ticketId = ticketId,
                authorName = author,
                content = content,
                isPrivate = isPrivate,
                entryType = entryType
            )
            val allTickets = repository.fetchTickets()
            _uiState.update {
                it.copy(
                    tickets = allTickets,
                    selectedTicket = updatedTicket ?: it.selectedTicket
                )
            }
        }
    }

    fun changeTicketStatus(ticketId: Int, status: TicketStatus) {
        viewModelScope.launch {
            val updatedTicket = repository.updateTicketStatus(ticketId, status)
            val allTickets = repository.fetchTickets()
            _uiState.update {
                it.copy(
                    tickets = allTickets,
                    selectedTicket = updatedTicket ?: it.selectedTicket
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    activeSessionToken = null,
                    selectedTicket = null
                )
            }
        }
    }

    class Factory(
        private val credentialManager: SecureCredentialManager,
        private val repository: GlpiRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(credentialManager, repository) as T
        }
    }
}
