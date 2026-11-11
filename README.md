# 📱 Widgets para Android (Material You & Dynamic Colors)

Aplicativo Android ultraleve focado em alta fidelidade estética e widgets modernos para a tela inicial, desenvolvido em **Kotlin**, **Jetpack Glance** e **Jetpack Compose + Material 3**. Suporte nativo à paleta de cores dinâmica do sistema (**Material You / Monet Engine**) do Android 12 ao **Android 17**.

---

## ✨ Recursos Principais

### 📅 1. Widget de Agenda Dinâmica (Google Calendar)
Sincronizado diretamente com a sua conta Google no dispositivo através do `CalendarContract` do Android (sem sobrecarga de APIs em nuvem, rápido e offline-first).

- **Material You (Dynamic Colors)**: Acompanha automaticamente as cores extraídas do papel de parede do sistema e dos ícones temáticos do Android.
- **Transparência / Vidro**: Controle deslizante de opacidade de 0% (vidro total) a 100% (sólido).
- **Formatos e Bordas**: Ajuste do raio dos cantos (de 0dp a 32dp pílula) e espessura do contorno.
- **Modos de Visualização**:
  - *Lista de Compromissos*: Visão ordenada por horário com cores da agenda.
  - *Destaque do Próximo Evento*: Exibição tipográfica elegante com contagem e local.
- **Sincronização em Tempo Real**: Observador nativo do sistema que atualiza os eventos automaticamente ao criar ou editar na Agenda do Google.
- **Botão de Atualização Rápida (🔄)**: Ícone discreto no widget para forçar a atualização a qualquer momento.
- **Ação por Toque**: Tocar em qualquer compromisso abre a Agenda do Google diretamente na tela do evento.

---

### 🖼️ 2. Widget de Porta-Retratos (Fotos Personalizadas)
Exiba suas fotos favoritas com suporte a rotação inteligente e múltiplos formatos estéticos.

- **Seleção Nativa**: Utiliza o **Android Photo Picker** do sistema, sem necessidade de permissões invasivas de leitura de arquivos.
- **Formatos Personalizados**:
  - Retangular com cantos arredondados configuráveis.
  - Círculo / Oval.
  - Squircle moderno.
  - Moldura externa (*passe-partout*) com suporte a cores do Material You.
- **Rotação Automática de Múltiplas Fotos**:
  - **Ao Desbloquear a Tela**: Muda para a próxima foto automaticamente cada vez que você liga/desbloqueia o celular.
  - **Intervalos de Tempo**: Temporizador configurável (15 min, 30 min, 1 hora, 6 horas, Diário 24h).
  - **Toque Direto**: Toque na foto para alternar imediatamente para a imagem seguinte.
- **Design Limpo**: Sem números ou contadores sobrepostos na imagem.
- **Otimização de Memória**: Compressão automática e cache interno respeitando os limites estritos de IPC do Android RemoteViews.

---

## 🛠️ Tecnologias Utilizadas

| Tecnologia | Finalidade |
| :--- | :--- |
| **Kotlin 2.x** | Linguagem principal do projeto |
| **Jetpack Glance** (`glance-appwidget`, `glance-material3`) | Criação declarativa moderna dos widgets |
| **Jetpack Compose + Material 3** | Interface de configuração e pré-visualização em tempo real |
| **Coil** | Carregamento assíncrono e ultra-eficiente de imagens |
| **Jetpack DataStore** | Armazenamento reativo e assíncrono das preferências por widget |
| **R8 Compiler** | Otimização de código e redução do APK para apenas **3.2 MB** |

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- **Android Studio** (versão Ladybug / Meerkat / Koala ou superior)
- **Android SDK** com suporte a API 31 até API 35/37
- **JDK 17** ou superior

### Passos
1. Clone este repositório:
   ```bash
   git clone https://github.com/pedoandrade/widges.git
   cd widges
   ```
2. Abra o projeto no **Android Studio**:
   - Vá em `File` > `Open...` e selecione a pasta do projeto.
3. Conecte seu smartphone Android ou inicie um emulador.
4. Clique em **Run ▶** (`Shift + F10`) para compilar e instalar o app.

---

## 📦 Compilação via Linha de Comando

```bash
# Compilar APK de depuração
./gradlew assembleDebug

# Compilar APK final minificado (release)
./gradlew assembleRelease
```

Os APKs gerados estarão em:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## 📄 Licença

Distribuído sob a licença MIT. Veja `LICENSE` para mais informações.
