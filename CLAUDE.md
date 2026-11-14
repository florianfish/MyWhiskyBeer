# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

MyWhiskyBeer: personal Android app to rate beers and whiskies (Kotlin, Jetpack Compose, Room, Hilt). Offline-only, no account, no backend. UI strings, code comments, commit messages and docs are in French — keep it that way.

## Build

- Gradle wrapper is committed (8.14.3): `./gradlew assembleDebug testDebugUnitTest`. JDK 17, compileSdk/targetSdk 35, minSdk 26.
- Versions live in `gradle/libs.versions.toml` (AGP 8.7, Kotlin 2.1 with the Compose compiler plugin, KSP for Room and Hilt). Bump AGP/Kotlin/KSP/Hilt together: KSP's version is tied to Kotlin's.
- Room schemas are exported to `app/schemas/` (generated at build). Any entity change must bump `AppDatabase.version` and add a migration — users' notes must never be wiped.
- CI workflows are thin callers of the shared reusable workflows in the public repo `florianfish/Forge-Apk` (`android-build.yml`, `android-release.yml`, called `@main`). Change CI behaviour there, not here.
- `build.yml` runs `assembleDebug testDebugUnitTest` on every push to `main` and every PR, uploads the APK as the `MyWhiskyBeer-apk` artifact, comments the link on PRs, and on `main` replaces the `debug` pre-release: always-current APK at `https://github.com/florianfish/MyWhiskyBeer/releases/download/debug/MyWhiskyBeer-debug.apk`.
- Releases (`release.yml`), same procedure as MyRadioPlayer: push a `v*` tag, or run the workflow manually from `main` with a `type` input (`mineure`/`correctif`/`majeure`). Signing comes from repo secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`; `versionName` = tag without `v`, `versionCode` = run number, read from env vars in `app/build.gradle.kts`.
- Debug builds use the `.debug` applicationId suffix, so a debug APK installs next to the release one (separate data).

## Architecture

Single module `:app`, package `fr.florianfish.mywhiskybeer`.

- `data/local/` — Room. `ProductEntity` is the common trunk (type BEER/WHISKY); `BeerDetailsEntity` / `WhiskyDetailsEntity` hold type-specific fields (1:1, cascade). A product has many `TastingEntity`. Ratings are stored **0..100** for both types and displayed through `domain/RatingScale` (beer /5 by half points, whisky /100). Aromas are a reference table seeded on DB creation from `AromaSeed` (families → names, per type); `TastingAromaCrossRef` stores an intensity 1..3. `CellarItemEntity` (1:1 with product) holds stock/wishlist status and fill level.
- `data/photo/PhotoStore` — imports picked/captured photos into `filesDir/photos` (max 1600 px, JPEG 85, EXIF rotation applied). Camera captures go through a `FileProvider` (`${applicationId}.photos`, cache `captures/`).
- `di/` — Hilt modules. ViewModels inject DAOs directly (no repository layer yet); multi-table writes use `db.withTransaction`.
- `ui/` — Compose, one package per screen (`home`, `products`, `cellar`, `tasting`, `product`), type-safe navigation routes in `ui/navigation/Routes.kt`. Top-level screens get the outer Scaffold's padding; `TastingEditScreen` and `ProductDetailScreen` are full screen with their own Scaffold.
- `ui/theme/` — dark-first "cave à la lueur" theme; dynamic color is deliberately off. `ProductType.accent()` gives the type colour (amber whisky, straw beer). See `docs/DESIGN.md`.
- Signature components in `ui/components/`: `GlassGauge` (rating as a filling glass), `PourSlider` (drag on the glass to rate), `AromaWheel` (canvas wheel, tap cycles intensity), `AromaRadar`.

Unit tests (JVM) live in `app/src/test`; there are no instrumented tests yet.
