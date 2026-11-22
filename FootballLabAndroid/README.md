# Football Lab Android

Android native starter app (Kotlin + Jetpack Compose), isolated in the `FootballLabAndroid` directory.

## Build APK
GitHub Actions builds a debug APK whenever this folder changes on branch `football-lab-apk`, and also supports manual dispatch.

Artifact: `FootballLab-debug-apk`.

This initial build is a UI shell only. Excel import, DuckDB integration and persistent football data are not yet implemented.
