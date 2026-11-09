package br.com.hosb.glpi.domain.model

import com.google.gson.annotations.SerializedName

enum class LoginOrigin(val value: String, val label: String) {
    LOCAL("local", "Banco de dados interno do GLPI"),
    LDAP("ldap", "Diretório Corporativo AD / LDAP HOSB"),
    MAIL("mail", "Servidor de E-mail HOSB")
}

data class AuthCredentials(
    val serverUrl: String = "https://glpi.hosb.com.br/apirest.php/",
    val username: String = "",
    val password: String = "",
    val appToken: String = "",
    val userToken: String = "",
    val loginOrigin: LoginOrigin = LoginOrigin.LOCAL,
    val rememberMe: Boolean = true,
    val biometricEnabled: Boolean = true
)

enum class TicketStatus(val code: Int, val label: String) {
    NEW(1, "Novo"),
    ASSIGNED(2, "Em atendimento (atribuído)"),
    PLANNED(3, "Em atendimento (planejado)"),
    PENDING(4, "Pendente"),
    SOLVED(5, "Solucionado"),
    CLOSED(6, "Fechado");

    companion object {
        fun fromCode(code: Int): TicketStatus = entries.find { it.code == code } ?: ASSIGNED
    }
}

enum class TicketType(val code: Int, val label: String) {
    INCIDENT(1, "Incidente"),
    REQUEST(2, "Requisição");

    companion object {
        fun fromCode(code: Int): TicketType = entries.find { it.code == code } ?: REQUEST
    }
}

enum class TicketPriority(val code: Int, val label: String) {
    VERY_LOW(1, "Muito baixa"),
    LOW(2, "Baixa"),
    MEDIUM(3, "Média"),
    HIGH(4, "Alta"),
    VERY_HIGH(5, "Muito alta"),
    MAJOR(6, "Crítica");

    companion object {
        fun fromCode(code: Int): TicketPriority = entries.find { it.code == code } ?: MEDIUM
    }
}

data class TimelineEntry(
    val id: Int,
    val type: String, // "Acompanhamento", "Tarefa", "Solução", "Aprovação"
    val author: String,
    val role: String,
    val date: String,
    val content: String,
    val isPrivate: Boolean = false,
    val durationMinutes: Int? = null
)

data class TicketApproval(
    val id: Int,
    val stepName: String,
    val requesterName: String,
    val approverName: String,
    val status: String, // "Aguardando aprovação", "Concedida", "Recusada"
    val submissionComment: String,
    val approvalComment: String?,
    val updatedAt: String
)

data class Ticket(
    val id: Int,
    val title: String,
    val description: String,
    val status: TicketStatus,
    val type: TicketType,
    val priority: TicketPriority,
    val category: String,
    val location: String,
    val assignedGroup: String,
    val assignedTechnician: String,
    val requesterName: String,
    val requesterEmail: String,
    val requesterPhone: String,
    val observerName: String,
    val openedAt: String,
    val updatedAt: String,
    val timeline: List<TimelineEntry> = emptyList(),
    val approvals: List<TicketApproval> = emptyList()
)

data class InitSessionResponse(
    @SerializedName("session_token") val sessionToken: String
)

data class GlpiTicketDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String?,
    @SerializedName("content") val content: String?,
    @SerializedName("status") val status: Int?,
    @SerializedName("type") val type: Int?,
    @SerializedName("priority") val priority: Int?,
    @SerializedName("date") val date: String?,
    @SerializedName("date_mod") val dateMod: String?
)

data class CreateTicketInput(
    @SerializedName("name") val name: String,
    @SerializedName("content") val content: String,
    @SerializedName("type") val type: Int,
    @SerializedName("urgency") val urgency: Int,
    @SerializedName("itilcategories_id") val categoryId: Int = 0,
    @SerializedName("locations_id") val locationId: Int = 0
)

data class CreateTicketPayload(
    @SerializedName("input") val input: CreateTicketInput
)

data class CreateFollowupInput(
    @SerializedName("items_id") val ticketId: Int,
    @SerializedName("itemtype") val itemType: String = "Ticket",
    @SerializedName("content") val content: String,
    @SerializedName("is_private") val isPrivate: Int = 0
)

data class CreateFollowupPayload(
    @SerializedName("input") val input: CreateFollowupInput
)

object HosbSeedData {
    val realHosbTickets = listOf(
        Ticket(
            id = 18333,
            title = "ALTERAÇÃO DE CAIXA CARTAO ALTERACAO DE VENDA",
            description = "Solicito ajuste de lançamento em caixa referente à alteração de modalidade de cartão na venda realizada no atendimento ambulatorial. Comprovante anexado no lote do dia.",
            status = TicketStatus.ASSIGNED,
            type = TicketType.REQUEST,
            priority = TicketPriority.HIGH,
            category = "FINANCEIRO > ALTERAÇÃO DE CAIXA",
            location = "Matriz (Fonseca) > Recepção Ambulatorial",
            assignedGroup = "Financeiro /TIC",
            assignedTechnician = "Alex Barbosa Ribeiro",
            requesterName = "VANIA DE ARAGÃO SOUZA",
            requesterEmail = "vania.souza@hosb.com.br",
            requesterPhone = "(21) 3078-0800",
            observerName = "Coordenação Financeira",
            openedAt = "07-11-2026 11:24",
            updatedAt = "07-11-2026 11:24",
            timeline = listOf(
                TimelineEntry(
                    id = 901,
                    type = "Acompanhamento",
                    author = "VANIA DE ARAGÃO SOUZA",
                    role = "Requerente",
                    date = "07-11-2026 11:24",
                    content = "Abertura do chamado com borderô de conferência do turno da manhã."
                ),
                TimelineEntry(
                    id = 902,
                    type = "Tarefa",
                    author = "Alex Barbosa Ribeiro",
                    role = "Técnico TIC",
                    date = "07-11-2026 11:40",
                    content = "Verificando consistência da transação no módulo financeiro e log de conciliação.",
                    isPrivate = false,
                    durationMinutes = 25
                )
            ),
            approvals = listOf(
                TicketApproval(
                    id = 401,
                    stepName = "Validação Supervisor Financeiro",
                    requesterName = "VANIA DE ARAGÃO SOUZA",
                    approverName = "Supervisão Financeira HOSB",
                    status = "Concedida",
                    submissionComment = "Solicitação de estorno/alteração de venda em cartão.",
                    approvalComment = "Conferido com comprovante POS.",
                    updatedAt = "07-11-2026 11:32"
                )
            )
        ),
        Ticket(
            id = 18331,
            title = "PENDÊNCIA - Guia de Atendimento e Faturamento",
            description = "Conferência de guia com pendência de autorização no faturamento particular da Recepção de Urgência. Necessário liberação para fechamento de conta.",
            status = TicketStatus.ASSIGNED,
            type = TicketType.REQUEST,
            priority = TicketPriority.HIGH,
            category = "FATURAMENTO",
            location = "Matriz (Fonseca) > Recepção Urgência",
            assignedGroup = "Faturamento",
            assignedTechnician = "Equipe Faturamento",
            requesterName = "GRAZIELLA DINIZ GUIMARAES",
            requesterEmail = "graziella.guimaraes@hosb.com.br",
            requesterPhone = "(21) 3078-0812",
            observerName = "Recepção Particular",
            openedAt = "06-11-2026 16:22",
            updatedAt = "06-11-2026 16:22",
            timeline = listOf(
                TimelineEntry(
                    id = 903,
                    type = "Acompanhamento",
                    author = "GRAZIELLA DINIZ GUIMARAES",
                    role = "Requerente",
                    date = "06-11-2026 16:22",
                    content = "Documentação digitalizada anexada ao protocolo para análise do setor de Faturamento."
                )
            )
        ),
        Ticket(
            id = 18330,
            title = "PENDÊNCIA - Lote Recepção Urgência #2",
            description = "Regularização de pendência de lançamento de materiais em atendimento na Recepção de Urgência (Recepção Particular).",
            status = TicketStatus.ASSIGNED,
            type = TicketType.REQUEST,
            priority = TicketPriority.MEDIUM,
            category = "FATURAMENTO",
            location = "Matriz (Fonseca) > Recepção Urgência",
            assignedGroup = "Faturamento",
            assignedTechnician = "Equipe Faturamento",
            requesterName = "GRAZIELLA DINIZ GUIMARAES",
            requesterEmail = "graziella.guimaraes@hosb.com.br",
            requesterPhone = "(21) 3078-0812",
            observerName = "Recepção Particular",
            openedAt = "06-11-2026 16:20",
            updatedAt = "06-11-2026 16:20"
        ),
        Ticket(
            id = 18329,
            title = "PENDÊNCIA - Lote Recepção Urgência #1",
            description = "Conferência de guia de atendimento ambulatorial/urgência encaminhada para validação da equipe de faturamento.",
            status = TicketStatus.ASSIGNED,
            type = TicketType.REQUEST,
            priority = TicketPriority.MEDIUM,
            category = "FATURAMENTO",
            location = "Matriz (Fonseca) > Recepção Urgência",
            assignedGroup = "Faturamento",
            assignedTechnician = "Equipe Faturamento",
            requesterName = "GRAZIELLA DINIZ GUIMARAES",
            requesterEmail = "graziella.guimaraes@hosb.com.br",
            requesterPhone = "(21) 3078-0812",
            observerName = "Recepção Particular",
            openedAt = "06-11-2026 16:15",
            updatedAt = "06-11-2026 16:15"
        ),
        Ticket(
            id = 18309,
            title = "SOLICITAÇÃO DE COMPRAS - Equipamentos & Insumos TIC",
            description = "Solicitação de cotação e aquisição de insumos de conectividade, patch cords Cat6 blindados e módulos de reposição para o rack da Matriz (Fonseca) > TIC.",
            status = TicketStatus.ASSIGNED,
            type = TicketType.REQUEST,
            priority = TicketPriority.HIGH,
            category = "COMPRAS",
            location = "Matriz (Fonseca) > TIC",
            assignedGroup = "Cotacao",
            assignedTechnician = "Setor de Compras / Cotação",
            requesterName = "Alex Barbosa Ribeiro",
            requesterEmail = "alex.ribeiro@hosb.com.br",
            requesterPhone = "3078-0851 / 3078-0836",
            observerName = "Coordenação TIC",
            openedAt = "05-11-2026 12:01",
            updatedAt = "06-11-2026 11:04",
            timeline = listOf(
                TimelineEntry(
                    id = 910,
                    type = "Acompanhamento",
                    author = "Alex Barbosa Ribeiro",
                    role = "Requerente (TIC)",
                    date = "05-11-2026 12:01",
                    content = "Especificação técnica anexada para 3 fornecedores homologados."
                ),
                TimelineEntry(
                    id = 911,
                    type = "Acompanhamento",
                    author = "Equipe Cotação",
                    role = "Grupo Técnico",
                    date = "06-11-2026 11:04",
                    content = "Mapa comparativo de preços elaborado e encaminhado para aprovação da diretoria."
                )
            ),
            approvals = listOf(
                TicketApproval(
                    id = 402,
                    stepName = "Etapa de Aprovação — Diretoria / Compras",
                    requesterName = "Alex Barbosa Ribeiro",
                    approverName = "Gestão de Compras HOSB",
                    status = "Aguardando aprovação",
                    submissionComment = "Aquisição necessária para manutenção preventiva de infraestrutura de rede.",
                    approvalComment = null,
                    updatedAt = "06-11-2026 11:04"
                )
            )
        ),
        Ticket(
            id = 15803,
            title = "SobreAviso 2026 — Escala Plantão TIC / CPD",
            description = "Registro e controle anual de escala de SobreAviso 2026 da equipe de infraestrutura e suporte Matriz (Fonseca) > CPD.",
            status = TicketStatus.PENDING,
            type = TicketType.REQUEST,
            priority = TicketPriority.MEDIUM,
            category = "RH - RECURSOS HUMANOS",
            location = "Matriz (Fonseca) > CPD",
            assignedGroup = "RH TIC",
            assignedTechnician = "Rildo Paiva",
            requesterName = "Rildo Paiva",
            requesterEmail = "rildo@hosb.com.br",
            requesterPhone = "(21) 99872-9500",
            observerName = "Alex Barbosa Ribeiro",
            openedAt = "09-01-2026 16:49",
            updatedAt = "09-11-2026 09:28",
            timeline = listOf(
                TimelineEntry(
                    id = 920,
                    type = "Acompanhamento",
                    author = "Rildo Paiva",
                    role = "Requerente (CPD)",
                    date = "09-11-2026 09:28",
                    content = "Atualização da planilha de plantão referente à primeira quinzena de Novembro/2026."
                )
            )
        )
    )
}
