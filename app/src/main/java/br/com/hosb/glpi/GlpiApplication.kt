package br.com.hosb.glpi

import android.app.Application
import br.com.hosb.glpi.data.repository.GlpiRepository
import br.com.hosb.glpi.data.security.SecureCredentialManager
import br.com.hosb.glpi.util.GlpiNotificationHelper

class GlpiApplication : Application() {
    lateinit var credentialManager: SecureCredentialManager
        private set

    lateinit var repository: GlpiRepository
        private set

    lateinit var notificationHelper: GlpiNotificationHelper
        private set

    override fun onCreate() {
        super.onCreate()
        credentialManager = SecureCredentialManager(this)
        repository = GlpiRepository(this, credentialManager)
        notificationHelper = GlpiNotificationHelper(this)
    }
}
