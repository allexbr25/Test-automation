package br.com.hosb.glpi.domain.model

import com.google.gson.annotations.SerializedName

enum class LoginOrigin(val value: String, val label: String) {
    LOCAL("local", "Banco de dados interno do GLPI"),
    LDAP("ldap", "Diretório Corporativo AD / LDAP HOSB"),
    MAIL("mail", "Servidor de E-mail HOSB")
}

/**
 * Perfis reais extraídos do menu superior direito de glpi.hosb.com.br (/Session/ChangeProfile)
 */
enum class GlpiUserProfile(
    val profileId: Int,
    val label: String,
    val description: String,
    val showsAllEntitiesTickets: Boolean
) {
    ADMIN(3, "Admin", "Administrador GLPI HOSB", true),
    PERFIL_TIC(14, "Perfil_TIC", "Equipe Técnica TIC HOSB (Padrão)", false),
    PERFIL_USUARIO_PADRAO(12, "Perfil_Usuario_Padrao", "Interface de Usuário HOSB", false),
    SUPER_ADMIN(4, "Super-Admin", "Super Administrador Global HOSB", true),
    SUPERVISOR(7, "Supervisor", "Supervisão de Atendimento HOSB", false);

    companion object {
        fun fromId(id: Int): GlpiUserProfile = entries.find { it.profileId == id } ?: PERFIL_TIC
    }
}

enum class TicketSortOption(val label: String) {
    UPDATED_DESC("Última atualização (Recentes)"),
    OPENED_DESC("Data de abertura (Recentes)"),
    ID_DESC("ID (Maior → Menor)"),
    ID_ASC("ID (Menor → Maior)"),
    TITLE_ASC("Título (A → Z)")
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

        fun fromLabel(raw: String): TicketStatus {
            val lower = raw.lowercase()
            return when {
                lower.contains("novo") -> NEW
                lower.contains("planejado") -> PLANNED
                lower.contains("pendente") -> PENDING
                lower.contains("solucionado") -> SOLVED
                lower.contains("fechado") -> CLOSED
                else -> ASSIGNED
            }
        }
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
    val type: String,
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
    val status: String,
    val submissionComment: String,
    val approvalComment: String?,
    val updatedAt: String
)

data class TicketDocument(
    val id: Int,
    val title: String,
    val fileName: String,
    val category: String,
    val uploadedBy: String,
    val uploadedAt: String,
    val sizeLabel: String = "245 KB"
)

data class GlpiNotificationItem(
    val id: Int,
    val ticketId: Int,
    val title: String,
    val message: String,
    val timestamp: String,
    val isNewTicket: Boolean = false,
    val isRead: Boolean = false
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
    val inTicProfile: Boolean = true,
    val timeline: List<TimelineEntry> = emptyList(),
    val approvals: List<TicketApproval> = emptyList(),
    val documents: List<TicketDocument> = emptyList()
)

data class AssetTicketRecord(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("status") val status: String,
    @SerializedName("updated") val updated: String,
    @SerializedName("opened") val opened: String,
    @SerializedName("group") val group: String,
    @SerializedName("req") val req: String,
    @SerializedName("loc") val loc: String?,
    @SerializedName("cat") val cat: String,
    @SerializedName("obs") val obs: String?,
    @SerializedName("in_tic_profile") val inTicProfile: Boolean?
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
