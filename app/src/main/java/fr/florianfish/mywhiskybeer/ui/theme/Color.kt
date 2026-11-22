package fr.florianfish.mywhiskybeer.ui.theme

import androidx.compose.ui.graphics.Color

// Direction « cave à la lueur » : sombre et chaud, deux accents liquides.

// Mode sombre (par défaut)
val Cave = Color(0xFF17120E)            // fond : bois brûlé
val CaveSurface = Color(0xFF211A14)
val CaveSurfaceHigh = Color(0xFF2E251D)
val Parchment = Color(0xFFF3E6D3)       // texte : papier d'étiquette
val ParchmentMuted = Color(0xFFBFAE98)
val Outline = Color(0xFF6B5A4A)

val Amber = Color(0xFFE0A458)           // whisky
val OnAmber = Color(0xFF2A1700)
val Straw = Color(0xFFE8C468)           // bière
val OnStraw = Color(0xFF2B2000)
val Copper = Color(0xFFC9704A)          // accents ponctuels (alambic)
val OnCopper = Color(0xFF2B0E02)
// Cuir : sélection neutre (onglet actif, filtre, bouton tonal), sans évoquer un type
val Leather = Color(0xFF45342A)
val OnLeather = Color(0xFFF6DCC0)

// Mode clair
val Paper = Color(0xFFFAF4EA)
val PaperSurface = Color(0xFFFFFBF5)
val PaperSurfaceHigh = Color(0xFFF1E6D6)
val Ink = Color(0xFF241B14)
val InkMuted = Color(0xFF6B5A4A)
val AmberDeep = Color(0xFF9A5418)
val StrawDeep = Color(0xFF7A5E00)
val CopperDeep = Color(0xFF9C4A26)
val LeatherLight = Color(0xFFE9D6BF)
val OnLeatherLight = Color(0xFF2E1F14)

// Liquides des jauges (verre, bouteille) en mode clair : les accents foncés, faits pour
// le texte, donnent un liquide terne (bière kaki). En sombre, liquide = accent.
val AmberLiquid = Color(0xFFC77A2E)
val StrawLiquid = Color(0xFFD9A62A)
val Foam = Color(0xFFFFF6E3)            // mousse : toujours crème, jamais encre

val ErrorDark = Color(0xFFFFB4AB)
val ErrorLight = Color(0xFFBA1A1A)
