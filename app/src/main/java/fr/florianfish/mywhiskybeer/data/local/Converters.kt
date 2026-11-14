package fr.florianfish.mywhiskybeer.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun tagsToString(tags: List<String>): String = tags.joinToString(SEPARATOR)

    @TypeConverter
    fun stringToTags(value: String): List<String> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR)

    private companion object {
        /** Séparateur d'unité ASCII : ne peut pas apparaître dans un tag saisi. */
        const val SEPARATOR = "\u001F"
    }
}
