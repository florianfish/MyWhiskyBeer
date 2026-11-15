package fr.florianfish.mywhiskybeer.ui.navigation

import kotlinx.serialization.Serializable

// Routes typées (Navigation Compose 2.8+)

@Serializable data object HomeRoute
@Serializable data object ProductsRoute
@Serializable data object CellarRoute

/**
 * productId null = nouvelle dégustation d'un nouveau produit.
 * tastingId renseigné = modification d'une dégustation existante.
 */
@Serializable data class TastingEditRoute(val productId: Long? = null, val tastingId: Long? = null)

@Serializable data class ProductDetailRoute(val productId: Long)

/**
 * Ajout ou modification d'une bouteille en cave (stock, pas dégustation).
 * productId = produit existant ; wishlist = ouvrir directement en « à acheter ».
 */
@Serializable data class BottleEditRoute(val productId: Long? = null, val wishlist: Boolean = false)

@Serializable data object SettingsRoute

/** Modification des informations d'un produit (pas de ses dégustations). */
@Serializable data class ProductEditRoute(val productId: Long)
