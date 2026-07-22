# Deploy no Render

O arquivo `render.yaml` cria os três recursos de produção do SGD:

- `sgd-web`: site estático React/Vite;
- `sgd-api`: serviço web Docker com uma única instância;
- `sgd-db`: PostgreSQL 16 gerenciado, sem acesso externo.

## Pré-requisitos

1. Repositório conectado ao Render e ao GitHub Actions.
2. Workspace Render com cobrança habilitada, porque o banco gratuito não possui recuperação adequada para produção.
3. Provedor SMTP configurado.
4. Vendor de observabilidade que aceite OTLP HTTP/protobuf para traces, métricas e logs.

## Primeiro deploy

1. No Render, crie um Blueprint a partir do `render.yaml`.
2. Preencha os valores marcados com `sync: false`:

   | Variável | Conteúdo |
   | --- | --- |
   | `ADMIN_INITIAL_EMAIL` | E-mail do primeiro administrador (receberá convite para definir senha) |
   | `MAIL_FROM` | Remetente autorizado pelo SMTP |
   | `MAIL_HOST` | Host SMTP |
   | `MAIL_USERNAME` | Usuário SMTP |
   | `MAIL_PASSWORD` | Senha SMTP |
   | `OTEL_EXPORTER_OTLP_ENDPOINT` | Endpoint OTLP base do vendor |
   | `OTEL_EXPORTER_OTLP_HEADERS` | Headers exigidos pelo vendor, no formato `chave=valor` |

3. Aguarde primeiro o banco e depois a API. O Flyway aplica as migrações no startup; a API só fica pronta quando a aplicação e o indicador `db` estiverem saudáveis.
4. Aguarde o build do frontend. `VITE_API_ORIGIN` é preenchida com a URL pública da API pelo próprio Blueprint.
5. Faça os testes de fumaça descritos abaixo. O administrador inicial define a senha pelo link recebido por e-mail (ou pelo Mailpit em ambientes locais).
6. Depois do primeiro acesso, você pode remover `ADMIN_INITIAL_EMAIL` do ambiente. Sem esse valor, o bootstrap não tenta recriar o usuário.

O entrypoint recebe a URL interna do Render Postgres, extrai somente host e porta e mantém usuário, senha e nome do banco nas referências gerenciadas pelo Blueprint. O banco continua inacessível pela internet.

## Domínio personalizado

Por padrão, frontend e API usam URLs `onrender.com`. Ao adicionar um domínio próprio:

1. configure o domínio do frontend no Render;
2. altere `CORS_ALLOWED_ORIGIN` para a origem HTTPS exata do frontend;
3. altere `APP_PUBLIC_URL` para a mesma origem;
4. opcionalmente configure um domínio para a API e atualize `VITE_API_ORIGIN`, provocando um novo build do frontend.

Não use curingas no CORS e não coloque segredos em variáveis `VITE_*`, pois elas fazem parte do bundle público.

## Testes de fumaça

1. `GET /` do frontend retorna `200` por HTTPS.
2. `GET /actuator/health/readiness` da API retorna `UP` e inclui o banco saudável.
3. Login, renovação de token e logout funcionam pelo frontend.
4. Uma escrita simples persiste e pode ser lida novamente.
5. O fluxo de recuperação envia e-mail e abre a URL correta do frontend.
6. Uma requisição aparece no vendor como trace, métricas e logs correlacionados pelo mesmo `traceId`.

## Banco, migrações e restauração

- Mantenha `numInstances: 1`: o job `@Scheduled` atual executa em cada instância.
- Para escalar a API, mova o job para um Render Cron Job ou adicione trava distribuída antes de aumentar `numInstances`.
- Use migrações Flyway no padrão expand/contract. Uma versão não deve remover uma coluna ainda usada pela versão anterior.
- Antes de uma migração destrutiva, gere uma exportação lógica manual.
- Trimestralmente, restaure o PITR em um banco novo, execute os testes de fumaça contra a cópia e registre RPO, RTO e resultado.
- Não exclua o banco original antes de validar completamente uma restauração.

## Rollback e troca de credenciais

- O deploy automático ocorre apenas depois que a CI passa. Se uma release falhar, use o rollback do serviço no Render.
- Rollback de código não desfaz migração; por isso as migrações precisam ser retrocompatíveis.
- Para rotacionar o banco, crie uma nova credencial gerenciada, sincronize o Blueprint, redeploye a API, confirme que as conexões antigas cessaram e só então revogue a credencial anterior.
- Para trocar o vendor de observabilidade, altere apenas `OTEL_EXPORTER_OTLP_ENDPOINT` e `OTEL_EXPORTER_OTLP_HEADERS`, redeploye a API e recrie os dashboards no destino.
