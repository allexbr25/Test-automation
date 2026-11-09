package br.com.hosb.glpi

import android.app.Application
import br.com.hosb.glpi.data.repository.GlpiRepository
import br.com.hosb.glpi.data.security.SecureCredentialManager

class GlpiApplication : Application() {
    lateinit var credentialManager: SecureCredentialManager
        private set

    lateinit var repository: GlpiRepository
        private set

    override fun onCreate() {
        super.onCreate()
        credentialManager = SecureCredentialManager(this)
        repository = GlpiRepository(this, credentialManager)
    }
}
