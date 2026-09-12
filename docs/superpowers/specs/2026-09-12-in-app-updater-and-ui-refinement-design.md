# Especificação Técnica: Atualizador In-App, Refinamento do Botão de Servidor e Ícone do App

- **Data:** 2026-09-12
- **Status:** Aprovado para Planejamento
- **Projeto:** `dsh-mobile` (Android Kotlin Wrapper para DeepSeek Harness)
- **Repositório GitHub:** `Krekinha/dsh-mobile`

---

## 1. Visão Geral e Objetivos

Este documento especifica a implementação de quatro melhorias essenciais no aplicativo Android `dsh-mobile`:
1. **Subsistema de Atualização In-App:** Adicionar à janela de configurações do servidor uma funcionalidade de verificação de atualizações no GitHub Releases, exibição de tarefas de checagem, barra de progresso do download em tempo real e acionamento direto do instalador nativo do Android.
2. **Reposicionamento e Redimensionamento do Botão de Servidor:** Mover o botão flutuante de abertura das configurações (anteriormente no canto superior direito, onde cobria elementos da interface do DSH) para o canto inferior esquerdo (acima do botão de configurações do harness e alinhado à barra lateral), reduzir suas dimensões para 32dp e substituir o ícone de engrenagem por um ícone temático de servidor/computador.
3. **Identidade Visual e Ícone do Aplicativo:** Gerar a suíte completa de ícones de lançador Android (`mipmap-mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`) a partir da imagem `@logo.png` (512×512) e configurar o manifesto do aplicativo.
4. **Remoção do Pull-to-Refresh (`SwipeRefreshLayout`):** Eliminar o componente `SwipeRefreshLayout` que encapsulava o `WebView`, pois o gesto de puxar para atualizar conflita diretamente com a rolagem vertical (scroll up/down) das mensagens e telas do DSH. O `WebView` passa a ser filho direto do layout principal.

---

## 2. Arquitetura e Componentes

### 2.1 Componentes e Responsabilidades

```
+-------------------------------------------------------------------+
|                        MainActivity.kt                            |
|  - Controla o ciclo de vida do diálogo de configurações           |
|  - Gerencia o novo botão flutuante de servidor (inferior esquerdo)|
|  - Vincula o UpdateManager à interface do diálogo                 |
+---------------------------------+---------------------------------+
                                  |
                                  v
+-------------------------------------------------------------------+
|                        UpdateManager.kt                           |
|  - Consulta a API pública de releases do GitHub                   |
|  - Baixa o APK com streaming e notificação de progresso (bytes/%) |
|  - Aciona o instalador de pacotes via FileProvider                |
+-------------------+-----------------------------+-----------------+
                    |                             |
                    v                             v
+-----------------------------+     +-------------------------------+
|      VersionHelper.kt       |     |        FileProvider           |
|  - Compara semver vX.Y.Z    |     |  - Compartilha APK do cache   |
|  - Testado via JUnit        |     |    privado com o instalador   |
+-----------------------------+     +-------------------------------+
```

### 2.2 Estrutura de Arquivos Impactados e Novos

- **Novos Arquivos:**
  - `app/src/main/java/com/dsh/app/UpdateManager.kt` — Lógica de verificação, download e instalação.
  - `app/src/main/java/com/dsh/app/VersionHelper.kt` — Utilitário de parsing e comparação de versões semânticas.
  - `app/src/test/java/com/dsh/app/VersionHelperTest.kt` — Testes unitários para comparação de versão.
  - `app/src/main/res/xml/file_paths.xml` — Configuração de caminhos do FileProvider.
  - `app/src/main/res/drawable/ic_server.xml` — Ícone vetorial de servidor/DNS.
  - `app/src/main/res/mipmap-*/ic_launcher.png` e `ic_launcher_round.png` — Ícones gerados a partir do `logo.png`.
- **Arquivos Modificados:**
  - `app/src/main/AndroidManifest.xml` — Permissão `REQUEST_INSTALL_PACKAGES`, `<provider>` do FileProvider e `android:icon`/`android:roundIcon`.
  - `app/src/main/res/layout/activity_main.xml` — Posição, margens, tamanho e ícone do `btnSettings`.
  - `app/src/main/res/layout/dialog_settings.xml` — Bloco de atualização (botão, status, progresso, métricas).
  - `app/src/main/java/com/dsh/app/MainActivity.kt` — Eventos de clique do diálogo, integração com `UpdateManager` e tratamento de permissão de instalação.
  - `app/build.gradle.kts` — `versionCode = 2`, `versionName = "1.0.1"`.

---

## 3. Especificação do Subsistema de Atualização

### 3.1 Consulta à API do GitHub
- **URL:** `https://api.github.com/repos/Krekinha/dsh-mobile/releases/latest`
- **Método:** `GET`
- **Cabeçalhos:**
  - `Accept: application/vnd.github.v3+json`
  - `User-Agent: DSH-Mobile-App`
- **Campos Utilizados do JSON:**
  - `tag_name`: String da versão (ex.: `"v1.0.1"` ou `"1.0.1"`).
  - `name`: Título da release.
  - `body`: Notas de lançamento (opcional para exibição curta).
  - `assets`: Array de objetos contendo `name` (filtrado por terminação `.apk`), `browser_download_url` e `size`.

### 3.2 Comparação de Versão (`VersionHelper`)
- Normalização: remove espaços e prefixo `v`/`V`.
- Suporta formato semântico `MAJOR.MINOR.PATCH` (ex.: `1.0.1` vs `1.0.0`).
- Retorna `isNewer(remoteVersion: String, currentVersion: String): Boolean`.
- Se a versão remota for superior à instalada, aciona o estado `UPDATE_AVAILABLE`.
- Se for igual ou menor, aciona o estado `UP_TO_DATE`.

### 3.3 Streaming de Download e Cache
- Executado em thread de segundo plano com `ExecutorService` (sem travar a thread principal).
- Conexão via `HttpURLConnection` com suporte a redirecionamento HTTP 302/301 (necessário para downloads de assets do GitHub).
- Leitura em buffers de 8 KB acumulando bytes baixados.
- Notificação de progresso na `runOnUiThread`:
  - `bytesRead`: Long
  - `totalBytes`: Long
  - `progressPercent`: Int (0 a 100)
- Arquivo de destino: `File(context.cacheDir, "updates/dsh-update.apk")`.
- Antes de iniciar um novo download, qualquer arquivo parcial ou anterior é removido para evitar corrupção.

### 3.4 Acionamento do Instalador Nativo do Android (Android 8.0+ / API 26 a 34)
- **Permissão no Manifesto:**
  ```xml
  <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
  ```
- **Configuração do FileProvider (`AndroidManifest.xml`):**
  ```xml
  <provider
      android:name="androidx.core.content.FileProvider"
      android:authorities="${applicationId}.fileprovider"
      android:exported="false"
      android:grantUriPermissions="true">
      <meta-data
          android:name="android.support.FILE_PROVIDER_PATHS"
          android:resource="@xml/file_paths" />
  </provider>
  ```
- **Caminho mapeado (`res/xml/file_paths.xml`):**
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <paths>
      <cache-path name="updates" path="updates/" />
  </paths>
  ```
- **Fluxo de Instalação:**
  1. Verifica `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls())`:
     - Dispara `Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))`.
     - Exibe aviso instruindo o usuário a conceder a permissão.
  2. Obtém a URI segura: `FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)`.
  3. Cria Intent de instalação:
     ```kotlin
     val intent = Intent(Intent.ACTION_VIEW).apply {
         setDataAndType(apkUri, "application/vnd.android.package-archive")
         addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
         addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
     }
     context.startActivity(intent)
     ```

---

## 4. Especificação de Interface e UX

### 4.1 Reposicionamento do Botão de Servidor (`activity_main.xml`)
- **Posição:** `layout_gravity="bottom|start"`.
- **Margens:**
  - `android:layout_marginStart="10dp"`
  - `android:layout_marginBottom="76dp"` (posicionado exatamente acima do botão de engrenagem do DSH e acima da barra de navegação do sistema).
- **Dimensões:** `32dp × 32dp` (redução em relação aos 40dp anteriores).
- **Fundo:** `bg_settings_fab.xml` com raio e tamanho adaptados para 32dp.
- **Ícone:** `@drawable/ic_server` (vetor temático de servidor/computador com preenchimento claro/translúcido).

### 4.2 Painel de Atualizações no Diálogo (`dialog_settings.xml`)
- **Localização:** Abaixo do botão `btnResetDefault`, separado por uma linha divisória discreta (`View` de altura 1dp com cor `@color/surface_variant`).
- **Elementos Visuais:**
  - `txtVersionInfo`: Exibe a versão instalada (ex.: `"Versão do App: v1.0.1"`).
  - `btnCheckUpdates`: Botão com texto `"Verificar Atualizações"` e ícone de refresh.
  - `layoutUpdateProgress`: Container de progresso (inicialmente `View.GONE`):
    - `txtUpdateStatus`: Texto de status dinâmico (ex.: `"Buscando versão no GitHub..."`, `"Nova versão v1.0.2 disponível!"`, `"Baixando atualização..."`).
    - `progressBarUpdate`: Barra de progresso linear (indeterminada durante checagem; determinada durante download).
    - `txtProgressDetail`: Texto com percentual e tamanho (ex.: `"4.2 MB / 15.0 MB (28%)"`).
    - `btnActionUpdate`: Botão dinâmico que surge ao detectar nova versão (`"Baixar e Instalar"`) ou ao concluir o download (`"Instalar APK"`).

---

## 5. Ícone do Aplicativo (`logo.png`)

- **Origem:** Arquivo `@logo.png` (512×512 PNG, RGBA).
- **Destinos Gerados:**
  - `res/mipmap-mdpi/ic_launcher.png` (48×48)
  - `res/mipmap-hdpi/ic_launcher.png` (72×72)
  - `res/mipmap-xhdpi/ic_launcher.png` (96×96)
  - `res/mipmap-xxhdpi/ic_launcher.png` (144×144)
  - `res/mipmap-xxxhdpi/ic_launcher.png` (192×192)
  - Versões circulares correspondentes (`ic_launcher_round.png`).
- **Configuração no `AndroidManifest.xml`:**
  - `android:icon="@mipmap/ic_launcher"`
  - `android:roundIcon="@mipmap/ic_launcher_round"`

---

## 6. Estratégia de Testes e Validação

1. **Testes Unitários Locais (`VersionHelperTest`):**
   - Comparação de versão maior remota (`1.0.2` > `1.0.1` -> true).
   - Versão idêntica (`1.0.1` == `1.0.1` -> false).
   - Versão menor remota (`1.0.0` < `1.0.1` -> false).
   - Suporte a prefixos e sufixos (`v1.0.2` vs `1.0.1`).
2. **Validação de Sintaxe e Estrutura XML:**
   - Conferência de integridade de IDs de layout (`dialog_settings.xml`, `activity_main.xml`).
   - Conferência das permissões e providers no `AndroidManifest.xml`.
3. **Validação do Build via GitHub Actions CI:**
   - Commit e push acionam o workflow `.github/workflows/build-apk.yml`.
   - Geração bem-sucedida do `app-debug.apk` e atualização da release `latest`.
