# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

MyWhiskyBeer: personal Android app to rate beers and whiskies (Kotlin, Jetpack Compose, Room, Hilt). Offline-only, no account, no backend. UI strings, code comments, commit messages and docs are in French — keep it that way.

## Build

- Gradle wrapper is committed (9.9.1): `./gradlew assembleDebug testDebugUnitTest`. JDK 17, compileSdk 37, targetSdk 35, minSdk 26. Toolchain aligned with MyRadioPlayer.
- Versions live in `gradle/libs.versions.toml`: AGP 9.5 with **built-in Kotlin** (Kotlin 2.4). `org.jetbrains.kotlin.android` is only declared `apply false` in the root build to pin the Kotlin version, never applied in `:app`, and there is no `kotlinOptions` block (jvmTarget follows `compileOptions`). The Compose and serialization compiler plugins use the same Kotlin version. KSP (2.3.x) is no longer tied to the Kotlin version. Hilt ≥ 2.59 is required by AGP 9. Compose `hiltViewModel()` comes from `androidx.hilt:hilt-lifecycle-viewmodel-compose` (`androidx.hilt.lifecycle.viewmodel.compose`).
- Room schemas are exported to `app/schemas/` at build time **and committed**. Any entity change must bump `AppDatabase.version`, add the migration to `AppDatabase.MIGRATIONS`, add a data-preservation test in `MigrationTest`, and commit the new `N.json` — users' notes must never be wiped. On CI (`CI=true`), the `verifierSchemasRoom` task (run before `testDebugUnitTest`) fails if `git status` shows any change under `app/schemas`.
- CI workflows are thin callers of the shared reusable workflows in the public repo `florianfish/Forge-Apk` (`android-build.yml`, `android-release.yml`, called `@main`). Change CI behaviour there, not here.
- `build.yml` runs `assembleDebug testDebugUnitTest` on every push to `main` and every PR, uploads the APK as the `MyWhiskyBeer-apk` artifact, comments the link on PRs, and on `main` replaces the `debug` pre-release: always-current APK at `https://github.com/florianfish/MyWhiskyBeer/releases/download/debug/MyWhiskyBeer-debug.apk`.
- Releases (`release.yml`), same procedure as MyRadioPlayer: push a `v*` tag, or run the workflow manually from `main` with a `type` input (`mineure`/`correctif`/`majeure`). Signing comes from repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; `versionName` = tag without `v`, `versionCode` = run number, read from env vars in `app/build.gradle.kts`.
- Debug builds use the `.debug` applicationId suffix, so a debug APK installs next to the release one (separate data).

## Architecture

Single module `:app`, package `fr.florianfish.mywhiskybeer`.

- `data/local/` — Room. `ProductEntity` is the common trunk (type BEER/WHISKY); `BeerDetailsEntity` / `WhiskyDetailsEntity` hold type-specific fields (1:1, cascade). A product has many `TastingEntity`. Ratings are stored **0..100** for both types and displayed through `domain/RatingScale` (beer /5 by half points, whisky /100). Aromas are a reference table seeded on DB creation from `AromaSeed` (families → names, per type); `TastingAromaCrossRef` stores an intensity 1..3. `CellarItemEntity` (1:1 with product) holds stock/wishlist status, bottle quantity, fill level and purchase info. Schema v2 (`MIGRATION_1_2` adds `cellar_item.quantity`); migrations live in `AppDatabase.MIGRATIONS`. `ProductWriter` creates a product from the shared `domain/ProductForm`.
- `data/photo/PhotoStore` — imports picked/captured photos into `filesDir/photos` (max 1600 px, JPEG 85, EXIF rotation applied). Camera captures go through a `FileProvider` (`${applicationId}.photos`, cache `captures/`).
- `data/ocr/` — label scanning: `LabelReader` runs ML Kit text recognition (bundled model, offline) on the imported photo; `LabelScanner` = capture import + read + `domain/LabelParser` (pure Kotlin heuristics, unit-tested: type by keywords, name = tallest text line, producer from distillery/brewery lines, abv/age regexes, style/category/region/cask). `ProductForm.withGuess` only fills blank fields.
- `data/backup/` — full backup/restore as a `.zip` (`mywhiskybeer.json` + `photos/`), written/read through the Storage Access Framework (`CreateDocument` / `OpenDocument`), so the user picks Google Drive in the system picker: no OAuth client or Google Cloud project. Entities are `@Serializable`; photo paths are stored as file names and remapped on restore; tasting aromas are stored by (type, family, name), not id. Restore reads and validates the whole zip into `cacheDir/restore` before replacing everything in one transaction. Any new table must be added to `BackupData`/`BackupDao`, and a format change bumps `BackupData.FORMAT`.
- `di/` — Hilt modules. ViewModels inject DAOs directly (no repository layer yet); multi-table writes use `db.withTransaction`.
- `ui/` — Compose, one package per screen (`home`, `products`, `cellar`, `tasting`, `product`, `bottle`, `settings`), type-safe navigation routes in `ui/navigation/Routes.kt`. Top-level screens get the outer Scaffold's padding; `TastingEditScreen`, `BottleEditScreen` and `ProductDetailScreen` are full screen with their own Scaffold. Two distinct entry flows: « Déguster » (tasting form: rating, aromas, notes) and, in the cellar, « Bouteille » / « À acheter » (stock form: quantity, status, price, shop, purchase date — no rating). Both share `ui/components/ProductFormSection` (label scan, type, name with duplicate suggestions, details) and `PhotoSection`. The outer FAB is hidden on the cellar tab, which has its own.
- `ui/theme/` — dark-first "cave à la lueur" theme; dynamic color is deliberately off. `ProductType.accent()` gives the type colour (amber whisky, straw beer). See `docs/DESIGN.md`.
- Signature components in `ui/components/`: `GlassGauge` (rating as a filling glass), `PourSlider` (drag on the glass to rate), `AromaWheel` (canvas wheel, tap cycles intensity), `AromaRadar`.

Unit tests (JVM) live in `app/src/test`, including Room migration tests run with **Robolectric** (`data/local/MigrationTest`, `MigrationTestHelper`, `@Config(sdk = [35], application = Application::class)` to bypass Hilt). The schemas are exposed to the helper as assets of the `debug` source set. There are no instrumented tests.
