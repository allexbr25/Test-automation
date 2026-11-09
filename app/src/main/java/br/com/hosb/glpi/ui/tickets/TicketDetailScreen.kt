package br.com.hosb.glpi.ui.tickets

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import br.com.hosb.glpi.domain.model.Ticket
import br.com.hosb.glpi.domain.model.TicketStatus

enum class GlpiTimelineAction(
    val key: String,
    val label: String,
    val bgColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val icon: ImageVector
) {
    RESPONDER(
        key = "RESPONDER",
        label = "Responder",
        bgColor = Color(0xFFD0EBFF),
        contentColor = Color(0xFF0C4A6E),
        borderColor = Color(0xFF7DD3FC),
        icon = Icons.AutoMirrored.Filled.Send
    ),
    TAREFA(
        key = "TAREFA",
        label = "Tarefa",
        bgColor = Color(0xFFFDE68A),
        contentColor = Color(0xFF78350F),
        borderColor = Color(0xFFFCD34D),
        icon = Icons.Default.CheckBox
    ),
    SOLUCAO(
        key = "SOLUCAO",
        label = "Solução",
        bgColor = Color(0xFFBAE6FD),
        contentColor = Color(0xFF075985),
        borderColor = Color(0xFF38BDF8),
        icon = Icons.Default.Check
    ),
    DOCUMENTO(
        key = "DOCUMENTO",
        label = "Documento",
        bgColor = Color(0xFF86EFAC),
        contentColor = Color(0xFF065F46),
        borderColor = Color(0xFF4ADE80),
        icon = Icons.Default.Description
    ),
    APROVACAO(
        key = "APROVACAO",
        label = "Aprovação",
        bgColor = Color(0xFFDCFCE7),
        contentColor = Color(0xFF166534),
        borderColor = Color(0xFF86EFAC),
        icon = Icons.Default.ThumbUp
    )
}

@Composable
fun TicketDetailScreen(
    ticket: Ticket,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onAddFollowup: (Int, String, Boolean, String, String, Boolean) -> Unit,
    onAddTask: (Int, String, String, String, Int, String?, Boolean, Boolean) -> Unit,
    onAddSolution: (Int, String, String, String, Boolean) -> Unit,
    onAddDocument: (Int, String, String, String, String) -> Unit,
    onRequestApproval: (Int, String, String, String, String) -> Unit,
    onDecideApproval: (Int, Int, Boolean, String) -> Unit,
    onChangeStatus: (Int, TicketStatus) -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var activeActionKey by rememberSaveable { mutableStateOf(GlpiTimelineAction.RESPONDER.key) }
    var lastSentConfirmation by rememberSaveable { mutableStateOf<String?>(null) }

    val activeAction = GlpiTimelineAction.entries.find { it.key == activeActionKey }
        ?: GlpiTimelineAction.RESPONDER

    val tabs = listOf(
        "💬 Chamado & Interações (${ticket.timeline.size})",
        "📋 Dados & Atores",
        "📄 Documentos (${ticket.documents.size})",
        "👍 Aprovações (${ticket.approvals.size})"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(12.dp)
    ) {
        // Cabeçalho do Chamado
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
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
                                text = "• ${ticket.status.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${ticket.title} (${ticket.id})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

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
                            label = { Text("Status: ${statusOption.label}", maxLines = 1, softWrap = false) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Abas baseadas em Ticket$main do GLPI
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

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTabIndex) {
                0 -> TicketInteractiveTimelineTab(
                    ticket = ticket,
                    activeAction = activeAction,
                    confirmationMessage = lastSentConfirmation,
                    onSelectAction = {
                        activeActionKey = it.key
                        lastSentConfirmation = null
                    },
                    onSubmitFollowup = { text, origin, isPriv, isPend ->
                        onAddFollowup(ticket.id, text, isPriv, "Acompanhamento", origin, isPend)
                        lastSentConfirmation = "✔ Resposta (Acompanhamento) adicionada ao chamado #${ticket.id}!"
                    },
                    onSubmitTask = { text, cat, stateLbl, dur, planDate, isPriv, isPend ->
                        onAddTask(ticket.id, text, cat, stateLbl, dur, planDate, isPriv, isPend)
                        lastSentConfirmation = "✔ Tarefa ($stateLbl • ${dur} min) registrada no chamado #${ticket.id}!"
                    },
                    onSubmitSolution = { text, solType, solTpl, toKb ->
                        onAddSolution(ticket.id, text, solType, solTpl, toKb)
                        lastSentConfirmation = "✔ Solução adicionada e chamado #${ticket.id} marcado como Solucionado!"
                    },
                    onSubmitDocument = { docTitle, fileName, cat, comment ->
                        onAddDocument(ticket.id, docTitle, fileName, cat, comment)
                        lastSentConfirmation = "✔ Documento \"$fileName\" anexado ao chamado #${ticket.id}!"
                    },
                    onSubmitApproval = { step, valType, approver, comment ->
                        onRequestApproval(ticket.id, step, valType, approver, comment)
                        lastSentConfirmation = "✔ Solicitação de aprovação enviada para $approver no chamado #${ticket.id}!"
                    },
                    onDecideApproval = { appId, approved, comment ->
                        onDecideApproval(ticket.id, appId, approved, comment)
                        lastSentConfirmation = if (approved) {
                            "✔ Etapa de aprovação concedida no chamado #${ticket.id}!"
                        } else {
                            "✔ Etapa de aprovação recusada no chamado #${ticket.id}!"
                        }
                    }
                )
                1 -> TicketMetadataAndActorsTab(ticket)
                2 -> TicketDocumentsTab(
                    ticket = ticket,
                    onQuickAddDocument = {
                        selectedTabIndex = 0
                        activeActionKey = GlpiTimelineAction.DOCUMENTO.key
                    }
                )
                3 -> TicketApprovalsTab(
                    ticket = ticket,
                    onDecideApproval = { appId, approved, comment ->
                        onDecideApproval(ticket.id, appId, approved, comment)
                    },
                    onOpenRequestApproval = {
                        selectedTabIndex = 0
                        activeActionKey = GlpiTimelineAction.APROVACAO.key
                    }
                )
            }
        }
    }
}

@Composable
private fun TicketInteractiveTimelineTab(
    ticket: Ticket,
    activeAction: GlpiTimelineAction,
    confirmationMessage: String?,
    onSelectAction: (GlpiTimelineAction) -> Unit,
    onSubmitFollowup: (String, String, Boolean, Boolean) -> Unit,
    onSubmitTask: (String, String, String, Int, String?, Boolean, Boolean) -> Unit,
    onSubmitSolution: (String, String, String, Boolean) -> Unit,
    onSubmitDocument: (String, String, String, String) -> Unit,
    onSubmitApproval: (String, String, String, String) -> Unit,
    onDecideApproval: (Int, Boolean, String) -> Unit
) {
    val context = LocalContext.current

    // Estados de Responder (ITILFollowup)
    var followupText by rememberSaveable { mutableStateOf("") }
    var requestOrigin by rememberSaveable { mutableStateOf("Helpdesk") }
    var followupPrivate by rememberSaveable { mutableStateOf(false) }
    var followupPending by rememberSaveable { mutableStateOf(false) }

    // Estados de Tarefa (TicketTask)
    var taskText by rememberSaveable { mutableStateOf("") }
    var taskCategory by rememberSaveable { mutableStateOf("Suporte Técnico TIC") }
    var taskState by rememberSaveable { mutableStateOf("Feito") }
    var taskDuration by rememberSaveable { mutableIntStateOf(15) }
    var taskPlanExpanded by rememberSaveable { mutableStateOf(false) }
    var taskPlannedDate by rememberSaveable { mutableStateOf("") }
    var taskPrivate by rememberSaveable { mutableStateOf(false) }
    var taskPending by rememberSaveable { mutableStateOf(false) }

    // Estados de Solução (ITILSolution)
    var solutionText by rememberSaveable { mutableStateOf("") }
    var solutionTemplate by rememberSaveable { mutableStateOf("Atendimento concluído") }
    var solutionType by rememberSaveable { mutableStateOf("Resolvido remotamente") }
    var solutionToKb by rememberSaveable { mutableStateOf(false) }

    // Estados de Documento (Document)
    var docTitle by rememberSaveable { mutableStateOf("") }
    var docFileName by rememberSaveable { mutableStateOf("") }
    var docCategory by rememberSaveable { mutableStateOf("Evidência / Print de Tela") }
    var docComment by rememberSaveable { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var resolvedName = uri.lastPathSegment ?: "anexo_glpi_${ticket.id}.jpg"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0 && cursor.moveToFirst()) {
                        resolvedName = cursor.getString(nameIndex) ?: resolvedName
                    }
                }
            } catch (_: Exception) {
            }
            docFileName = resolvedName
            if (docTitle.isBlank()) {
                docTitle = resolvedName.substringBeforeLast(".")
            }
        }
    }

    // Estados de Aprovação (TicketValidation)
    var approvalStep by rememberSaveable { mutableStateOf("Aprovação") }
    var validatorType by rememberSaveable { mutableStateOf("Usuário") }
    var approverName by rememberSaveable { mutableStateOf("Coordenação TIC HOSB") }
    var approvalComment by rememberSaveable { mutableStateOf("") }

    // 1. Card de Abertura do Chamado (Verde suave idêntico ao GLPI Web da imagem enviada)
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE6F4EA)),
        border = BorderStroke(1.dp, Color(0xFFB7DFC4)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFD1E7DD)
            ) {
                Text(
                    text = "Criado em: ${ticket.openedAt} por 👤 ${ticket.requesterName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF0F5132),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = ticket.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF14532D)
            )
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFB7DFC4))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ticket.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1E293B)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "📍 ${ticket.location}  •  📂 ${ticket.category}  •  👥 Grupo: ${ticket.assignedGroup}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF334155)
            )
        }
    }

    // 2. Barra dos 5 Botões Oficiais do GLPI (Responder | Tarefa | Solução | Documento | Aprovação)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Barra de Ações do Chamado (GLPI):",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlpiTimelineAction.entries.forEach { action ->
                    val isSelected = activeAction == action
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) action.bgColor else action.bgColor.copy(alpha = 0.55f),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) action.contentColor else action.borderColor
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectAction(action) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.label,
                                tint = action.contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = action.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = action.contentColor,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!confirmationMessage.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF16A34A).copy(alpha = 0.14f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Text(
                        text = confirmationMessage,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Painel Dinâmico correspondente ao botão selecionado
            when (activeAction) {
                // ================= 1. RESPONDER (ITILFollowup) =================
                GlpiTimelineAction.RESPONDER -> {
                    Text(
                        text = "💬 Responder ao Chamado #${ticket.id} (Acompanhamento)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = GlpiTimelineAction.RESPONDER.contentColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Modelos rápidos de acompanhamento:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Em verificação pela equipe TIC.",
                            "Acessos do colaborador revogados conforme solicitado.",
                            "Favor informar mais detalhes ou anexar print da tela.",
                            "Atendimento realizado e validado no setor."
                        ).forEach { preset ->
                            FilterChip(
                                selected = followupText == preset,
                                onClick = { followupText = preset },
                                label = { Text(preset, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Origem da requisição:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Helpdesk", "Telefone", "E-mail", "Direta", "Escrita").forEach { orig ->
                            FilterChip(
                                selected = requestOrigin == orig,
                                onClick = { requestOrigin = orig },
                                label = { Text(orig, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = followupText,
                        onValueChange = { followupText = it },
                        label = { Text("Mensagem de acompanhamento...") },
                        placeholder = { Text("Digite a resposta para o chamado #${ticket.id}...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            Checkbox(checked = followupPrivate, onCheckedChange = { followupPrivate = it })
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Privado", style = MaterialTheme.typography.bodySmall, maxLines = 1, softWrap = false)

                            Spacer(modifier = Modifier.width(8.dp))

                            Checkbox(checked = followupPending, onCheckedChange = { followupPending = it })
                            Text("Pendente", style = MaterialTheme.typography.bodySmall, maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = {
                                onSubmitFollowup(followupText, requestOrigin, followupPrivate, followupPending)
                                followupText = ""
                            },
                            enabled = followupText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Adicionar", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }

                // ================= 2. TAREFA (TicketTask) =================
                GlpiTimelineAction.TAREFA -> {
                    Text(
                        text = "☑️ Nova Tarefa Técnica no Chamado #${ticket.id}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = GlpiTimelineAction.TAREFA.contentColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Modelos de tarefa:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Desativação de conta AD, e-mail e sistema MV.",
                            "Verificação presencial de estação de trabalho e rede.",
                            "Manutenção preventiva e testes de impressão.",
                            "Configuração de perfil e permissões de usuário."
                        ).forEach { tpl ->
                            FilterChip(
                                selected = taskText == tpl,
                                onClick = { taskText = tpl },
                                label = { Text(tpl, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Text(
                        text = "Categoria da Tarefa:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Suporte Técnico TIC",
                            "Gestão de Acessos / AD",
                            "Infraestrutura & Redes",
                            "Sistemas / MV"
                        ).forEach { cat ->
                            FilterChip(
                                selected = taskCategory == cat,
                                onClick = { taskCategory = cat },
                                label = { Text(cat, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status:", style = MaterialTheme.typography.labelSmall)
                        listOf("Para fazer", "Feito", "Informação").forEach { st ->
                            FilterChip(
                                selected = taskState == st,
                                onClick = { taskState = st },
                                label = { Text(st, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Duração:", style = MaterialTheme.typography.labelSmall)
                        listOf(15, 30, 45, 60, 120).forEach { mins ->
                            FilterChip(
                                selected = taskDuration == mins,
                                onClick = { taskDuration = mins },
                                label = { Text("${mins} min", maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            taskPlanExpanded = !taskPlanExpanded
                            if (taskPlanExpanded && taskPlannedDate.isBlank()) {
                                taskPlannedDate = "10-11-2026 09:00"
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (taskPlanExpanded) "Ocultar planejamento" else "Planejar esta tarefa",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    AnimatedVisibility(visible = taskPlanExpanded) {
                        OutlinedTextField(
                            value = taskPlannedDate,
                            onValueChange = { taskPlannedDate = it },
                            label = { Text("Data e Hora Planejada (DD-MM-AAAA HH:MM)") },
                            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = taskText,
                        onValueChange = { taskText = it },
                        label = { Text("Descrição da tarefa realizada ou planejada...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            Checkbox(checked = taskPrivate, onCheckedChange = { taskPrivate = it })
                            Text("Privado", style = MaterialTheme.typography.bodySmall, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.width(8.dp))
                            Checkbox(checked = taskPending, onCheckedChange = { taskPending = it })
                            Text("Pendente", style = MaterialTheme.typography.bodySmall, maxLines = 1, softWrap = false)
                        }

                        Button(
                            onClick = {
                                onSubmitTask(
                                    taskText,
                                    taskCategory,
                                    taskState,
                                    taskDuration,
                                    if (taskPlanExpanded) taskPlannedDate else null,
                                    taskPrivate,
                                    taskPending
                                )
                                taskText = ""
                            },
                            enabled = taskText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD97706),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Adicionar", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }

                // ================= 3. SOLUÇÃO (ITILSolution) =================
                GlpiTimelineAction.SOLUCAO -> {
                    Text(
                        text = "✓ Propor Solução Oficial para o Chamado #${ticket.id}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = GlpiTimelineAction.SOLUCAO.contentColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Modelo de Solução:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Atendimento concluído" to "Procedimento técnico concluído com sucesso e validado junto ao setor solicitante.",
                            "Acessos revogados/liberados" to "Todos os acessos solicitados no chamado foram processados conforme política de segurança do HOSB.",
                            "Equipamento configurado" to "Equipamento revisado, configurado e testado em pleno funcionamento na unidade.",
                            "Orientação ao usuário" to "Usuário orientado quanto ao fluxo correto do sistema; chamado encerrado."
                        ).forEach { (tplName, tplBody) ->
                            FilterChip(
                                selected = solutionTemplate == tplName,
                                onClick = {
                                    solutionTemplate = tplName
                                    solutionText = tplBody
                                },
                                label = { Text(tplName, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Text(
                        text = "Tipo de Solução:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Resolvido remotamente",
                            "Resolvido presencialmente",
                            "Contorno técnico aplicado"
                        ).forEach { sType ->
                            FilterChip(
                                selected = solutionType == sType,
                                onClick = { solutionType = sType },
                                label = { Text(sType, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = solutionText,
                        onValueChange = { solutionText = it },
                        label = { Text("Descrição detalhada da Solução...") },
                        placeholder = { Text("Descreva como o chamado #${ticket.id} foi solucionado...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )

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
                            Checkbox(checked = solutionToKb, onCheckedChange = { solutionToKb = it })
                            Text(
                                text = "Salvar na Base de Conhecimento (KB)",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = {
                                onSubmitSolution(solutionText, solutionType, solutionTemplate, solutionToKb)
                                solutionText = ""
                            },
                            enabled = solutionText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0284C7),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Adicionar", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }

                // ================= 4. DOCUMENTO (Document_Item) =================
                GlpiTimelineAction.DOCUMENTO -> {
                    Text(
                        text = "📄 Anexar Documento / Print ao Chamado #${ticket.id}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = GlpiTimelineAction.DOCUMENTO.contentColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Rubrica / Categoria do Documento:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Evidência / Print de Tela",
                            "Termo / Desligamento",
                            "Laudo Técnico TIC",
                            "Nota Fiscal / Orçamento"
                        ).forEach { cat ->
                            FilterChip(
                                selected = docCategory == cat,
                                onClick = { docCategory = cat },
                                label = { Text(cat, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                br.com.hosb.glpi.util.AppVisibility.suppressNextLock = true
                                filePickerLauncher.launch("*/*")
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Escolher Arquivo (Foto / PDF)", maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = {
                                docFileName = "captura_tela_chamado_${ticket.id}.png"
                                if (docTitle.isBlank()) {
                                    docTitle = "Captura de tela #${ticket.id}"
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("📸 Captura de tela", maxLines = 1, softWrap = false)
                        }
                    }

                    if (docFileName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "📎 Arquivo selecionado: $docFileName",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Título do Documento") },
                        placeholder = { Text("Ex: Comprovante de revogação de acesso #${ticket.id}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = docComment,
                        onValueChange = { docComment = it },
                        label = { Text("Comentário / Observação (Opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                val finalFile = docFileName.ifBlank { "documento_chamado_${ticket.id}.pdf" }
                                val finalTitle = docTitle.ifBlank { finalFile }
                                onSubmitDocument(finalTitle, finalFile, docCategory, docComment)
                                docTitle = ""
                                docFileName = ""
                                docComment = ""
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Adicionar novo arquivo", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }
                }

                // ================= 5. APROVAÇÃO (TicketValidation) =================
                GlpiTimelineAction.APROVACAO -> {
                    Text(
                        text = "👍 Solicitar ou Responder Aprovação no Chamado #${ticket.id}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = GlpiTimelineAction.APROVACAO.contentColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Etapa de aprovação *:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Aprovação",
                            "Validação Técnica TIC",
                            "Autorização de Gestor / RH",
                            "Diretoria HOSB"
                        ).forEach { stp ->
                            FilterChip(
                                selected = approvalStep == stp,
                                onClick = { approvalStep = stp },
                                label = { Text(stp, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tipo de Aprovador:", style = MaterialTheme.typography.labelSmall)
                        listOf("Usuário", "Grupo").forEach { vType ->
                            FilterChip(
                                selected = validatorType == vType,
                                onClick = {
                                    validatorType = vType
                                    approverName = if (vType == "Grupo") "Grupo TIC" else "Mônica Sousa"
                                },
                                label = { Text(vType, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Text(
                        text = "Selecionar Aprovador ($validatorType):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val approverOptions = if (validatorType == "Grupo") {
                        listOf("Grupo TIC", "Supervisão HOSB", "Coordenação RH", "Diretoria HOSB")
                    } else {
                        listOf("Mônica Sousa", "Alex Barbosa Ribeiro", "Supervisão TIC", "Gestor do Setor")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        approverOptions.forEach { appr ->
                            FilterChip(
                                selected = approverName == appr,
                                onClick = { approverName = appr },
                                label = { Text(appr, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = approvalComment,
                        onValueChange = { approvalComment = it },
                        label = { Text("Comentários da solicitação de aprovação...") },
                        placeholder = { Text("Informe o motivo ou escopo da aprovação...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onSubmitApproval(approvalStep, validatorType, approverName, approvalComment)
                                approvalComment = ""
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Adicionar", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                        }
                    }

                    if (ticket.approvals.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aprovações registradas neste chamado:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ticket.approvals.forEach { approval ->
                            ApprovalDecisionCard(
                                approval = approval,
                                onApprove = { comment -> onDecideApproval(approval.id, true, comment) },
                                onReject = { comment -> onDecideApproval(approval.id, false, comment) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }

    // 3. Histórico de Mensagens, Tarefas, Soluções, Documentos e Aprovações
    Text(
        text = "Histórico do Chamado (${ticket.timeline.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp)
    )

    ticket.timeline.reversed().forEach { item ->
        val cardColor = when (item.type) {
            "Tarefa" -> Color(0xFFFFFBEB)
            "Solução" -> Color(0xFFF0F9FF)
            "Documento" -> Color(0xFFECFDF5)
            "Aprovação" -> Color(0xFFF0FDF4)
            else -> MaterialTheme.colorScheme.surface
        }
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${item.type} • ${item.author}" + if (item.isPrivate) " 🔒 (Privado)" else "",
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
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF1E293B)
                )
            }
        }
    }
}

@Composable
private fun ApprovalDecisionCard(
    approval: br.com.hosb.glpi.domain.model.TicketApproval,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    var decisionNote by rememberSaveable(approval.id) { mutableStateOf("") }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${approval.stepName} • ${approval.status}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Solicitante: ${approval.requesterName} → Aprovador: ${approval.approverName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Comentário: ${approval.submissionComment}",
                style = MaterialTheme.typography.bodySmall
            )
            if (!approval.approvalComment.isNullOrBlank()) {
                Text(
                    text = "Parecer: ${approval.approvalComment}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (approval.status.contains("Aguardando", ignoreCase = true)) {
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = decisionNote,
                    onValueChange = { decisionNote = it },
                    label = { Text("Parecer de aprovação/recusa (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onApprove(decisionNote) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aprovar", maxLines = 1, softWrap = false)
                    }
                    OutlinedButton(
                        onClick = { onReject(decisionNote) },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recusar", maxLines = 1, softWrap = false)
                    }
                }
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
                text = "Descrição Original do Chamado",
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
private fun TicketDocumentsTab(
    ticket: Ticket,
    onQuickAddDocument: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Documentos Anexados (${ticket.documents.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onQuickAddDocument,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Novo Documento", maxLines = 1, softWrap = false)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (ticket.documents.isEmpty()) {
                Text(
                    text = "Nenhum documento anexado a este chamado ainda. Toque em \"Documento\" na barra de ações ou em \"Novo Documento\" acima para anexar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ticket.documents.forEach { doc ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "📎 ${doc.title} (${doc.fileName})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Rubrica: ${doc.category} • Enviado por ${doc.uploadedBy} em ${doc.uploadedAt}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TicketApprovalsTab(
    ticket: Ticket,
    onDecideApproval: (Int, Boolean, String) -> Unit,
    onOpenRequestApproval: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Etapas de Aprovação (${ticket.approvals.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onOpenRequestApproval,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Solicitar Aprovação", maxLines = 1, softWrap = false)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (ticket.approvals.isEmpty()) {
                Text(
                    text = "Nenhuma aprovação solicitada ainda para este chamado. Use o botão \"👍 Aprovação\" para enviar um pedido de validação.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ticket.approvals.forEach { approval ->
                    ApprovalDecisionCard(
                        approval = approval,
                        onApprove = { comment -> onDecideApproval(approval.id, true, comment) },
                        onReject = { comment -> onDecideApproval(approval.id, false, comment) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
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
