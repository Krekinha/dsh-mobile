# DSH Android App Wrapper

Aplicativo Android nativo e minimalista para envelopar a interface web do **DSH (DeepSeek Harness)**, removendo a barra de endereços e controles do Chrome para proporcionar uma interface limpa, imersiva e responsiva.

---

## Recursos

- **Design Clean & Borda a Borda (Edge-to-Edge):** Oculta a interface de navegação do browser tradicional, aproveitando a tela inteira com as barras de status e navegação translúcidas.
- **URL Padrão & Configurável:** Inicia por padrão em `http://192.168.0.102:3080/`, com um botão flutuante discreto de engrenagem para alterar o IP/porta a qualquer momento.
- **Upload de Imagens e Anexos:** Integração total com o seletor nativo de arquivos do Android (`WebChromeClient` com `onShowFileChooser`), permitindo enviar imagens e documentos diretamente nas conversas do DSH.
- **Gesto de Atualizar (Pull-to-Refresh):** Deslize para baixo no topo para recarregar o dashboard rapidamente.
- **Botão Voltar Nativo:** Pressionar o botão/gesto Voltar minimiza ou fecha o aplicativo diretamente, sem loops de histórico na WebView.
- **Tratamento de Quedas de Rede:** Se o servidor estiver offline, o app exibe uma tela amigável com opção de tentar novamente ou trocar de IP.
- **Tráfego HTTP Local Permitido:** Configurado com `usesCleartextTraffic="true"` e `network_security_config.xml` para permitir conexões HTTP sem exigência de certificados SSL locais.

---

## Estrutura do Projeto

```
harness/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/dsh/app/
│       │   │   ├── MainActivity.kt
│       │   │   ├── AppPreferences.kt
│       │   │   ├── UrlHelper.kt
│       │   │   ├── DshWebViewClient.kt
│       │   │   └── DshWebChromeClient.kt
│       │   └── res/
│       │       ├── drawable/
│       │       ├── layout/
│       │       ├── values/
│       │       └── xml/network_security_config.xml
│       └── test/java/com/dsh/app/
│           └── UrlHelperTest.kt
├── docs/
│   ├── plans/
│   └── superpowers/specs/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Como Compilar e Executar

### Opção 1: Via Android Studio (Recomendado)
1. Abra o **Android Studio**.
2. Clique em **Open** e selecione a pasta deste repositório (`/home/krek/repos/lab/harness` ou caminho correspondente no Windows).
3. Aguarde a sincronização do Gradle.
4. Conecte seu smartphone Android via USB (com depuração USB ativada) ou inicie um emulador.
5. Clique no botão **Run (Shift + F10)**.

### Opção 2: Via Linha de Comando (Gradle CLI)
Certifique-se de ter o Android SDK e Java instalados.

1. Gerar o APK de Debug:
   ```bash
   ./gradlew assembleDebug
   ```
2. O APK gerado estará disponível em:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```
3. Instalar no celular conectado via ADB:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Alterando a URL do Servidor

1. Abra o app no celular.
2. Toque no **ícone sutil de engrenagem** no canto superior direito.
3. Insira o novo endereço (ex: `http://192.168.0.105:3080/` ou `http://meu-servidor:3080/`).
4. Toque em **Salvar e Conectar**. O app salvará nas preferências e recarregará a tela automaticamente.
