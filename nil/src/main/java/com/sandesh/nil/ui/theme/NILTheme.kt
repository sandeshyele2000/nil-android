/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

private val NilTypography = Typography(
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 17.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp)
)

@Composable
fun NILTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val darkScheme = darkColorScheme(
        primary = NILColors.accent(),
        onPrimary = NILColors.textPrimary(),
        primaryContainer = NILColors.surfaceElevated(),
        onPrimaryContainer = NILColors.textPrimary(),
        secondary = NILColors.info(),
        onSecondary = NILColors.textPrimary(),
        tertiary = NILColors.warning(),
        onTertiary = NILColors.bg(),
        background = NILColors.bg(),
        onBackground = NILColors.textPrimary(),
        surface = NILColors.surface(),
        onSurface = NILColors.textPrimary(),
        surfaceVariant = NILColors.surfaceElevated(),
        onSurfaceVariant = NILColors.textSecondary(),
        outline = NILColors.border(),
        error = NILColors.error(),
        onError = NILColors.textPrimary()
    )
    val lightScheme = lightColorScheme(
        primary = NILColors.accent(),
        onPrimary = NILColors.surface(),
        primaryContainer = NILColors.surfaceElevated(),
        onPrimaryContainer = NILColors.textPrimary(),
        secondary = NILColors.info(),
        onSecondary = NILColors.surface(),
        tertiary = NILColors.warning(),
        onTertiary = NILColors.surface(),
        background = NILColors.bg(),
        onBackground = NILColors.textPrimary(),
        surface = NILColors.surface(),
        onSurface = NILColors.textPrimary(),
        surfaceVariant = NILColors.surfaceElevated(),
        onSurfaceVariant = NILColors.textSecondary(),
        outline = NILColors.border(),
        error = NILColors.error(),
        onError = NILColors.surface()
    )
    val colorScheme = if (darkTheme) darkScheme else lightScheme

    CompositionLocalProvider(
        LocalNILSpacing provides NILSpacing()
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NilTypography,
            content = content
        )
    }
}
