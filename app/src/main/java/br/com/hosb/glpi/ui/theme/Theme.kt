package br.com.hosb.glpi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta inspirada no GLPI 10 (Tabler UI) e identidade Hospitalar/Corporativa HOSB
val GlpiNavy = Color(0xFF0F2942)
val GlpiPrimaryBlue = Color(0xFF1D63B8)
val GlpiTealAccent = Color(0xFF0D9488)
val GlpiSurfaceLight = Color(0xFFF4F6FA)
val GlpiCardLight = Color(0xFFFFFFFF)
val GlpiSurfaceDark = Color(0xFF0B1521)
val GlpiCardDark = Color(0xFF132235)

// Cores Semânticas para Status ITIL do GLPI
val StatusNewColor = Color(0xFF16A34A)       // Novo
val StatusAssignedColor = Color(0xFF2563EB)  // Em atendimento (atribuído)
val StatusPlannedColor = Color(0xFF0891B2)   // Planejado
val StatusPendingColor = Color(0xFFD97706)   // Pendente
val StatusSolvedColor = Color(0xFF059669)    // Solucionado
val StatusClosedColor = Color(0xFF475569)    // Fechado

private val LightColors = lightColorScheme(
    primary = GlpiPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDbeafe),
    onPrimaryContainer = GlpiNavy,
    secondary = GlpiTealAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    background = GlpiSurfaceLight,
    onBackground = Color(0xFF0F172A),
    surface = GlpiCardLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF082F49),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    background = GlpiSurfaceDark,
    onBackground = Color(0xFFF1F5F9),
    surface = GlpiCardDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155)
)

@Composable
fun GlpiHosbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
