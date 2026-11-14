package fr.florianfish.mywhiskybeer.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType

/** Référentiel d'arômes inséré à la création de la base. Famille → arômes. */
object AromaSeed {

    val whisky: Map<String, List<String>> = mapOf(
        "Fruité" to listOf("Pomme", "Poire", "Agrumes", "Fruits secs", "Fruits rouges", "Fruits tropicaux", "Raisin sec"),
        "Floral" to listOf("Bruyère", "Fleurs blanches", "Herbe coupée", "Miel"),
        "Céréales" to listOf("Malt", "Biscuit", "Pain grillé", "Porridge"),
        "Boisé" to listOf("Vanille", "Chêne", "Caramel", "Toffee", "Noix de coco"),
        "Épicé" to listOf("Poivre", "Cannelle", "Clou de girofle", "Gingembre", "Muscade"),
        "Tourbé / fumé" to listOf("Tourbe", "Feu de bois", "Cendre", "Goudron", "Médicinal"),
        "Sherry" to listOf("Chocolat", "Café", "Cuir", "Tabac", "Noix"),
        "Marin" to listOf("Iode", "Sel", "Algues"),
    )

    val beer: Map<String, List<String>> = mapOf(
        "Houblon" to listOf("Agrumes", "Pamplemousse", "Résineux", "Fruits tropicaux", "Herbacé", "Floral"),
        "Malt" to listOf("Pain", "Biscuit", "Caramel", "Miel", "Torréfié", "Café", "Chocolat"),
        "Levure" to listOf("Banane", "Clou de girofle", "Poivre", "Funky / Brett"),
        "Fruité" to listOf("Fruits rouges", "Fruits à noyau", "Agrumes"),
        "Acidité" to listOf("Lactique", "Acétique", "Vineux"),
        "Autres" to listOf("Vanille", "Boisé", "Épices", "Fumé"),
    )

    val callback = object : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            insertAll(db, ProductType.WHISKY, whisky)
            insertAll(db, ProductType.BEER, beer)
        }
    }

    private fun insertAll(db: SupportSQLiteDatabase, type: ProductType, aromas: Map<String, List<String>>) {
        aromas.forEach { (family, names) ->
            names.forEach { name ->
                db.execSQL(
                    "INSERT INTO aroma (productType, family, name) VALUES (?, ?, ?)",
                    arrayOf(type.name, family, name),
                )
            }
        }
    }
}
