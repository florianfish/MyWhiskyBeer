package fr.florianfish.mywhiskybeer.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import fr.florianfish.mywhiskybeer.data.local.entity.BeerStyleEntity
import fr.florianfish.mywhiskybeer.data.local.entity.BeerStyleUsage
import kotlinx.coroutines.flow.Flow

/** Liste des styles de bière gérée par l'utilisateur. */
@Dao
abstract class BeerStyleDao {

    @Query("SELECT * FROM beer_style ORDER BY position, name")
    abstract fun observeAll(): Flow<List<BeerStyleEntity>>

    /** Nombre de bières par style (styles utilisés seulement). */
    @Query("SELECT style, COUNT(*) AS count FROM beer_details WHERE style IS NOT NULL GROUP BY style")
    abstract fun observeUsage(): Flow<List<BeerStyleUsage>>

    @Query("SELECT COALESCE(MAX(position), -1) FROM beer_style")
    abstract suspend fun maxPosition(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(style: BeerStyleEntity)

    @Query("DELETE FROM beer_style WHERE name = :name")
    abstract suspend fun delete(name: String)

    @Query("SELECT COUNT(*) FROM beer_details WHERE style = :name COLLATE NOCASE")
    abstract suspend fun usageCount(name: String): Int

    @Query("UPDATE beer_details SET style = :replacement WHERE style = :name COLLATE NOCASE")
    abstract suspend fun replaceInBeers(name: String, replacement: String)

    /** Ajoute le style en fin de liste s'il n'y est pas déjà (casse ignorée). */
    @Transaction
    open suspend fun add(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) insert(BeerStyleEntity(trimmed, maxPosition() + 1))
    }

    /**
     * Supprime un style. S'il est utilisé, [replacement] est obligatoire : les bières concernées
     * passent à ce style (ajouté à la liste au besoin). Refusé sinon.
     */
    @Transaction
    open suspend fun remove(name: String, replacement: String?) {
        val used = usageCount(name)
        if (used > 0) {
            val target = replacement?.trim()?.takeIf { it.isNotEmpty() && !it.equals(name, ignoreCase = true) }
            requireNotNull(target) { "Style utilisé par $used bière(s) : choisir un style de remplacement" }
            add(target)
            replaceInBeers(name, target)
        }
        delete(name)
    }
}
