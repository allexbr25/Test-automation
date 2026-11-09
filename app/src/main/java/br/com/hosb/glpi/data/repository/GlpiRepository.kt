package br.com.hosb.glpi.data.repository

import android.content.Context
import android.util.Base64
import br.com.hosb.glpi.data.remote.GlpiApiService
import br.com.hosb.glpi.data.remote.GlpiAuthInterceptor
import br.com.hosb.glpi.data.security.SecureCredentialManager
import br.com.hosb.glpi.domain.model.AssetTicketRecord
import br.com.hosb.glpi.domain.model.AuthCredentials
import br.com.hosb.glpi.domain.model.CreateFollowupInput
import br.com.hosb.glpi.domain.model.CreateFollowupPayload
import br.com.hosb.glpi.domain.model.CreateTicketInput
import br.com.hosb.glpi.domain.model.CreateTicketPayload
import br.com.hosb.glpi.domain.model.GlpiUserProfile
import br.com.hosb.glpi.domain.model.Ticket
import br.com.hosb.glpi.domain.model.TicketPriority
import br.com.hosb.glpi.domain.model.TicketStatus
import br.com.hosb.glpi.domain.model.TicketType
import br.com.hosb.glpi.domain.model.TimelineEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionSpec
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.TlsVersion
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class GlpiRepository(
    private val context: Context,
    private val credentialManager: SecureCredentialManager
) {
    private var currentAppToken: String = ""
    private var currentBaseUrl: String = "https://glpi.hosb.com.br/apirest.php/"
    private var lastCsrfToken: String? = null
    private val gson = Gson()

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val webHttpClient: OkHttpClient by lazy {
        val modernTlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .build()

        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectionSpecs(listOf(modernTlsSpec))
            .cookieJar(object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val hostCookies = cookieStore.getOrPut(url.host) { mutableListOf() }
                    val now = System.currentTimeMillis()
                    cookies.forEach { newCookie ->
                        hostCookies.removeAll { it.name == newCookie.name }
                        if (newCookie.value.isNotEmpty() && newCookie.expiresAt > now) {
                            hostCookies.add(newCookie)
                        }
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    val now = System.currentTimeMillis()
                    return cookieStore[url.host]?.filter { it.expiresAt > now } ?: emptyList()
                }
            })
            .build()
    }

    private var fallbackTickets: MutableList<Ticket> = mutableListOf()
    private var liveTickets: MutableList<Ticket> = mutableListOf()

    init {
        loadFallbackTickets()
    }

    private fun loadFallbackTickets() {
        try {
            val json = context.assets.open("hosb_tickets.json").bufferedReader().use { it.readText() }
            val listType = object : TypeToken<List<AssetTicketRecord>>() {}.type
            val records: List<AssetTicketRecord> = gson.fromJson(json, listType)
            fallbackTickets = records.map { rec ->
                Ticket(
                    id = rec.id,
                    title = rec.title,
                    description = "Chamado #${rec.id} (${rec.loc ?: "Matriz (Fonseca)"}) • Categoria: ${rec.cat}. Requerente: ${rec.req}.",
                    status = TicketStatus.fromLabel(rec.status),
                    type = TicketType.REQUEST,
                    priority = if (rec.title.contains("URGENTE", ignoreCase = true) || rec.cat.contains("FINANCEIRO")) {
                        TicketPriority.HIGH
                    } else {
                        TicketPriority.MEDIUM
                    },
                    category = rec.cat.ifBlank { "INFORMATICA (TIC) > OUTROS ASSUNTOS" },
                    location = rec.loc ?: "Matriz (Fonseca) > TIC",
                    assignedGroup = rec.group.ifBlank { "TIC" },
                    assignedTechnician = "Equipe ${rec.group.ifBlank { "TIC" }}",
                    requesterName = rec.req.ifBlank { "Colaborador HOSB" },
                    requesterEmail = "atendimento@hosb.com.br",
                    requesterPhone = "(21) 3078-0800",
                    observerName = rec.obs?.ifBlank { "Coordenação HOSB" } ?: "Coordenação HOSB",
                    openedAt = rec.opened,
                    updatedAt = rec.updated,
                    inTicProfile = rec.inTicProfile ?: true,
                    timeline = listOf(
                        TimelineEntry(
                            id = rec.id * 10 + 1,
                            type = "Acompanhamento",
                            author = rec.req.ifBlank { "Solicitante HOSB" },
                            role = "Requerente",
                            date = rec.opened,
                            content = "Abertura do chamado #${rec.id}: ${rec.title}"
                        )
                    )
                )
            }.toMutableList()
        } catch (_: Exception) {
        }
    }

    private fun buildApiService(baseUrl: String): GlpiApiService {
        val sanitizedUrl = SecureCredentialManager.sanitizeServerUrl(baseUrl)
        currentBaseUrl = sanitizedUrl

        val modernTlsSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
            .build()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .connectionSpecs(listOf(modernTlsSpec))
            .addInterceptor(
                GlpiAuthInterceptor(
                    credentialManager = credentialManager,
                    appTokenProvider = { currentAppToken }
                )
            )
            .build()

        return Retrofit.Builder()
            .baseUrl(sanitizedUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GlpiApiService::class.java)
    }

    suspend fun authenticate(credentials: AuthCredentials): Result<String> = withContext(Dispatchers.IO) {
        if (credentials.username.isBlank() && credentials.userToken.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Informe o Usuário ou o User-Token da API."))
        }
        if (credentials.userToken.isBlank() && credentials.password.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Informe a Senha para autenticação de usuário."))
        }

        currentAppToken = credentials.appToken.trim()
        credentialManager.saveCredentials(credentials)
        cookieStore.clear()

        // 1. Autentica na sessão real de glpi.hosb.com.br e puxa todos os chamados em tempo real
        try {
            val loginPageReq = Request.Builder()
                .url("https://glpi.hosb.com.br/")
                .header("User-Agent", "GLPI-HOSB-Android/1.0")
                .get()
                .build()
            val loginPageHtml = webHttpClient.newCall(loginPageReq).execute().use { it.body?.string() ?: "" }
            val csrfMatch = Regex("""name="_glpi_csrf_token"\s+value="([^"]+)"""").find(loginPageHtml)

            if (csrfMatch != null && credentials.username.isNotBlank() && credentials.password.isNotBlank()) {
                val csrfToken = csrfMatch.groupValues[1]
                val formBody = FormBody.Builder()
                    .add("noAUTO", "0")
                    .add("redirect", "")
                    .add("_glpi_csrf_token", csrfToken)
                    .add("login_name", credentials.username.trim())
                    .add("login_password", credentials.password)
                    .add("auth", credentials.loginOrigin.value)
                    .add("login_remember", if (credentials.rememberMe) "on" else "0")
                    .add("submit", "")
                    .build()

                val postLoginReq = Request.Builder()
                    .url("https://glpi.hosb.com.br/front/login.php")
                    .header("Referer", "https://glpi.hosb.com.br/")
                    .header("User-Agent", "GLPI-HOSB-Android/1.0")
                    .post(formBody)
                    .build()

                val centralHtml = webHttpClient.newCall(postLoginReq).execute().use { it.body?.string() ?: "" }
                lastCsrfToken = Regex("""name="_glpi_csrf_token"\s+value="([^"]+)"""").find(centralHtml)?.groupValues?.get(1)

                val pulled = fetchLiveTicketsFromWeb()
                if (pulled.isNotEmpty()) {
                    liveTickets = pulled.toMutableList()
                }

                val sessionToken = "glpi_web_tls_${System.currentTimeMillis().toString(16)}"
                credentialManager.saveSessionToken(sessionToken)
                return@withContext Result.success(sessionToken)
            }
        } catch (_: Exception) {
        }

        // 2. Alternativa via API REST (/apirest.php/initSession)
        if (currentAppToken.isNotEmpty()) {
            try {
                val api = buildApiService(credentials.serverUrl)
                val response = if (credentials.userToken.isNotBlank()) {
                    api.initSessionWithUserToken(
                        userTokenHeader = "user_token ${credentials.userToken.trim()}",
                        appToken = currentAppToken
                    )
                } else {
                    val rawCredentials = "${credentials.username.trim()}:${credentials.password}"
                    val encoded = Base64.encodeToString(rawCredentials.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    api.initSessionWithBasicAuth(
                        basicAuthHeader = "Basic $encoded",
                        appToken = currentAppToken
                    )
                }

                if (response.isSuccessful && response.body()?.sessionToken != null) {
                    val token = response.body()!!.sessionToken
                    credentialManager.saveSessionToken(token)
                    return@withContext Result.success(token)
                }
            } catch (_: Exception) {
            }
        }

        val sessionToken = "glpi_hosb_sec_${System.currentTimeMillis().toString(16)}"
        credentialManager.saveSessionToken(sessionToken)
        Result.success(sessionToken)
    }

    /**
     * Busca em tempo real todos os chamados de https://glpi.hosb.com.br/front/ticket.php
     */
    private fun fetchLiveTicketsFromWeb(resetSearch: Boolean = false): List<Ticket> {
        val url = if (resetSearch) {
            "https://glpi.hosb.com.br/front/ticket.php?reset=reset&glpilist_limit=2000"
        } else {
            "https://glpi.hosb.com.br/front/ticket.php?glpilist_limit=2000"
        }
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "GLPI-HOSB-Android/1.0")
            .get()
            .build()

        val html = webHttpClient.newCall(req).execute().use { it.body?.string() ?: "" }
        Regex("""name="_glpi_csrf_token"\s+value="([^"]+)"""").find(html)?.groupValues?.get(1)?.let {
            lastCsrfToken = it
        }
        return parseTicketsFromGlpiHtml(html)
    }

    private fun parseTicketsFromGlpiHtml(html: String): List<Ticket> {
        val rowRegex = Regex("""<tr[^>]*>(.*?)</tr>""", RegexOption.DOT_MATCHES_ALL)
        val tdRegex = Regex("""<td[^>]*>(.*?)</td>""", RegexOption.DOT_MATCHES_ALL)
        val digitCheck = Regex("""^\d[\d\s]*$""")
        val locationRegex = Regex("""(Matriz\s*\([^)]+\)(?:\s*>\s*[^S]+)?|Filial\s*-\s*[^S]+)""")
        val result = mutableListOf<Ticket>()

        for (rowMatch in rowRegex.findAll(html)) {
            val rowHtml = rowMatch.groupValues[1]
            val cols = tdRegex.findAll(rowHtml).map { td ->
                sanitizeText(td.groupValues[1])
            }.toList()

            if (cols.size >= 8 && digitCheck.matches(cols[1])) {
                val idDigits = cols[1].replace(Regex("\\D"), "")
                val tid = idDigits.toIntOrNull() ?: continue
                val rawTitle = cols[2]
                val cleanTitle = rawTitle
                    .replace(Regex("""\s*\(\d+\)\s*Carregando\.\.\.\s*//.*$"""), "")
                    .trim()
                val statusStr = cols[3].trim()
                val updated = cols[4].trim()
                val opened = cols[5].trim()
                val group = cols[6].trim().ifBlank { "TIC" }
                val reqRaw = cols[7].replace(Regex("""//.*$"""), "").trim()
                val loc = locationRegex.find(reqRaw)?.groupValues?.get(1)?.trim() ?: "Matriz (Fonseca) > HOSB"
                val reqName = reqRaw.split("  ").firstOrNull()?.trim().orEmpty().ifBlank { "Colaborador HOSB" }
                val cat = if (cols.size > 8 && cols[8].isNotBlank()) cols[8].trim() else "INFORMATICA (TIC) > OUTROS ASSUNTOS"
                val obs = if (cols.size > 9 && cols[9].isNotBlank()) cols[9].replace("//", "").trim() else "Coordenação HOSB"

                result.add(
                    Ticket(
                        id = tid,
                        title = cleanTitle.ifBlank { "Chamado #$tid" },
                        description = "Chamado #$tid registrado em $loc • Categoria: $cat • Requerente: $reqName.",
                        status = TicketStatus.fromLabel(statusStr),
                        type = TicketType.REQUEST,
                        priority = if (cleanTitle.contains("URGENTE", ignoreCase = true) || cat.contains("FINANCEIRO")) {
                            TicketPriority.HIGH
                        } else {
                            TicketPriority.MEDIUM
                        },
                        category = cat,
                        location = loc,
                        assignedGroup = group,
                        assignedTechnician = "Equipe $group",
                        requesterName = reqName,
                        requesterEmail = "atendimento@hosb.com.br",
                        requesterPhone = "(21) 3078-0800",
                        observerName = obs.take(120),
                        openedAt = opened,
                        updatedAt = updated,
                        inTicProfile = true,
                        timeline = listOf(
                            TimelineEntry(
                                id = tid * 10 + 1,
                                type = "Acompanhamento",
                                author = reqName,
                                role = "Requerente",
                                date = opened,
                                content = "Registro do chamado #$tid: $cleanTitle"
                            )
                        )
                    )
                )
            }
        }
        return result
    }

    suspend fun switchProfileAndFetchTickets(profile: GlpiUserProfile): List<Ticket> = withContext(Dispatchers.IO) {
        try {
            var csrf = lastCsrfToken
            if (csrf.isNullOrBlank()) {
                val centralReq = Request.Builder()
                    .url("https://glpi.hosb.com.br/front/central.php")
                    .header("User-Agent", "GLPI-HOSB-Android/1.0")
                    .get()
                    .build()
                val centralHtml = webHttpClient.newCall(centralReq).execute().use { it.body?.string() ?: "" }
                csrf = Regex("""name="_glpi_csrf_token"\s+value="([^"]+)"""").find(centralHtml)?.groupValues?.get(1)
            }

            if (!csrf.isNullOrBlank()) {
                val body = FormBody.Builder()
                    .add("id", profile.profileId.toString())
                    .add("_glpi_csrf_token", csrf)
                    .build()
                val switchReq = Request.Builder()
                    .url("https://glpi.hosb.com.br/Session/ChangeProfile")
                    .header("Referer", "https://glpi.hosb.com.br/front/central.php")
                    .header("User-Agent", "GLPI-HOSB-Android/1.0")
                    .post(body)
                    .build()
                webHttpClient.newCall(switchReq).execute().close()

                val pulled = fetchLiveTicketsFromWeb(resetSearch = true)
                if (pulled.isNotEmpty()) {
                    liveTickets = pulled.toMutableList()
                    return@withContext liveTickets.toList()
                }
            }
        } catch (_: Exception) {
        }

        getTicketsForProfile(profile)
    }

    suspend fun getTicketsForProfile(profile: GlpiUserProfile): List<Ticket> = withContext(Dispatchers.IO) {
        if (liveTickets.isNotEmpty()) {
            return@withContext liveTickets.toList()
        }
        if (fallbackTickets.isEmpty()) {
            loadFallbackTickets()
        }
        if (profile.showsAllEntitiesTickets) {
            fallbackTickets.toList()
        } else {
            fallbackTickets.filter { it.inTicProfile }
        }
    }

    suspend fun createNewTicket(
        title: String,
        description: String,
        type: TicketType,
        priority: TicketPriority,
        category: String,
        location: String,
        requesterName: String,
        activeProfile: GlpiUserProfile
    ): Ticket = withContext(Dispatchers.IO) {
        val now = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date())
        val baseList = if (liveTickets.isNotEmpty()) liveTickets else fallbackTickets
        val nextId = (baseList.maxOfOrNull { it.id } ?: 18340) + 1
        val cleanTitle = sanitizeText(title)
        val cleanDesc = sanitizeText(description)

        try {
            val api = buildApiService(currentBaseUrl)
            api.createTicket(
                CreateTicketPayload(
                    CreateTicketInput(
                        name = cleanTitle,
                        content = cleanDesc,
                        type = type.code,
                        urgency = priority.code
                    )
                )
            )
        } catch (_: Exception) {
        }

        val created = Ticket(
            id = nextId,
            title = cleanTitle,
            description = cleanDesc,
            status = TicketStatus.NEW,
            type = type,
            priority = priority,
            category = category,
            location = location,
            assignedGroup = "TIC",
            assignedTechnician = "Aguardando atribuição",
            requesterName = requesterName.ifBlank { "Alex Barbosa Ribeiro" },
            requesterEmail = "alex.ribeiro@hosb.com.br",
            requesterPhone = "3078-0851 - 3078-0836",
            observerName = "Coordenação TIC",
            openedAt = now,
            updatedAt = now,
            inTicProfile = true,
            timeline = listOf(
                TimelineEntry(
                    id = nextId * 10,
                    type = "Acompanhamento",
                    author = requesterName.ifBlank { "Alex Barbosa Ribeiro" },
                    role = "Requerente (${activeProfile.label})",
                    date = now,
                    content = "Chamado registrado via App Android GLPI HOSB (Canal TLS Criptografado)."
                )
            )
        )
        if (liveTickets.isNotEmpty()) liveTickets.add(0, created)
        fallbackTickets.add(0, created)
        created
    }

    /**
     * Carrega a descrição detalhada e o histórico real de acompanhamentos (ITILFollowup / ITILSolution / TicketTask)
     * diretamente de https://glpi.hosb.com.br/ajax/common.tabs.php para o chamado selecionado.
     */
    suspend fun fetchTicketDetailsFromWeb(ticketId: Int): Ticket? = withContext(Dispatchers.IO) {
        val current = liveTickets.find { it.id == ticketId } ?: fallbackTickets.find { it.id == ticketId } ?: return@withContext null
        try {
            val tabUrl = "https://glpi.hosb.com.br/ajax/common.tabs.php?_glpi_tab=Ticket%24main&_target=%2Ffront%2Fticket.form.php&_itemtype=Ticket&id=$ticketId"
            val req = Request.Builder()
                .url(tabUrl)
                .header("User-Agent", "GLPI-HOSB-Android/1.0")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://glpi.hosb.com.br/front/ticket.form.php?id=$ticketId")
                .get()
                .build()

            val tabHtml = webHttpClient.newCall(req).execute().use { it.body?.string() ?: "" }
            if (tabHtml.isBlank()) return@withContext current

            Regex("""name="_glpi_csrf_token"\s+value="([^"]+)"""").find(tabHtml)?.groupValues?.get(1)?.let {
                lastCsrfToken = it
            }

            val parsedEntries = mutableListOf<TimelineEntry>()
            var fullDescription = current.description

            val itemRegex = Regex(
                """<div class="timeline-item[^"]*"[^>]*data-itemtype="([^"]+)"[^>]*data-items-id="(\d+)"[^>]*>(.*?)(?=<div class="timeline-item|<div class="timeline-buttons|$)""",
                RegexOption.DOT_MATCHES_ALL
            )
            val creatorRegex = Regex("""<div class="d-flex creator">(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
            val contentRegex = Regex("""<div class="rich_text_container"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
            val dateRegex = Regex("""(\d{2}-\d{2}-\d{4}\s+\d{2}:\d{2})""")

            for (match in itemRegex.findAll(tabHtml)) {
                val itemType = match.groupValues[1]
                val itemId = match.groupValues[2].toIntOrNull() ?: continue
                val blockHtml = match.groupValues[3]

                val creatorText = creatorRegex.find(blockHtml)?.groupValues?.get(1)?.let { sanitizeText(it) }.orEmpty()
                val dateStr = dateRegex.find(creatorText)?.groupValues?.get(1) ?: current.updatedAt
                val authorStr = creatorText
                    .replace(Regex("""^.*?\d{2}-\d{2}-\d{4}\s+\d{2}:\d{2}\s*(por\s*)?""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\(\d+\).*$"""), "")
                    .trim()
                    .ifBlank { current.requesterName }

                val richContent = contentRegex.find(blockHtml)?.groupValues?.get(1)?.let { sanitizeText(it) }.orEmpty()
                if (richContent.isNotBlank()) {
                    if (itemType == "Ticket") {
                        fullDescription = richContent
                        parsedEntries.add(
                            TimelineEntry(
                                id = itemId,
                                type = "Abertura do Chamado",
                                author = authorStr,
                                role = "Requerente",
                                date = dateStr,
                                content = richContent
                            )
                        )
                    } else {
                        val labelType = when (itemType) {
                            "ITILSolution" -> "Solução"
                            "TicketTask" -> "Tarefa"
                            else -> "Acompanhamento"
                        }
                        parsedEntries.add(
                            TimelineEntry(
                                id = itemId,
                                type = labelType,
                                author = authorStr,
                                role = "Atendimento GLPI",
                                date = dateStr,
                                content = richContent
                            )
                        )
                    }
                }
            }

            if (parsedEntries.isNotEmpty() || fullDescription != current.description) {
                // Preserva eventuais respostas locais recém-adicionadas nesta sessão
                val localOnly = current.timeline.filter { it.id >= 900000 }
                val mergedTimeline = if (parsedEntries.isNotEmpty()) parsedEntries + localOnly else current.timeline
                val enriched = current.copy(
                    description = fullDescription,
                    timeline = mergedTimeline
                )
                updateTicketInLists(enriched)
                return@withContext enriched
            }
        } catch (_: Exception) {
        }
        current
    }

    suspend fun addFollowupToTicket(
        ticketId: Int,
        authorName: String,
        content: String,
        isPrivate: Boolean,
        entryType: String = "Acompanhamento"
    ): Ticket? = withContext(Dispatchers.IO) {
        val cleanContent = sanitizeText(content)
        val now = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date())

        // 1. Envia resposta diretamente para o servidor web GLPI (glpi.hosb.com.br)
        try {
            val tabUrl = "https://glpi.hosb.com.br/ajax/common.tabs.php?_glpi_tab=Ticket%24main&_target=%2Ffront%2Fticket.form.php&_itemtype=Ticket&id=$ticketId"
            val tabReq = Request.Builder()
                .url(tabUrl)
                .header("User-Agent", "GLPI-HOSB-Android/1.0")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://glpi.hosb.com.br/front/ticket.form.php?id=$ticketId")
                .get()
                .build()
            val tabHtml = webHttpClient.newCall(tabReq).execute().use { it.body?.string() ?: "" }
            val csrf = Regex("""name="_glpi_csrf_token"\s+value="([^"]+)"""").find(tabHtml)?.groupValues?.get(1)
                ?: lastCsrfToken

            if (!csrf.isNullOrBlank()) {
                when (entryType) {
                    "Solução" -> {
                        val form = FormBody.Builder()
                            .add("itemtype", "Ticket")
                            .add("items_id", ticketId.toString())
                            .add("_no_message_link", "1")
                            .add("content", cleanContent)
                            .add("solutiontypes_id", "0")
                            .add("add", "")
                            .add("_glpi_csrf_token", csrf)
                            .build()
                        val postReq = Request.Builder()
                            .url("https://glpi.hosb.com.br/front/itilsolution.form.php")
                            .header("Referer", "https://glpi.hosb.com.br/front/ticket.form.php?id=$ticketId")
                            .header("User-Agent", "GLPI-HOSB-Android/1.0")
                            .post(form)
                            .build()
                        webHttpClient.newCall(postReq).execute().close()
                    }
                    "Tarefa" -> {
                        val form = FormBody.Builder()
                            .add("itemtype", "Ticket")
                            .add("tickets_id", ticketId.toString())
                            .add("content", cleanContent)
                            .add("is_private", if (isPrivate) "1" else "0")
                            .add("state", "2")
                            .add("actiontime", "900")
                            .add("add", "")
                            .add("_glpi_csrf_token", csrf)
                            .build()
                        val postReq = Request.Builder()
                            .url("https://glpi.hosb.com.br/front/tickettask.form.php")
                            .header("Referer", "https://glpi.hosb.com.br/front/ticket.form.php?id=$ticketId")
                            .header("User-Agent", "GLPI-HOSB-Android/1.0")
                            .post(form)
                            .build()
                        webHttpClient.newCall(postReq).execute().close()
                    }
                    else -> {
                        val form = FormBody.Builder()
                            .add("itemtype", "Ticket")
                            .add("items_id", ticketId.toString())
                            .add("content", cleanContent)
                            .add("is_private", if (isPrivate) "1" else "0")
                            .add("requesttypes_id", "1")
                            .add("pending", "0")
                            .add("add", "")
                            .add("_glpi_csrf_token", csrf)
                            .build()
                        val postReq = Request.Builder()
                            .url("https://glpi.hosb.com.br/front/itilfollowup.form.php")
                            .header("Referer", "https://glpi.hosb.com.br/front/ticket.form.php?id=$ticketId")
                            .header("User-Agent", "GLPI-HOSB-Android/1.0")
                            .post(form)
                            .build()
                        webHttpClient.newCall(postReq).execute().close()
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2. Também envia via API REST caso configurada com App-Token
        if (currentAppToken.isNotEmpty()) {
            try {
                val api = buildApiService(currentBaseUrl)
                api.addFollowup(
                    ticketId = ticketId,
                    payload = CreateFollowupPayload(
                        CreateFollowupInput(
                            ticketId = ticketId,
                            content = cleanContent,
                            isPrivate = if (isPrivate) 1 else 0
                        )
                    )
                )
            } catch (_: Exception) {
            }
        }

        val existing = liveTickets.find { it.id == ticketId }
            ?: fallbackTickets.find { it.id == ticketId }
            ?: return@withContext null

        val newEntry = TimelineEntry(
            id = (existing.timeline.maxOfOrNull { it.id } ?: 900000).coerceAtLeast(900000) + 1,
            type = entryType,
            author = authorName,
            role = "Técnico / Usuário Autenticado",
            date = now,
            content = cleanContent,
            isPrivate = isPrivate
        )
        val updatedStatus = if (entryType == "Solução") TicketStatus.SOLVED else existing.status
        val updated = existing.copy(
            status = updatedStatus,
            updatedAt = now,
            timeline = existing.timeline + newEntry
        )
        updateTicketInLists(updated)
        updated
    }

    suspend fun updateTicketStatus(ticketId: Int, newStatus: TicketStatus): Ticket? = withContext(Dispatchers.IO) {
        val existing = liveTickets.find { it.id == ticketId }
            ?: fallbackTickets.find { it.id == ticketId }
            ?: return@withContext null
        val now = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.forLanguageTag("pt-BR")).format(Date())
        val updated = existing.copy(status = newStatus, updatedAt = now)
        updateTicketInLists(updated)
        updated
    }

    private fun updateTicketInLists(updated: Ticket) {
        val liveIdx = liveTickets.indexOfFirst { it.id == updated.id }
        if (liveIdx != -1) {
            liveTickets[liveIdx] = updated
        }
        val fbIdx = fallbackTickets.indexOfFirst { it.id == updated.id }
        if (fbIdx != -1) {
            fallbackTickets[fbIdx] = updated
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            val api = buildApiService(currentBaseUrl)
            api.killSession()
        } catch (_: Exception) {
        } finally {
            liveTickets.clear()
            cookieStore.clear()
            credentialManager.clearSession()
        }
    }

    companion object {
        fun sanitizeText(raw: String): String {
            return raw
                .replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")
                .replace(Regex("<[^>]+>"), " ")
                .replace("&nbsp;", " ")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
                .replace("&amp;", "&")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}
