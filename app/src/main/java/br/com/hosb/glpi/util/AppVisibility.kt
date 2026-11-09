package br.com.hosb.glpi.util

/** Estado global de visibilidade do app (primeiro plano / segundo plano). */
object AppVisibility {
    @Volatile
    var isForeground: Boolean = false

    /** Evita bloquear o app ao voltar do seletor de arquivos/câmera aberto pelo próprio app. */
    @Volatile
    var suppressNextLock: Boolean = false
}
