# Routes de navigation sérialisées (kotlinx.serialization)
-keepclassmembers @kotlinx.serialization.Serializable class fr.florianfish.mywhiskybeer.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
