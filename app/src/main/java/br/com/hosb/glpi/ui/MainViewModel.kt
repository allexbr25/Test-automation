package br.com.hosb.glpi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.hosb.glpi.data.repository.GlpiRepository
import br.com.hosb.glpi.data.security.SecureCredentialManager
import br.com.hosb.glpi.domain.model.AuthCredentials
import br.com.hosb.glpi.domain.model.GlpiNotificationItem
import br.com.hosb.glpi.domain.model.GlpiUserProfile
import br.com.hosb.glpi.domain.model.LoginOrigin
import br.com.hosb.glpi.domain.model.Ticket
import br.com.hosb.glpi.domain.model.TicketPriority
import br.com.hosb.glpi.domain.model.TicketSortOption
import br.com.hosb.glpi.domain.model.TicketStatus
import br.com.hosb.glpi.domain.model.TicketType
import br.com.hosb.glpi.util.GlpiNotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class GlpiUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val credentials: AuthCredentials = AuthCredentials(),
    val activeSessionToken: String? = null,
    val activeProfile: GlpiUserProfile = GlpiUserProfile.PERFIL_TIC,
    val activeEntity: String = "HOSB (Matriz Fonseca)",
    val tickets: List<Ticket> = emptyList(),
    val notifications: List<GlpiNotificationItem> = emptyList(),
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
    val unreadNotificationCount: Int
        get() = notifications.count { !it.isRead }

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
    private val repository: GlpiRepository,
    private val notificationHelper: GlpiNotificationHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GlpiUiState(
            credentials = credentialManager.loadCredentials()
        )
    )
    val uiState: StateFlow<GlpiUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

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
        executeLogin(currentCreds)
    }

    fun loginWithBiometrics() {
        val bioCreds = credentialManager.loadBiometricCredentials(_uiState.value.credentials)
        executeLogin(bioCreds)
    }

    private fun executeLogin(creds: AuthCredentials) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.authenticate(creds)
            result.fold(
                onSuccess = { sessionToken ->
                    val profile = _uiState.value.activeProfile
                    val loadedTickets = repository.getTicketsForProfile(profile)
                    val topTicket = loadedTickets.firstOrNull()
                    val now = nowFormatted()
                    val welcomeNotifications = if (topTicket != null) {
                        listOf(
                            GlpiNotificationItem(
                                id = topTicket.id * 100 + 1,
                                ticketId = topTicket.id,
                                title = "🆕 Chamado Recente #${topTicket.id}: ${topTicket.title}",
                                message = "Requerente: ${topTicket.requesterName} • Setor: ${topTicket.location}",
                                timestamp = topTicket.updatedAt.ifBlank { now },
                                isNewTicket = true,
                                isRead = false
                            )
                        )
                    } else {
                        emptyList()
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            activeSessionToken = sessionToken,
                            tickets = loadedTickets,
                            notifications = welcomeNotifications,
                            selectedTicket = null,
                            credentials = creds.copy(password = "")
                        )
                    }

                    if (topTicket != null) {
                        notificationHelper.showNotification(
                            notificationId = topTicket.id,
                            title = "GLPI HOSB • ${loadedTickets.size} chamados sincronizados",
                            body = "Último chamado #${topTicket.id}: ${topTicket.title} (${topTicket.requesterName})"
                        )
                    }

                    startRealtimeNotificationMonitor()
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

    private fun startRealtimeNotificationMonitor() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (isActive && _uiState.value.isAuthenticated) {
                delay(35_000L)
                val previousMap = _uiState.value.tickets.associateBy { it.id }
                val latestList = repository.pollLatestTicketsFromWeb(_uiState.value.activeProfile)
                if (latestList.isNotEmpty() && previousMap.isNotEmpty()) {
                    latestList.take(25).forEach { ticket ->
                        val prev = previousMap[ticket.id]
                        if (prev == null) {
                            pushAlert(
                                ticketId = ticket.id,
                                title = "🆕 Novo Chamado #${ticket.id}: ${ticket.title}",
                                body = "Aberto por ${ticket.requesterName} em ${ticket.location} (${ticket.category})",
                                isNewTicket = true
                            )
                        } else if (prev.updatedAt != ticket.updatedAt || prev.status != ticket.status) {
                            pushAlert(
                                ticketId = ticket.id,
                                title = "🔔 Movimentação no Chamado #${ticket.id}",
                                body = "${ticket.title} • Status: ${ticket.status.label} (${ticket.updatedAt})",
                                isNewTicket = false
                            )
                        }
                    }
                    _uiState.update { it.copy(tickets = latestList) }
                }
            }
        }
    }

    private fun pushAlert(
        ticketId: Int,
        title: String,
        body: String,
        isNewTicket: Boolean
    ) {
        val now = nowFormatted()
        val item = GlpiNotificationItem(
            id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            ticketId = ticketId,
            title = title,
            message = body,
            timestamp = now,
            isNewTicket = isNewTicket,
            isRead = false
        )
        _uiState.update { state ->
            state.copy(notifications = listOf(item) + state.notifications.take(29))
        }
        notificationHelper.showNotification(
            notificationId = item.id,
            title = title,
            body = body
        )
    }

    fun markAllNotificationsRead() {
        _uiState.update { state ->
            state.copy(notifications = state.notifications.map { it.copy(isRead = true) })
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
            pushAlert(
                ticketId = created.id,
                title = "🆕 Novo Chamado #${created.id} Criado",
                body = "${created.title} • Setor: ${created.location}",
                isNewTicket = true
            )
        }
    }

    fun addFollowup(
        ticketId: Int,
        content: String,
        isPrivate: Boolean,
        entryType: String,
        requestOrigin: String = "Helpdesk",
        setPending: Boolean = false
    ) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val author = _uiState.value.credentials.username.ifBlank { "Alex Barbosa Ribeiro" }
            val updatedTicket = repository.addFollowupToTicket(
                ticketId = ticketId,
                authorName = author,
                content = content,
                isPrivate = isPrivate,
                entryType = entryType,
                requestOrigin = requestOrigin,
                setPending = setPending
            )
            val allTickets = repository.getTicketsForProfile(_uiState.value.activeProfile)
            _uiState.update {
                it.copy(
                    tickets = allTickets,
                    selectedTicket = updatedTicket ?: it.selectedTicket
                )
            }
            pushAlert(
                ticketId = ticketId,
                title = "💬 Resposta enviada no Chamado #$ticketId",
                body = content.take(90),
                isNewTicket = false
            )
        }
    }

    fun addTask(
        ticketId: Int,
        content: String,
        category: String,
        stateLabel: String,
        durationMinutes: Int,
        plannedDate: String?,
        isPrivate: Boolean,
        setPending: Boolean
    ) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val author = _uiState.value.credentials.username.ifBlank { "Alex Barbosa Ribeiro" }
            val updatedTicket = repository.addTaskToTicket(
                ticketId = ticketId,
                authorName = author,
                content = content,
                category = category,
                stateLabel = stateLabel,
                durationMinutes = durationMinutes,
                plannedDate = plannedDate,
                isPrivate = isPrivate,
                setPending = setPending
            )
            val allTickets = repository.getTicketsForProfile(_uiState.value.activeProfile)
            _uiState.update {
                it.copy(
                    tickets = allTickets,
                    selectedTicket = updatedTicket ?: it.selectedTicket
                )
            }
            pushAlert(
                ticketId = ticketId,
                title = "☑️ Tarefa registrada no Chamado #$ticketId",
                body = "$category ($stateLabel • ${durationMinutes} min): ${content.take(70)}",
                isNewTicket = false
            )
        }
    }

    fun addSolution(
        ticketId: Int,
        content: String,
        solutionType: String,
        solutionTemplate: String,
        linkToKb: Boolean
    ) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val author = _uiState.value.credentials.username.ifBlank { "Alex Barbosa Ribeiro" }
            val updatedTicket = repository.addSolutionToTicket(
                ticketId = ticketId,
                authorName = author,
                content = content,
                solutionType = solutionType,
                solutionTemplate = solutionTemplate,
                linkToKb = linkToKb
            )
            val allTickets = repository.getTicketsForProfile(_uiState.value.activeProfile)
            _uiState.update {
                it.copy(
                    tickets = allTickets,
                    selectedTicket = updatedTicket ?: it.selectedTicket
                )
            }
            pushAlert(
                ticketId = ticketId,
                title = "✓ Solução registrada no Chamado #$ticketId",
                body = "Chamado solucionado ($solutionType): ${content.take(75)}",
                isNewTicket = false
            )
        }
    }

    fun addDocument(
        ticketId: Int,
        title: String,
        fileName: String,
        category: String,
        comment: String
    ) {
        viewModelScope.launch {
            val author = _uiState.value.credentials.username.ifBlank { "Alex Barbosa Ribeiro" }
            val updatedTicket = repository.addDocumentToTicket(
                ticketId = ticketId,
                authorName = author,
                title = title,
                fileName = fileName,
                category = category,
                comment = comment
            )
            val allTickets = repository.getTicketsForProfile(_uiState.value.activeProfile)
            _uiState.update {
                it.copy(
                    tickets = allTickets,
                    selectedTicket = updatedTicket ?: it.selectedTicket
                )
            }
            pushAlert(
                ticketId = ticketId,
                title = "📄 Documento anexado ao Chamado #$ticketId",
                body = "${title.ifBlank { fileName }} ($category)",
                isNewTicket = false
            )
        }
    }

    fun requestApproval(
        ticketId: Int,
        stepName: String,
        validatorType: String,
        approverName: String,
        comment: String
    ) {
        viewModelScope.launch {
            val requester = _uiState.value.credentials.username.ifBlank { "Alex Barbosa Ribeiro" }
            val updatedTicket = repository.requestApprovalForTicket(
                ticketId = ticketId,
                requesterName = requester,
                stepName = stepName,
                validatorType = validatorType,
                approverName = approverName,
                comment = comment
            )
            val allTickets = repository.getTicketsForProfile(_uiState.value.activeProfile)
            _uiState.update {
                it.copy(
                    tickets = allTickets,
                    selectedTicket = updatedTicket ?: it.selectedTicket
                )
            }
            pushAlert(
                ticketId = ticketId,
                title = "👍 Aprovação solicitada no Chamado #$ticketId",
                body = "$stepName enviada para $approverName ($validatorType)",
                isNewTicket = false
            )
        }
    }

    fun decideApproval(
        ticketId: Int,
        approvalId: Int,
        approved: Boolean,
        decisionComment: String
    ) {
        viewModelScope.launch {
            val actor = _uiState.value.credentials.username.ifBlank { "Alex Barbosa Ribeiro" }
            val updatedTicket = repository.decideTicketApproval(
                ticketId = ticketId,
                approvalId = approvalId,
                actorName = actor,
                approved = approved,
                decisionComment = decisionComment
            )
            val allTickets = repository.getTicketsForProfile(_uiState.value.activeProfile)
            _uiState.update {
                it.copy(
                    tickets = allTickets,
                    selectedTicket = updatedTicket ?: it.selectedTicket
                )
            }
            pushAlert(
                ticketId = ticketId,
                title = if (approved) "✅ Aprovação concedida no Chamado #$ticketId" else "❌ Aprovação recusada no Chamado #$ticketId",
                body = decisionComment.ifBlank { "Decisão registrada por $actor." },
                isNewTicket = false
            )
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
            pushAlert(
                ticketId = ticketId,
                title = "🔔 Status alterado no Chamado #$ticketId",
                body = "Novo status: ${status.label}",
                isNewTicket = false
            )
        }
    }

    fun logout() {
        pollingJob?.cancel()
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

    private fun nowFormatted(): String {
        return SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date())
    }

    class Factory(
        private val credentialManager: SecureCredentialManager,
        private val repository: GlpiRepository,
        private val notificationHelper: GlpiNotificationHelper
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(credentialManager, repository, notificationHelper) as T
        }
    }
}
