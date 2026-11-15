# Routes de navigation sérialisées (kotlinx.serialization)
-keepclassmembers @kotlinx.serialization.Serializable class fr.florianfish.mywhiskybeer.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# ML Kit (scanner de codes-barres, OCR des étiquettes) : ses bibliothèques n'embarquent pas toutes les
# règles R8 nécessaires. Avec R8 en mode complet (défaut d'AGP 9), des classes chargées par réflexion
# sont supprimées et GmsBarcodeScanning.getClient() plante en release (les builds de debug, non
# minifiés, fonctionnent). https://github.com/googlesamples/mlkit/issues/1018
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_** { *; }
# Composants ML Kit enregistrés via les registrars de firebase-components (instanciés par réflexion)
-keep class * implements com.google.firebase.components.ComponentRegistrar { void <init>(); }
-keep,allowshrinking interface com.google.firebase.components.ComponentRegistrar
-dontwarn com.google.firebase.components.Component$Instantiation
-dontwarn com.google.firebase.components.Component$ComponentType
