package br.com.hosb.glpi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.hosb.glpi.data.repository.GlpiRepository
import br.com.hosb.glpi.data.security.SecureCredentialManager
import br.com.hosb.glpi.domain.model.AuthCredentials
import br.com.hosb.glpi.domain.model.GlpiUserProfile
import br.com.hosb.glpi.domain.model.LoginOrigin
import br.com.hosb.glpi.domain.model.Ticket
import br.com.hosb.glpi.domain.model.TicketPriority
import br.com.hosb.glpi.domain.model.TicketSortOption
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
    val activeProfile: GlpiUserProfile = GlpiUserProfile.PERFIL_TIC,
    val activeEntity: String = "HOSB (Matriz Fonseca)",
    val tickets: List<Ticket> = emptyList(),
    val selectedStatusFilter: TicketStatus? = null,
    val selectedGroupFilter: String = "TODOS",
    val selectedCategoryFilter: String = "TODAS",
    val sortOption: TicketSortOption = TicketSortOption.UPDATED_DESC,
    val pageSize: Int = 50,
    val currentPage: Int = 1,
    val isFilterPanelExpanded: Boolean = false,
    val searchQuery: String = "",
    val selectedTicket: Ticket? = null,
    val isNewTicketSheetOpen: Boolean = false
) {
    val availableGroups: List<String>
        get() = listOf("TODOS") + tickets.map { it.assignedGroup }.filter { it.isNotBlank() }.distinct().sorted()

    val availableCategories: List<String>
        get() = listOf("TODAS") + tickets.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

    val activeFilterCount: Int
        get() {
            var count = 0
            if (selectedStatusFilter != null) count++
            if (selectedGroupFilter != "TODOS") count++
            if (selectedCategoryFilter != "TODAS") count++
            if (searchQuery.isNotBlank()) count++
            return count
        }

    val filteredTickets: List<Ticket>
        get() {
            val base = tickets.filter { ticket ->
                val matchesStatus = selectedStatusFilter == null || ticket.status == selectedStatusFilter
                val matchesGroup = selectedGroupFilter == "TODOS" || ticket.assignedGroup.equals(selectedGroupFilter, ignoreCase = true)
                val matchesCategory = selectedCategoryFilter == "TODAS" || ticket.category.equals(selectedCategoryFilter, ignoreCase = true)
                val q = searchQuery.trim().lowercase()
                val matchesSearch = q.isEmpty() ||
                    ticket.id.toString().contains(q) ||
                    ticket.title.lowercase().contains(q) ||
                    ticket.category.lowercase().contains(q) ||
                    ticket.assignedGroup.lowercase().contains(q) ||
                    ticket.requesterName.lowercase().contains(q) ||
                    ticket.location.lowercase().contains(q)
                matchesStatus && matchesGroup && matchesCategory && matchesSearch
            }

            return when (sortOption) {
                TicketSortOption.UPDATED_DESC -> base
                TicketSortOption.OPENED_DESC -> base.sortedByDescending { it.id }
                TicketSortOption.ID_DESC -> base.sortedByDescending { it.id }
                TicketSortOption.ID_ASC -> base.sortedBy { it.id }
                TicketSortOption.TITLE_ASC -> base.sortedBy { it.title.lowercase() }
            }
        }

    val totalFilteredCount: Int
        get() = filteredTickets.size

    val totalPages: Int
        get() = if (totalFilteredCount == 0) 1 else ((totalFilteredCount + pageSize - 1) / pageSize)

    val pagedTickets: List<Ticket>
        get() {
            val safePage = currentPage.coerceIn(1, totalPages)
            val fromIndex = (safePage - 1) * pageSize
            if (fromIndex >= filteredTickets.size) return emptyList()
            val toIndex = (fromIndex + pageSize).coerceAtMost(filteredTickets.size)
            return filteredTickets.subList(fromIndex, toIndex)
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

    init {
        viewModelScope.launch {
            val initialTickets = repository.getTicketsForProfile(GlpiUserProfile.PERFIL_TIC)
            _uiState.update { it.copy(tickets = initialTickets) }
        }
    }

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
                    val profile = _uiState.value.activeProfile
                    val loadedTickets = repository.getTicketsForProfile(profile)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            activeSessionToken = sessionToken,
                            tickets = loadedTickets,
                            selectedTicket = null,
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

    fun switchProfile(profile: GlpiUserProfile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val ticketsForProfile = repository.switchProfileAndFetchTickets(profile)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    activeProfile = profile,
                    tickets = ticketsForProfile,
                    currentPage = 1,
                    selectedTicket = null
                )
            }
        }
    }

    fun selectStatusFilter(status: TicketStatus?) {
        _uiState.update { it.copy(selectedStatusFilter = status, currentPage = 1) }
    }

    fun selectGroupFilter(group: String) {
        _uiState.update { it.copy(selectedGroupFilter = group, currentPage = 1) }
    }

    fun selectCategoryFilter(category: String) {
        _uiState.update { it.copy(selectedCategoryFilter = category, currentPage = 1) }
    }

    fun selectSortOption(sort: TicketSortOption) {
        _uiState.update { it.copy(sortOption = sort, currentPage = 1) }
    }

    fun selectPageSize(size: Int) {
        _uiState.update { it.copy(pageSize = size, currentPage = 1) }
    }

    fun setPage(page: Int) {
        _uiState.update { it.copy(currentPage = page.coerceIn(1, it.totalPages)) }
    }

    fun toggleFilterPanel() {
        _uiState.update { it.copy(isFilterPanelExpanded = !it.isFilterPanelExpanded) }
    }

    fun clearAllFilters() {
        _uiState.update {
            it.copy(
                selectedStatusFilter = null,
                selectedGroupFilter = "TODOS",
                selectedCategoryFilter = "TODAS",
                searchQuery = "",
                sortOption = TicketSortOption.UPDATED_DESC,
                currentPage = 1
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query, currentPage = 1) }
    }

    fun selectTicket(ticket: Ticket?) {
        _uiState.update { it.copy(selectedTicket = ticket) }
        if (ticket != null) {
            viewModelScope.launch {
                val enriched = repository.fetchTicketDetailsFromWeb(ticket.id)
                if (enriched != null) {
                    val allTickets = repository.getTicketsForProfile(_uiState.value.activeProfile)
                    _uiState.update { state ->
                        if (state.selectedTicket?.id == enriched.id) {
                            state.copy(tickets = allTickets, selectedTicket = enriched)
                        } else {
                            state.copy(tickets = allTickets)
                        }
                    }
                }
            }
        }
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
            val activeProfile = _uiState.value.activeProfile
            val created = repository.createNewTicket(
                title = title,
                description = description,
                type = type,
                priority = priority,
                category = category,
                location = location,
                requesterName = requester,
                activeProfile = activeProfile
            )
            val updatedList = repository.getTicketsForProfile(activeProfile)
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
            val allTickets = repository.getTicketsForProfile(_uiState.value.activeProfile)
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
            val allTickets = repository.getTicketsForProfile(_uiState.value.activeProfile)
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
