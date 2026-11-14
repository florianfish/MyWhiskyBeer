package fr.florianfish.mywhiskybeer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Titres en serif (esprit étiquette), texte courant en sans-serif pour la lisibilité.
// TODO : embarquer une serif à caractère (ex. Fraunces ou Playfair Display) dans res/font.
private val Display = FontFamily.Serif
private val Body = FontFamily.SansSerif

private val base = Typography()

val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    headlineLarge = base.headlineLarge.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    headlineMedium = base.headlineMedium.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    headlineSmall = base.headlineSmall.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    titleLarge = base.titleLarge.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontFamily = Body, fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontFamily = Body),
    bodyLarge = base.bodyLarge.copy(fontFamily = Body),
    bodyMedium = base.bodyMedium.copy(fontFamily = Body),
    bodySmall = base.bodySmall.copy(fontFamily = Body),
    labelLarge = base.labelLarge.copy(fontFamily = Body, fontWeight = FontWeight.SemiBold),
    labelMedium = base.labelMedium.copy(fontFamily = Body, letterSpacing = 0.8.sp),
    labelSmall = base.labelSmall.copy(fontFamily = Body, letterSpacing = 0.8.sp),
)

/** Style des notes chiffrées : grandes, en serif. */
val RatingTextStyle = TextStyle(fontFamily = Display, fontWeight = FontWeight.Bold, fontSize = 22.sp)
