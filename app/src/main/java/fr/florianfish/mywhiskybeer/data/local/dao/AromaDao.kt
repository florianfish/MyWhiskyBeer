package fr.florianfish.mywhiskybeer.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import fr.florianfish.mywhiskybeer.data.local.entity.AromaEntity
import fr.florianfish.mywhiskybeer.data.local.entity.ProductType
import kotlinx.coroutines.flow.Flow

@Dao
interface AromaDao {
    @Query("SELECT * FROM aroma WHERE productType = :type ORDER BY family, name")
    fun observeByType(type: ProductType): Flow<List<AromaEntity>>
}
