package br.com.hosb.glpi.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import br.com.hosb.glpi.domain.model.AuthCredentials
import br.com.hosb.glpi.domain.model.LoginOrigin

/**
 * Gerenciador de Credenciais Seguro utilizando Android Keystore (AES-256-GCM)
 * via EncryptedSharedPreferences.
 *
 * Boas práticas aplicadas:
 * - Chave mestra gerada e protegida por hardware (StrongBox / TEE quando disponível).
 * - Nenhuma senha, App-Token ou Session-Token é gravado em texto puro no disco.
 * - Suporte a expiração automática de sessão e limpeza criptográfica no logout.
 */
class SecureCredentialManager(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    @Volatile
    var activeSessionToken: String? = null
        private set

    fun saveSessionToken(token: String) {
        activeSessionToken = token
        securePrefs.edit()
            .putString(KEY_SESSION_TOKEN, token)
            .putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getValidSessionToken(): String? {
        val inMemory = activeSessionToken
        if (!inMemory.isNullOrBlank()) return inMemory

        val storedToken = securePrefs.getString(KEY_SESSION_TOKEN, null) ?: return null
        val timestamp = securePrefs.getLong(KEY_SESSION_TIMESTAMP, 0L)
        val isExpired = (System.currentTimeMillis() - timestamp) > SESSION_MAX_TTL_MS
        if (isExpired) {
            clearSession()
            return null
        }
        activeSessionToken = storedToken
        return storedToken
    }

    fun saveCredentials(credentials: AuthCredentials) {
        val editor = securePrefs.edit()
            .putString(KEY_SERVER_URL, sanitizeServerUrl(credentials.serverUrl))
            .putString(KEY_APP_TOKEN, credentials.appToken.trim())
            .putString(KEY_LOGIN_ORIGIN, credentials.loginOrigin.value)
            .putBoolean(KEY_REMEMBER_ME, credentials.rememberMe)
            .putBoolean(KEY_BIOMETRIC_ENABLED, credentials.biometricEnabled)

        if (credentials.rememberMe) {
            editor.putString(KEY_USERNAME, credentials.username.trim())
            editor.putString(KEY_USER_TOKEN, credentials.userToken.trim())
        } else {
            editor.remove(KEY_USERNAME)
            editor.remove(KEY_USER_TOKEN)
        }
        editor.apply()
    }

    fun loadCredentials(): AuthCredentials {
        val originValue = securePrefs.getString(KEY_LOGIN_ORIGIN, LoginOrigin.LOCAL.value)
        val origin = LoginOrigin.entries.find { it.value == originValue } ?: LoginOrigin.LOCAL

        return AuthCredentials(
            serverUrl = securePrefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL,
            username = securePrefs.getString(KEY_USERNAME, "") ?: "",
            password = "", // Senha nunca retorna preenchida em memória após reinício por segurança
            appToken = securePrefs.getString(KEY_APP_TOKEN, "") ?: "",
            userToken = securePrefs.getString(KEY_USER_TOKEN, "") ?: "",
            loginOrigin = origin,
            rememberMe = securePrefs.getBoolean(KEY_REMEMBER_ME, true),
            biometricEnabled = securePrefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)
        )
    }

    fun clearSession() {
        activeSessionToken = null
        securePrefs.edit()
            .remove(KEY_SESSION_TOKEN)
            .remove(KEY_SESSION_TIMESTAMP)
            .apply()
    }

    fun wipeAllCredentials() {
        activeSessionToken = null
        securePrefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILE_NAME = "glpi_hosb_encrypted_store"
        private const val DEFAULT_SERVER_URL = "https://glpi.hosb.com.br/apirest.php/"
        private const val SESSION_MAX_TTL_MS = 8 * 60 * 60 * 1000L // 8 horas

        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USERNAME = "username"
        private const val KEY_APP_TOKEN = "app_token"
        private const val KEY_USER_TOKEN = "user_token"
        private const val KEY_LOGIN_ORIGIN = "login_origin"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_SESSION_TIMESTAMP = "session_timestamp"

        fun sanitizeServerUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            if (trimmed.isEmpty()) return DEFAULT_SERVER_URL
            val httpsEnforced = when {
                trimmed.startsWith("http://", ignoreCase = true) ->
                    "https://" + trimmed.removePrefix("http://").removePrefix("HTTP://")
                !trimmed.startsWith("https://", ignoreCase = true) ->
                    "https://$trimmed"
                else -> trimmed
            }
            return if (httpsEnforced.endsWith("/")) httpsEnforced else "$httpsEnforced/"
        }
    }
}
