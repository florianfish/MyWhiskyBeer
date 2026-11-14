# MyWhiskyBeer

Carnet de dégustation perso — bières & whisky.

Application Android **à usage personnel** pour noter les bières et les whiskies que je goûte, garder une trace de ma cave et retrouver ce que j'ai aimé.

Pas de réseau social, pas de compte : un carnet perso, rapide à remplir, qui fonctionne hors-ligne.

## Principes

- **Saisie en moins de 30 secondes** : sinon je ne noterai pas au bar ou en salon.
- **Hors-ligne d'abord** : tout est stocké localement.
- **Un produit ≠ une dégustation** : je peux goûter la même bouteille plusieurs fois et voir l'évolution de mes notes.
- **Tronc commun + champs spécifiques** : bière et whisky partagent l'essentiel, chacun a ses champs propres. Extensible plus tard (rhum, gin, vin…).

## Stack envisagée

| Besoin | Choix |
|---|---|
| Langage / UI | Kotlin + Jetpack Compose (Material 3) |
| Stockage local | Room (SQLite) |
| Photos | CameraX, fichiers dans le stockage privé de l'app |
| Code-barres / OCR étiquette | ML Kit (sur l'appareil, gratuit) |
| Architecture | MVVM, Hilt pour l'injection, Flow/StateFlow |
| Sauvegarde | Export/import JSON + photos (zip), puis éventuellement synchro auto-hébergée |

## Compiler

Ouvrir le projet dans Android Studio (JDK 17), ou en ligne de commande :

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

minSdk 26, targetSdk 35. Le schéma Room est exporté dans `app/schemas/` (à versionner).

## Structure

```
app/src/main/java/fr/florianfish/mywhiskybeer/
├── data/local/        # Room : entités, DAO, base, référentiel d'arômes
├── di/                # Modules Hilt
├── domain/            # Logique métier (échelles de notes…)
└── ui/                # Compose : thème, composants, écrans, navigation
```

## Documentation

- [`docs/PLAN.md`](docs/PLAN.md) — modèle de données, écrans, roadmap par version
- [`docs/FEATURES.md`](docs/FEATURES.md) — idées de fonctionnalités, priorisées
- [`docs/DESIGN.md`](docs/DESIGN.md) — direction visuelle, couleurs, éléments signature
