package fr.florianfish.mywhiskybeer.data.local.entity

/** Type de produit. Extensible plus tard (rhum, gin…). */
enum class ProductType { BEER, WHISKY }

enum class PeatLevel { NONE, LIGHT, MEDIUM, HEAVY }

enum class CellarStatus { IN_STOCK, OPENED, EMPTY, WISHLIST }
