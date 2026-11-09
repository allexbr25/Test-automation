# GLPI HOSB — Aplicativo Android Nativo (Kotlin + Jetpack Compose)

Aplicativo Android desenvolvido para a central de serviços **GLPI HOSB (`https://glpi.hosb.com.br`)**, projetado com **Material Design 3**, layout responsivo adaptável (Smartphones e Tablets em modo *Master-Detail*) e arquitetura focada em **segurança e boas práticas**.

---

## 1. Mapeamento Fiel da Tela de Login e Credenciais (`glpi.hosb.com.br`)

Implementado em [`LoginScreen.kt`](app/src/main/java/br/com/hosb/glpi/ui/login/LoginScreen.kt), reproduzindo os elementos da página `welcome-anonymous` do GLPI HOSB com suporte completo à API REST (`/apirest.php`):

- **Usuário (`login_name`)**: Campo de identificação do colaborador/técnico.
- **Senha (`login_password`)**: Campo protegido com alternância de visibilidade, limpeza imediata da memória após autenticação e atalho para recuperação de senha (`/front/lostpassword.php?lostpassword=1`) quando a origem é local.
- **Origem de login (`auth`)**: Seletor idêntico ao GLPI (`Banco de dados interno do GLPI` — `local`, além de suporte a diretórios corporativos).
- **Credenciais de API REST (`/apirest.php/`)**:
  - **Endpoint da API**: Pré-configurado para `https://glpi.hosb.com.br/apirest.php/`.
  - **App-Token da API (`app_token`)**: Obrigatório pela instância `glpi.hosb.com.br` (`ERROR_APP_TOKEN_PARAMETERS_MISSING` quando ausente), injetado via cabeçalho `App-Token`.
  - **Token Pessoal (`user_token` — Opcional)**: Permite autenticação via cabeçalho `Authorization: user_token <token>` além do fluxo padrão `Authorization: Basic base64(login:senha)`.
- **Lembrar de mim (`login_remember`)**: Persistência criptografada no **Android Keystore (`AES-256-GCM`)**.

---

## 2. Disposição das Telas de Chamados (`front/central.php` & `front/ticket.php`)

Implementada em [`TicketListScreen.kt`](app/src/main/java/br/com/hosb/glpi/ui/tickets/TicketListScreen.kt), [`TicketDetailScreen.kt`](app/src/main/java/br/com/hosb/glpi/ui/tickets/TicketDetailScreen.kt) e [`NewTicketSheet.kt`](app/src/main/java/br/com/hosb/glpi/ui/tickets/NewTicketSheet.kt):

- **Painel de Indicadores (KPIs)**: Contadores rápidos para *Total*, *Em Atendimento (atribuído)*, *Pendentes* e *Novos*.
- **Filtros por Status ITIL e Busca Instantânea**: Pesquisa por ID (`#18333`, `#18309`, `#15803`, etc.), Título, Categoria (`FINANCEIRO > ALTERAÇÃO DE CAIXA`, `FATURAMENTO`, `COMPRAS`, `RH - RECURSOS HUMANOS`), Localização (`Matriz (Fonseca) > TIC`, `CPD`, `Recepção Urgência`), Grupo Técnico (`Financeiro /TIC`, `Faturamento`, `Cotacao`, `RH TIC`) e Requerente.
- **Detalhes do Chamado em Abas (`Ticket$main`)**:
  1. **Chamado & Atores**: Descrição sanitizada, Categoria, Localização, Urgência, Datas de Abertura/Atualização, Requerente, Grupo Técnico Atribuído e Observador.
  2. **Timeline (Acompanhamentos, Tarefas e Soluções)**: Histórico cronológico e compositor para envio de novos `ITILFollowup` (públicos ou privados para a equipe TIC).
  3. **Aprovações (`TicketValidation`)**: Etapas de validação, status, comentários e botões de decisão (*Aprovar Etapa* / *Recusar*).
  4. **Segurança & Logs**: Diagnóstico em tempo real da sessão criptografada e políticas de proteção ativas.

---

## 3. Segurança e Boas Práticas Implementadas

| Camada | Implementação | Arquivo |
| :--- | :--- | :--- |
| **Armazenamento Criptografado** | `EncryptedSharedPreferences` com chave `MasterKey` `AES256_GCM` no Android Keystore | [`SecureCredentialManager.kt`](app/src/main/java/br/com/hosb/glpi/data/security/SecureCredentialManager.kt) |
| **Transporte HTTPS / TLS 1.3** | Bloqueio estrito de tráfego HTTP (`cleartextTrafficPermitted="false"`) e `ConnectionSpec.MODERN_TLS` | [`network_security_config.xml`](app/src/main/res/xml/network_security_config.xml) |
| **Interceptor de Sessão & Tokens** | Injeção segura de `App-Token` e `Session-Token`, invalidação automática em `HTTP 401` | [`GlpiAuthInterceptor.kt`](app/src/main/java/br/com/hosb/glpi/data/remote/GlpiAuthInterceptor.kt) |
| **Proteção contra Captura de Tela** | `WindowManager.LayoutParams.FLAG_SECURE` ativo na `MainActivity` | [`MainActivity.kt`](app/src/main/java/br/com/hosb/glpi/MainActivity.kt) |
| **Prevenção de Extração / Backup** | `android:allowBackup="false"` e regras de exclusão em `data_extraction_rules.xml` | [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) |
| **Sanitização HTML / Anti-XSS** | Limpeza de tags e entidades HTML provenientes de descrições e acompanhamentos do GLPI | [`GlpiRepository.kt`](app/src/main/java/br/com/hosb/glpi/data/repository/GlpiRepository.kt) |
