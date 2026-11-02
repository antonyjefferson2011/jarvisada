# JARVIS Mobile

Aplicativo Android nativo em Kotlin, com Jetpack Compose, MVVM, comandos locais, voz, histórico SQLite e sincronização opcional com Supabase. A chave da Groq é usada somente por uma Supabase Edge Function e não é incluída no APK nem neste ZIP.

## O que funciona no aplicativo

- Conversa por texto e reconhecimento de fala em português brasileiro.
- Leitura das respostas em voz alta, botão para interromper a fala e controles de velocidade e volume.
- Histórico e memórias locais em SQLite; preferências no armazenamento privado do aplicativo.
- Comandos locais para data, hora, cálculo, conversão de unidades, informações básicas do aparelho, abertura de aplicativos/configurações e busca de lugares em um aplicativo de mapas instalado.
- Cadastro e login Supabase; sessões guardadas com criptografia do Android Keystore.
- Sincronização de mensagens e memórias com tabelas protegidas por Row Level Security (RLS).
- Chat com a Groq por uma Edge Function autenticada.

## Requisitos

- Android Studio recente com suporte a Android Gradle Plugin 8.10.1.
- JDK 17 ou superior.
- Android SDK Platform 35 e Build Tools correspondentes.
- Para usar IA e sincronização: um projeto Supabase e uma chave Groq.
- Para instalação no Galaxy A05: Android 8.0/API 26 ou superior. O A05 atende ao requisito mínimo.

## Abrir e compilar

1. Extraia o ZIP.
2. No Android Studio, escolha **Open** e selecione a pasta raiz `JarvisMobile` (a pasta que contém `settings.gradle.kts`).
3. Aguarde a sincronização do Gradle e confirme a instalação do Android SDK Platform 35 se o Android Studio solicitar.
4. Execute em um emulador ou aparelho conectado. Para gerar um APK de depuração, use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

Se preferir o terminal, use `./gradlew testDebugUnitTest assembleDebug`. O primeiro build baixa o Gradle e as dependências Android.

## Configurar o Supabase e a Groq

Os dados de cada projeto Supabase são adicionados na tela **Configurações** do aplicativo. A chave pública `anon`/`publishable` pode ficar no cliente Android, mas **não use a chave `service_role` nem uma chave secreta no APK**.

1. Crie um projeto Supabase e mantenha anotados a URL do projeto e a chave pública (`anon` ou `sb_publishable_...`).
2. No painel SQL do Supabase, execute `supabase/migrations/20261101000000_jarvis_schema.sql`. Ela cria as tabelas `jarvis_messages` e `jarvis_memories`, índices e políticas RLS por usuário.
3. Instale e autentique o Supabase CLI na sua máquina. A partir da pasta do projeto, associe o diretório ao projeto Supabase:

   ```bash
   supabase login
   supabase link --project-ref SEU_PROJECT_REF
   ```

4. Configure a chave Groq como segredo **no projeto Supabase**, não no Android nem no Git:

   ```bash
   supabase secrets set GROQ_API_KEY=SUA_CHAVE_GROQ
   ```

   Opcionalmente, defina `GROQ_MODEL`. O padrão é `openai/gpt-oss-120b`, listado na documentação de modelos da Groq.

5. Publique a função:

   ```bash
   supabase functions deploy jarvis-chat
   ```

   A configuração em `supabase/config.toml` mantém `verify_jwt = true`. A função também valida a sessão do usuário antes de chamar a Groq.

6. Abra o app, vá a **Configurações**, informe a URL e a chave pública do projeto, salve e crie uma conta. Se a confirmação de e-mail estiver habilitada no Supabase, confirme o endereço antes de entrar.

As credenciais que o app pede são valores de configuração daquele projeto. Não compartilhe a chave Groq, a chave `service_role` nem sua senha. A senha da conta não é salva; os tokens de sessão são cifrados com uma chave do Android Keystore.

## Instalar no Samsung Galaxy A05

1. Ative as opções de desenvolvedor e a depuração USB no telefone.
2. Conecte o aparelho ao computador, aceite a autorização exibida no telefone e selecione o A05 como dispositivo no Android Studio.
3. Pressione **Run** para instalar. Alternativamente, gere o APK com **Build APK(s)**, transfira-o ao telefone e abra-o para instalar.
4. Permita o microfone quando o Android solicitar. O reconhecimento de fala depende do serviço de fala disponível no aparelho; o reconhecimento offline só funciona quando esse serviço oferece suporte ao português offline.

## Voz em segundo plano e ferramentas externas

O botão de microfone inicia a captura apenas enquanto o app está aberto. A opção de palavra-chave **prepara a preferência**, mas não ativa escuta contínua: o Android restringe o uso do microfone em segundo plano e exige um serviço em primeiro plano visível ao usuário. Não há detecção de “JARVIS” ativa nesta versão.

Calculadora, conversões, data, hora, consulta básica do aparelho, abertura de apps e pesquisa em mapas funcionam localmente ou por intents do Android. A conversa com a Groq pode traduzir e responder a perguntas gerais, mas **pesquisa na web, clima e notícias em tempo real não estão conectados**. O app não simula esses resultados. Para adicioná-los, integre e documente um provedor específico em uma versão futura.

## Estrutura

```text
app/src/main/java/com/jarvis/mobile/
  data/local/       SQLite e modelos locais
  data/remote/      autenticação, chat e sincronização Supabase
  data/repository/  repositório de dados
  domain/           comandos locais e calculadora
  navigation/       fluxo entre conversa e configurações
  security/         sessão cifrada com Android Keystore
  ui/               telas Compose e ViewModel
  voice/            reconhecimento de fala e síntese de voz
supabase/
  functions/jarvis-chat/
  migrations/
```

## Testes

Os testes unitários da calculadora estão em `app/src/test`. Execute `./gradlew testDebugUnitTest`. Testes de autenticação e sincronização reais exigem configurar um projeto Supabase e suas credenciais no aparelho.
