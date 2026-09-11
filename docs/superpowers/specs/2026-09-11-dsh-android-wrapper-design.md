# Especificação de Design — DSH Android Wrapper App

- **Data:** 2026-09-11
- **Status:** Proposta validada
- **Autor:** Antigravity & Usuário

---

## 1. Visão Geral e Propósito

O objetivo deste projeto é fornecer uma aplicação Android nativa e minimalista que envelope a interface web do **DSH (DeepSeek Harness)**, acessível em rede local pelo endereço padrão `http://192.168.0.102:3080/`.

O app remove as distrações visuais e barras de navegação do Chrome/navegadores móveis, oferecendo uma experiência imersiva de tela limpa ("clean design"), com inicialização ágil, suporte nativo a upload de anexos/imagens, atualização por gesto e configuração rápida de IP/porta para flexibilidade na rede.

---

## 2. Requisitos e Comportamento

### 2.1 Requisitos Funcionais
1. **URL Padrão e Configurável:**
   - URL padrão inicial: `http://192.168.0.102:3080/`.
   - Permite alteração manual via diálogo de configurações salvo em `SharedPreferences`.
   - Opção de restaurar a URL padrão.
2. **Navegação e Botão "Voltar":**
   - O gesto ou botão nativo de "Voltar" do sistema sempre minimiza o aplicativo (`moveTaskToBack(true)`), sem percorrer o histórico de rotas da WebView.
3. **Atualização por Gesto (Pull-to-Refresh):**
   - Suporte a arrastar a tela para baixo no topo (`SwipeRefreshLayout`) para recarregar a interface do DSH.
4. **Envio de Arquivos e Imagens (Anexos DSH):**
   - Interceptação de `onShowFileChooser` via `WebChromeClient` integrado ao `ActivityResultLauncher` do Android.
   - Permite que o usuário selecione imagens da galeria ou arquivos do dispositivo para envio nas conversas do DSH.
5. **Tratamento de Falha de Conexão:**
   - Exibição de tela de erro limpa caso o servidor local esteja offline ou o IP esteja inacessível.
   - Ações disponíveis na tela de erro: "Tentar Novamente" e "Alterar Endereço IP".

### 2.2 Requisitos Visuais e de Interface
1. **Borda a Borda (Edge-to-Edge):**
   - Aplicação em tela cheia com barras de status e navegação do sistema integradas e translúcidas, mantendo contraste legível dos ícones do sistema.
2. **Botão de Ajustes Sutil:**
   - Botão flutuante circular com baixa opacidade (~35%) posicionado no canto superior direito.
   - Ao ser clicado, abre o diálogo de edição da URL sem poluir a visão padrão do dashboard.

---

## 3. Arquitetura Técnica

### 3.1 Stack Tecnológico
- **Linguagem:** Kotlin.
- **Build System:** Gradle (Android Gradle Plugin).
- **SDK Targets:**
  - `minSdk`: 26 (Android 8.0 Oreo — compatibilidade ampla com suporte moderno de WebViews).
  - `compileSdk`: 34.
  - `targetSdk`: 34.
- **Bibliotecas AndroidX:**
  - `androidx.appcompat:appcompat:1.6.1`
  - `androidx.swiperefreshlayout:swiperefreshlayout:1.1.0`
  - `com.google.android.material:material:1.11.0`

### 3.2 Configurações da WebView
- `javaScriptEnabled = true`: Execução da SPA e lógica reativa do DSH.
- `domStorageEnabled = true`: Habilitação de `localStorage` e `sessionStorage`.
- `databaseEnabled = true`: Suporte a bancos de dados no cliente (IndexedDB).
- `cacheMode = WebSettings.LOAD_DEFAULT`: Otimização de carregamento de assets estáticos.
- `allowFileAccess = false`: Segurança contra vazamento de arquivos locais do dispositivo.
- `mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW`: Permite recursos mistos se necessário em redes locais.

### 3.3 Segurança de Rede e Conexão HTTP Local
Como o endereço padrão é `http://` (tráfego sem SSL), o Android requer autorização explícita:
- `android:usesCleartextTraffic="true"` no `AndroidManifest.xml`.
- Arquivo `res/xml/network_security_config.xml` configurando permissão de tráfego em texto claro para redes locais e loopback.

---

## 4. Estrutura de Arquivos

```
harness/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/dsh/app/
│           │   ├── MainActivity.kt
│           │   ├── DshWebViewClient.kt
│           │   ├── DshWebChromeClient.kt
│           │   └── AppPreferences.kt
│           └── res/
│               ├── drawable/
│               │   ├── ic_settings.xml
│               │   └── bg_settings_btn.xml
│               ├── layout/
│               │   ├── activity_main.xml
│               │   ├── dialog_settings.xml
│               │   └── view_error_state.xml
│               ├── values/
│               │   ├── colors.xml
│               │   ├── strings.xml
│               │   └── themes.xml
│               └── xml/
│                   └── network_security_config.xml
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```

---

## 5. Componentes e Fluxo de Dados

### 5.1 `AppPreferences.kt`
- Abstrai o acesso a `SharedPreferences`.
- Chave: `key_server_url`.
- Valor padrão: `http://192.168.0.102:3080/`.
- Métodos: `getServerUrl(): String`, `setServerUrl(url: String)`, `resetToDefault()`.

### 5.2 `DshWebChromeClient.kt`
- Herda de `WebChromeClient`.
- Implementa `onShowFileChooser(webView, filePathCallback, fileChooserParams)`.
- Dispara `ActivityResultLauncher<Intent>` para o usuário escolher o arquivo via seletor nativo do Android.
- Em caso de cancelamento pelo usuário, invoca `filePathCallback.onReceiveValue(null)` para liberar a WebView.

### 5.3 `DshWebViewClient.kt`
- Herda de `WebViewClient`.
- `onPageFinished`: Desativa o indicador de animação do `SwipeRefreshLayout` e oculta a tela de erro.
- `onReceivedError`: Exibe a tela de erro e oculta a WebView caso ocorra falha de rede/DNS/conexão recusada.

### 5.4 `MainActivity.kt`
- Configura o modo Edge-to-Edge (`WindowCompat.setDecorFitsSystemWindows(window, false)`).
- Inicializa a WebView e aplica as configurações do `DshWebChromeClient` e `DshWebViewClient`.
- Configura o listener do `SwipeRefreshLayout` para invocar `webView.reload()`.
- Registra callback no `onBackPressedDispatcher` chamando `moveTaskToBack(true)`.
- Controla o botão de engrenagem flutuante para inflar o `dialog_settings.xml`.

---

## 6. Critérios de Sucesso e Validação

1. **Compilação sem erros:** O projeto compila com sucesso via `./gradlew assembleDebug`.
2. **Carregamento da URL:** Ao abrir o app, tenta carregar `http://192.168.0.102:3080/`.
3. **Fallback amigável:** Caso o servidor não responda, a tela de erro surge com botão de tentar novamente e de trocar o IP.
4. **Troca de URL:** Alterar o IP no diálogo persiste a alteração e recarrega para o novo endereço.
5. **Comportamento do botão Voltar:** Pressionar Voltar minimiza o app imediatamente.
6. **Upload de anexos:** Clicar no botão de anexo dentro da interface web do DSH abre o seletor nativo do Android sem falhas.
