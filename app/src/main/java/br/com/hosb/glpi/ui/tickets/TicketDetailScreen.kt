package br.com.hosb.glpi.ui.tickets

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.hosb.glpi.domain.model.Ticket
import br.com.hosb.glpi.domain.model.TicketStatus

@Composable
fun TicketDetailScreen(
    ticket: Ticket,
    sessionToken: String?,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onAddFollowup: (Int, String, Boolean, String) -> Unit,
    onChangeStatus: (Int, TicketStatus) -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var followupText by rememberSaveable { mutableStateOf("") }
    var isPrivateFollowup by rememberSaveable { mutableStateOf(false) }
    var followupType by rememberSaveable { mutableStateOf("Acompanhamento") }

    val tabs = listOf(
        "Chamado & Atores",
        "Timeline (${ticket.timeline.size})",
        "Aprovações (${ticket.approvals.size})",
        "Segurança & Logs"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Cabeçalho do Chamado
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Chamado #${ticket.id}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${ticket.type.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = ticket.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ações rápidas de Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        TicketStatus.ASSIGNED,
                        TicketStatus.PENDING,
                        TicketStatus.SOLVED
                    ).forEach { statusOption ->
                        FilterChip(
                            selected = ticket.status == statusOption,
                            onClick = { onChangeStatus(ticket.id, statusOption) },
                            label = { Text(statusOption.label, maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Abas baseadas em Ticket$main do GLPI (Chamado, Timeline, Aprovações, Histórico/Segurança)
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 0.dp,
            containerColor = Color.Transparent
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTabIndex) {
                0 -> TicketMetadataAndActorsTab(ticket)
                1 -> TicketTimelineTab(
                    ticket = ticket,
                    followupText = followupText,
                    isPrivate = isPrivateFollowup,
                    followupType = followupType,
                    onFollowupTextChange = { followupText = it },
                    onPrivateChange = { isPrivateFollowup = it },
                    onTypeChange = { followupType = it },
                    onSendFollowup = {
                        onAddFollowup(ticket.id, followupText, isPrivateFollowup, followupType)
                        followupText = ""
                    }
                )
                2 -> TicketApprovalsTab(ticket)
                3 -> TicketSecurityAuditTab(ticket, sessionToken)
            }
        }
    }
}

@Composable
private fun TicketMetadataAndActorsTab(ticket: Ticket) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Descrição do Chamado",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            DetailRow("Categoria", ticket.category)
            DetailRow("Localização", ticket.location)
            DetailRow("Urgência / Prioridade", ticket.priority.label)
            DetailRow("Data de abertura", ticket.openedAt)
            DetailRow("Última atualização", ticket.updatedAt)
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Atores do Chamado (ITIL)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            DetailRow("Requerente", "${ticket.requesterName} (${ticket.requesterEmail})")
            DetailRow("Contato / Ramal", ticket.requesterPhone)
            DetailRow("Atribuído (Grupo Técnico)", ticket.assignedGroup)
            DetailRow("Técnico Responsável", ticket.assignedTechnician)
            DetailRow("Observador", ticket.observerName)
        }
    }
}

@Composable
private fun TicketTimelineTab(
    ticket: Ticket,
    followupText: String,
    isPrivate: Boolean,
    followupType: String,
    onFollowupTextChange: (String) -> Unit,
    onPrivateChange: (Boolean) -> Unit,
    onTypeChange: (String) -> Unit,
    onSendFollowup: () -> Unit
) {
    // Compositor de Novo Acompanhamento / Tarefa / Solução
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Nova Interação no Chamado",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Acompanhamento", "Tarefa", "Solução").forEach { type ->
                    FilterChip(
                        selected = followupType == type,
                        onClick = { onTypeChange(type) },
                        label = { Text(type, maxLines = 1, softWrap = false) }
                    )
                }
            }

            OutlinedTextField(
                value = followupText,
                onValueChange = onFollowupTextChange,
                label = { Text("Adicionar $followupType...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

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
                    Checkbox(checked = isPrivate, onCheckedChange = onPrivateChange)
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Privado (Somente TIC)",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onSendFollowup,
                    enabled = followupText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Enviar", maxLines = 1, softWrap = false)
                }
            }
        }
    }

    ticket.timeline.forEach { item ->
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.type} • ${item.author}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )
                    Text(
                        text = item.date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = item.content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TicketApprovalsTab(ticket: Ticket) {
    if (ticket.approvals.isEmpty()) {
        Card(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Nenhuma etapa de aprovação pendente para este chamado.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        ticket.approvals.forEach { approval ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = approval.stepName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow("Status da Aprovação", approval.status)
                    DetailRow("Requerente", approval.requesterName)
                    DetailRow("Aprovador", approval.approverName)
                    DetailRow("Comentário", approval.submissionComment)
                    if (!approval.approvalComment.isNullOrBlank()) {
                        DetailRow("Parecer", approval.approvalComment)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {}) { Text("Aprovar Etapa") }
                        OutlinedButton(onClick = {}) { Text("Recusar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketSecurityAuditTab(ticket: Ticket, sessionToken: String?) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF16A34A)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Auditoria & Segurança da Sessão GLPI",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            DetailRow("Servidor Remoto", "https://glpi.hosb.com.br (TLS 1.3)")
            DetailRow("Armazenamento Local", "EncryptedSharedPreferences (AES-256-GCM)")
            DetailRow(
                "Session-Token Ativo",
                sessionToken?.take(10)?.plus("••••••••••••") ?: "Protegido em Keystore"
            )
            DetailRow("Proteção de Tela", "FLAG_SECURE habilitado para dados sensíveis")
            DetailRow("Sanitização HTML/XSS", "Ativa em Título, Descrição e Acompanhamentos")
            DetailRow("ID Auditado", "Ticket #${ticket.id}")
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.6f)
        )
    }
}
