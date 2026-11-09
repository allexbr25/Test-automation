package br.com.hosb.glpi.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import br.com.hosb.glpi.GlpiApplication
import br.com.hosb.glpi.domain.model.GlpiUserProfile
import br.com.hosb.glpi.domain.model.Ticket
import java.util.concurrent.TimeUnit

/**
 * Sincronização em segundo plano: continua verificando novos chamados e movimentações
 * mesmo com o app fechado (sobrevive a reinício do aparelho via WorkManager).
 */
class TicketSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as GlpiApplication
        val credentialManager = app.credentialManager
        if (!credentialManager.isPersistentLoginActive()) return Result.success()
        // Com o app aberto, o monitor em tempo real do ViewModel já cuida das notificações
        if (AppVisibility.isForeground) return Result.success()

        return try {
            val repository = app.repository
            val auth = repository.authenticate(credentialManager.loadStoredCredentials())
            if (auth.isFailure) return Result.retry()

            val latest = repository.pollLatestTicketsFromWeb(GlpiUserProfile.PERFIL_TIC)
            if (latest.isEmpty()) return Result.success()

            val previous = credentialManager.loadTicketSnapshot()
            if (previous.isNotEmpty()) {
                val alerts = mutableListOf<Triple<Int, String, String>>()
                latest.forEach { ticket ->
                    val prev = previous[ticket.id]
                    if (prev == null) {
                        alerts += Triple(
                            ticket.id,
                            "🆕 Novo Chamado #${ticket.id}: ${ticket.title}",
                            "Aberto por ${ticket.requesterName} em ${ticket.location}"
                        )
                    } else if (prev != snapshotValue(ticket)) {
                        alerts += Triple(
                            ticket.id,
                            "🔔 Movimentação no Chamado #${ticket.id}",
                            "${ticket.title} • Status: ${ticket.status.label} (${ticket.updatedAt})"
                        )
                    }
                }
                alerts.take(MAX_INDIVIDUAL_ALERTS).forEach { (id, title, body) ->
                    app.notificationHelper.showNotification(id, title, body)
                }
                if (alerts.size > MAX_INDIVIDUAL_ALERTS) {
                    app.notificationHelper.showNotification(
                        SUMMARY_NOTIFICATION_ID,
                        "GLPI HOSB • ${alerts.size} atualizações",
                        "Há novos chamados e movimentações. Toque para abrir."
                    )
                }
            }
            credentialManager.saveTicketSnapshot(snapshotOf(latest))
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "glpi_hosb_ticket_sync"
        private const val MAX_INDIVIDUAL_ALERTS = 5
        private const val SUMMARY_NOTIFICATION_ID = 999_001

        fun snapshotValue(ticket: Ticket): String = "${ticket.updatedAt};${ticket.status.name}"

        fun snapshotOf(tickets: List<Ticket>): Map<Int, String> =
            tickets.associate { it.id to snapshotValue(it) }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TicketSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
