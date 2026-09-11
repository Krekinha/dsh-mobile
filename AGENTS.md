# AGENTS.md — DSH Android App Wrapper

## Projeto
Aplicativo Android nativo (Kotlin) que envelopa a interface web do DSH (DeepSeek Harness).

## Stack & Ferramentas
- **Linguagem:** Kotlin 1.9.22
- **Build System:** Gradle com Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`)
- **Android SDK:** `minSdk = 26`, `targetSdk = 34`, `compileSdk = 34`
- **UI:** Edge-to-Edge nativo, Material Components, SwipeRefreshLayout, WebView moderna

## Comandos Principais (executar dentro de dsh-mobile/)
- Compilar APK de desenvolvimento:
  ```bash
  ./gradlew assembleDebug
  ```
- Executar testes unitários:
  ```bash
  ./gradlew testDebugUnitTest
  ```
