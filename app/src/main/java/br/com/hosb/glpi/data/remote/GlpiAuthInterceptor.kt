package br.com.hosb.glpi.data.remote

import br.com.hosb.glpi.data.security.SecureCredentialManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Interceptor de segurança para todas as chamadas à API REST do GLPI (glpi.hosb.com.br).
 *
 * Responsabilidades:
 * 1. Garante que apenas conexões HTTPS sejam utilizadas (rejeita downgrade para HTTP).
 * 2. Injeta automaticamente os cabeçalhos `App-Token` e `Session-Token` quando disponíveis.
 * 3. Detecta respostas HTTP 401 (sessão expirada/inválida) e invalida o token local imediatamente.
 */
class GlpiAuthInterceptor(
    private val credentialManager: SecureCredentialManager,
    private val appTokenProvider: () -> String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (!originalRequest.isHttps) {
            throw IOException("Conexão bloqueada por política de segurança: apenas HTTPS é permitido para o GLPI HOSB.")
        }

        val requestBuilder = originalRequest.newBuilder()
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        val appToken = appTokenProvider().trim()
        if (appToken.isNotEmpty() && originalRequest.header("App-Token") == null) {
            requestBuilder.header("App-Token", appToken)
        }

        val sessionToken = credentialManager.getValidSessionToken()
        if (!sessionToken.isNullOrBlank() && originalRequest.header("Session-Token") == null) {
            requestBuilder.header("Session-Token", sessionToken)
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401) {
            credentialManager.clearSession()
        }

        return response
    }
}
