package br.com.hosb.glpi.ui.tickets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.hosb.glpi.R
import br.com.hosb.glpi.domain.model.GlpiUserProfile
import br.com.hosb.glpi.domain.model.Ticket
import br.com.hosb.glpi.domain.model.TicketPriority
import br.com.hosb.glpi.domain.model.TicketSortOption
import br.com.hosb.glpi.domain.model.TicketStatus
import br.com.hosb.glpi.ui.GlpiUiState
import br.com.hosb.glpi.ui.theme.StatusAssignedColor
import br.com.hosb.glpi.ui.theme.StatusClosedColor
import br.com.hosb.glpi.ui.theme.StatusNewColor
import br.com.hosb.glpi.ui.theme.StatusPendingColor
import br.com.hosb.glpi.ui.theme.StatusPlannedColor
import br.com.hosb.glpi.ui.theme.StatusSolvedColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDashboardScreen(
    uiState: GlpiUiState,
    isExpandedScreen: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (TicketStatus?) -> Unit,
    onGroupFilterChange: (String) -> Unit,
    onCategoryFilterChange: (String) -> Unit,
    onSortOptionChange: (TicketSortOption) -> Unit,
    onPageSizeChange: (Int) -> Unit,
    onPageChange: (Int) -> Unit,
    onToggleFilterPanel: () -> Unit,
    onClearAllFilters: () -> Unit,
    onSwitchProfile: (GlpiUserProfile) -> Unit,
    onSelectTicket: (Ticket?) -> Unit,
    onOpenNewTicket: () -> Unit,
    onAddFollowup: (Int, String, Boolean, String) -> Unit,
    onChangeStatus: (Int, TicketStatus) -> Unit,
    onLogout: () -> Unit
) {
    var profileMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(10.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier.size(width = 42.dp, height = 40.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo_hosb),
                                    contentDescription = "Logo HOSB",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "GLPI HOSB",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Entidade: HOSB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Menu Superior Direito de Perfis e Entidade (idêntico ao header de glpi.hosb.com.br)
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { profileMenuExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ManageAccounts,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = uiState.activeProfile.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "HOSB • ${uiState.credentials.username.ifBlank { "alex.ribeiro" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Alternar Perfil"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = profileMenuExpanded,
                            onDismissRequest = { profileMenuExpanded = false }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(
                                    text = "Alex Barbosa Ribeiro",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Entidade ativa: HOSB",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider()
                            Text(
                                text = "PERFIS DISPONÍVEIS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )

                            GlpiUserProfile.entries.forEach { profile ->
                                val isCurrent = uiState.activeProfile == profile
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = profile.label,
                                                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium
                                            )
                                            Text(
                                                text = profile.description,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    leadingIcon = {
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.VerifiedUser,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        profileMenuExpanded = false
                                        onSwitchProfile(profile)
                                    }
                                )
                            }

                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Sair da Sessão (Logout)",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    profileMenuExpanded = false
                                    onLogout()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenNewTicket,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Criar Chamado") }
            )
        }
    ) { innerPadding ->
        if (isExpandedScreen) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.46f)
                        .fillMaxHeight()
                ) {
                    TicketListPane(
                        uiState = uiState,
                        onSearchQueryChange = onSearchQueryChange,
                        onStatusFilterChange = onStatusFilterChange,
                        onGroupFilterChange = onGroupFilterChange,
                        onCategoryFilterChange = onCategoryFilterChange,
                        onSortOptionChange = onSortOptionChange,
                        onPageSizeChange = onPageSizeChange,
                        onPageChange = onPageChange,
                        onToggleFilterPanel = onToggleFilterPanel,
                        onClearAllFilters = onClearAllFilters,
                        onSelectTicket = onSelectTicket
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(0.54f)
                        .fillMaxHeight()
                        .padding(end = 12.dp)
                ) {
                    val activeTicket = uiState.selectedTicket ?: uiState.pagedTickets.firstOrNull()
                    if (activeTicket != null) {
                        TicketDetailScreen(
                            ticket = activeTicket,
                            sessionToken = uiState.activeSessionToken,
                            showBackButton = false,
                            onBack = {},
                            onAddFollowup = onAddFollowup,
                            onChangeStatus = onChangeStatus
                        )
                    }
                }
            }
        } else {
            val selectedTicket = uiState.selectedTicket
            if (selectedTicket != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    TicketDetailScreen(
                        ticket = selectedTicket,
                        sessionToken = uiState.activeSessionToken,
                        showBackButton = true,
                        onBack = { onSelectTicket(null) },
                        onAddFollowup = onAddFollowup,
                        onChangeStatus = onChangeStatus
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    TicketListPane(
                        uiState = uiState,
                        onSearchQueryChange = onSearchQueryChange,
                        onStatusFilterChange = onStatusFilterChange,
                        onGroupFilterChange = onGroupFilterChange,
                        onCategoryFilterChange = onCategoryFilterChange,
                        onSortOptionChange = onSortOptionChange,
                        onPageSizeChange = onPageSizeChange,
                        onPageChange = onPageChange,
                        onToggleFilterPanel = onToggleFilterPanel,
                        onClearAllFilters = onClearAllFilters,
                        onSelectTicket = onSelectTicket
                    )
                }
            }
        }
    }
}

@Composable
private fun TicketListPane(
    uiState: GlpiUiState,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (TicketStatus?) -> Unit,
    onGroupFilterChange: (String) -> Unit,
    onCategoryFilterChange: (String) -> Unit,
    onSortOptionChange: (TicketSortOption) -> Unit,
    onPageSizeChange: (Int) -> Unit,
    onPageChange: (Int) -> Unit,
    onToggleFilterPanel: () -> Unit,
    onClearAllFilters: () -> Unit,
    onSelectTicket: (Ticket?) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Resumo de Indicadores (KPIs sincronizados com o perfil ativo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val assignedCount = uiState.tickets.count { it.status == TicketStatus.ASSIGNED }
            val pendingCount = uiState.tickets.count { it.status == TicketStatus.PENDING }
            val plannedCount = uiState.tickets.count { it.status == TicketStatus.PLANNED }

            KpiBadgeCard(
                label = "Total (${uiState.activeProfile.label})",
                count = uiState.tickets.size.toString(),
                accent = MaterialTheme.colorScheme.primary
            )
            KpiBadgeCard("Atribuídos", assignedCount.toString(), StatusAssignedColor)
            KpiBadgeCard("Pendentes", pendingCount.toString(), StatusPendingColor)
            KpiBadgeCard("Planejados", plannedCount.toString(), StatusPlannedColor)
        }

        // Barra de Pesquisa + Botão de Filtros Avançados GLPI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Pesquisar ID, título, requerente...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )

            OutlinedButton(
                onClick = onToggleFilterPanel,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Filtros")
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Filtros",
                    maxLines = 1,
                    softWrap = false
                )
                if (uiState.activeFilterCount > 0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Badge { Text(uiState.activeFilterCount.toString()) }
                }
            }
        }

        // Painel Expansível de Filtros Avançados (Grupo Técnico, Categoria, Ordenação, Linhas/Página)
        AnimatedVisibility(visible = uiState.isFilterPanelExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Critérios de Filtro e Ordenação (GLPI)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        TextButton(
                            onClick = onClearAllFilters,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Limpar filtros",
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Text("Grupo Técnico Atribuído:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.availableGroups.forEach { group ->
                            FilterChip(
                                selected = uiState.selectedGroupFilter == group,
                                onClick = { onGroupFilterChange(group) },
                                label = { Text(group, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Categoria do Chamado:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        uiState.availableCategories.forEach { category ->
                            FilterChip(
                                selected = uiState.selectedCategoryFilter == category,
                                onClick = { onCategoryFilterChange(category) },
                                label = { Text(category, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Ordenação:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TicketSortOption.entries.forEach { sort ->
                            FilterChip(
                                selected = uiState.sortOption == sort,
                                onClick = { onSortOptionChange(sort) },
                                label = { Text(sort.label, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Linhas por página (glpilist_limit):", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25, 50, 100, 500).forEach { limit ->
                            FilterChip(
                                selected = uiState.pageSize == limit,
                                onClick = { onPageSizeChange(limit) },
                                label = {
                                    Text(
                                        text = if (limit >= 500) "Todos" else "$limit / pág",
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Chips Rápidos de Status ITIL
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.selectedStatusFilter == null,
                onClick = { onStatusFilterChange(null) },
                label = { Text("Todos (${uiState.tickets.size})", maxLines = 1, softWrap = false) }
            )
            TicketStatus.entries.forEach { status ->
                val count = uiState.tickets.count { it.status == status }
                FilterChip(
                    selected = uiState.selectedStatusFilter == status,
                    onClick = {
                        onStatusFilterChange(
                            if (uiState.selectedStatusFilter == status) null else status
                        )
                    },
                    label = { Text("${status.label} ($count)", maxLines = 1, softWrap = false) }
                )
            }
        }

        // Barra de Paginação estilo GLPI (100% responsiva — sem quebra vertical em nenhuma tela)
        val startItem = if (uiState.totalFilteredCount == 0) 0 else ((uiState.currentPage - 1) * uiState.pageSize) + 1
        val endItem = (startItem + uiState.pagedTickets.size - 1).coerceAtLeast(0)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Exibindo $startItem a $endItem de ${uiState.totalFilteredCount} linhas",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PaginationStepButton(
                    label = "◀ Anterior",
                    enabled = uiState.currentPage > 1,
                    onClick = { onPageChange(uiState.currentPage - 1) }
                )
                Text(
                    text = "${uiState.currentPage}/${uiState.totalPages}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
                PaginationStepButton(
                    label = "Próxima ▶",
                    enabled = uiState.currentPage < uiState.totalPages,
                    onClick = { onPageChange(uiState.currentPage + 1) }
                )
            }
        }

        // Lista de Chamados Paginada e Filtrada
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.pagedTickets, key = { it.id }) { ticket ->
                TicketCardItem(
                    ticket = ticket,
                    isSelected = uiState.selectedTicket?.id == ticket.id,
                    onClick = { onSelectTicket(ticket) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun PaginationStepButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (enabled) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }
    val contentColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun KpiBadgeCard(label: String, count: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = count,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun TicketCardItem(
    ticket: Ticket,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val statusColor = statusColorFor(ticket.status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "#${ticket.id}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = statusColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = ticket.status.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                PriorityPill(priority = ticket.priority)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = ticket.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = ticket.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Business,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = ticket.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ticket.requesterName,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ticket.assignedGroup,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Abertura: ${ticket.openedAt}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Atualizado: ${ticket.updatedAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${ticket.timeline.size} interação(ões) registrada(s)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onClick)
                ) {
                    Text(
                        text = "💬 Responder Chamado",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityPill(priority: TicketPriority) {
    val color = when (priority) {
        TicketPriority.HIGH, TicketPriority.VERY_HIGH, TicketPriority.MAJOR -> Color(0xFFDC2626)
        TicketPriority.MEDIUM -> Color(0xFFD97706)
        else -> Color(0xFF475569)
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = "Urgência: ${priority.label}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

fun statusColorFor(status: TicketStatus): Color = when (status) {
    TicketStatus.NEW -> StatusNewColor
    TicketStatus.ASSIGNED -> StatusAssignedColor
    TicketStatus.PLANNED -> StatusPlannedColor
    TicketStatus.PENDING -> StatusPendingColor
    TicketStatus.SOLVED -> StatusSolvedColor
    TicketStatus.CLOSED -> StatusClosedColor
}
